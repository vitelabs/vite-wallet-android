package net.vite.wallet.abi

import androidx.annotation.Keep
import com.google.gson.Gson
import net.vite.wallet.abi.datatypes.DynamicArray
import net.vite.wallet.abi.datatypes.Type
import net.vite.wallet.abi.datatypes.Utf8String
import net.vite.wallet.abi.datatypes.generated.AbiTypes
import java.util.regex.Pattern

@Keep
class AbiJsonParseException(msg: String) : Exception(msg)

class FunctionDesc(
    val name: String,
    val paramNameTypePair: List<Pair<String, TypeReference<out Type<*>>>>?
) {
    fun getParamsList() = paramNameTypePair?.fold(ArrayList<TypeReference<Type<*>>>()) { acc, pair ->
        acc.add(pair.second as TypeReference<Type<*>>)
        acc
    }

}

@Keep
private data class InnerNameTypePair(
    val type: String?,
    val name: String?
)

@Keep
private data class InnerFunctionDesc(
    val type: String?,
    val name: String?,
    val inputs: List<InnerNameTypePair>?
)

object AbiJsonParse {
    fun parse(json: String): FunctionDesc {
        val funcDesc = Gson().fromJson<InnerFunctionDesc>(json, InnerFunctionDesc::class.java)
        val functionName = funcDesc.name ?: throw  AbiJsonParseException("empty functionName")
        if (funcDesc.type != "function") throw  AbiJsonParseException("type is not function")
        val inputs = funcDesc.inputs?.fold(ArrayList<Pair<String, TypeReference<out Type<*>>>>()) { acc, item ->
            val typeReference =
                item.type?.let { typeStr ->
                    try {
                        decodeAsBase(typeStr)
                    } catch (e: Exception) {
                        decodeAsArray(typeStr)
                    }
                } ?: throw UnsupportedOperationException("Unsupported type encountered: ${item.type}")

            val name = item.name ?: ""
            acc.add(name to typeReference)
            acc
        }
        return FunctionDesc(name = functionName, paramNameTypePair = inputs)

    }

    private fun decodeAsArray(typeStr: String): TypeReference<out Type<*>> {
        val matcheResult = Pattern.compile("^(.+)\\[(([1-9][0-9]*)?)]$").matcher(typeStr)
        if (!matcheResult.matches()) {
            throw UnsupportedOperationException("Unsupported type encountered: $typeStr")
        }
        val componentType = AbiTypes.getType(matcheResult.group(1))

        if (componentType in arrayOf(
                Utf8String::class.java,
                DynamicArray::class.java,
                net.vite.wallet.abi.datatypes.Array::class.java
            )
        ) {
            throw UnsupportedOperationException("Unsupported componentType $typeStr")
        }

        val arraySizeStr = matcheResult.group(2)
        return if (arraySizeStr.isNullOrEmpty()) {
            object : TypeReference<DynamicArray<Type<*>>>() {
                override fun getType(): java.lang.reflect.Type {
                    return componentType
                }
            }
        } else {
            val fixsize = arraySizeStr.toIntOrNull() ?: throw IllegalArgumentException("not support size $arraySizeStr")
            object : TypeReference.StaticArrayTypeReference<Type<*>>(fixsize) {
                override fun getType(): java.lang.reflect.Type {
                    return componentType
                }
            }
        }
    }


    private fun decodeAsBase(typeStr: String): TypeReference<out Type<*>> {
        return TypeReference.create(
            AbiTypes.getType(typeStr)
        )
    }

}