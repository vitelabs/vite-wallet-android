package net.vite.wallet.utils

import android.util.Base64
import android.util.TypedValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import net.vite.wallet.ViteConfig
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.experimental.and


const val ZeroHash = "0000000000000000000000000000000000000000000000000000000000000000"
private val HEX_CHARS = "0123456789abcdef"

fun String.hexToBytes(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i]);
        val secondIndex = HEX_CHARS.indexOf(this[i + 1]);

        val octet = firstIndex.shl(4).or(secondIndex)
        result[i.shr(1)] = octet.toByte()
    }

    return result
}

fun Byte.toHex(): String {
    val result = StringBuilder()
    val octet = toInt()
    val firstIndex = (octet and 0xF0).ushr(4)
    val secondIndex = octet and 0x0F
    result.append(HEX_CHARS[firstIndex])
    result.append(HEX_CHARS[secondIndex])
    return result.toString()
}

fun ByteArray.toHex(): String {
    val result = StringBuilder()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString()
}

fun ByteArray?.isUTF8(): Boolean {
    val bytes = this
    if (bytes != null && bytes.isNotEmpty()) {
        var foundStartByte = false
        var requireByte = 0
        for (i in bytes.indices) {
            val current: Byte = bytes[i]
            //当前字节小于128，标准ASCII码范围
            if (current and 0x80.toByte() == 0x00.toByte()) {
                if (foundStartByte) {
                    foundStartByte = false
                    requireByte = 0
                }
                continue
                //当前以0x110开头，标记2字节编码开始，后面需紧跟1个0x10开头字节
            } else if (current and 0xC0.toByte() == 0xC0.toByte()) {
                foundStartByte = true
                requireByte = 1
                //当前以0x1110开头，标记3字节编码开始，后面需紧跟2个0x10开头字节
            } else if (current and 0xE0.toByte() == 0xE0.toByte()) {
                foundStartByte = true
                requireByte = 2
                //当前以0x11110开头，标记4字节编码开始，后面需紧跟3个0x10开头字节
            } else if (current and 0xF0.toByte() == 0xF0.toByte()) {
                foundStartByte = true
                requireByte = 3
                //当前以0x111110开头，标记5字节编码开始，后面需紧跟4个0x10开头字节
            } else if (current and 0xE8.toByte() == 0xE8.toByte()) {
                foundStartByte = true
                requireByte = 4
                //当前以0x1111110开头，标记6字节编码开始，后面需紧跟5个0x10开头字节
            } else if (current and 0xEC.toByte() == 0xEC.toByte()) {
                foundStartByte = true
                requireByte = 5
                //当前以0x10开头，判断是否满足utf8编码规则
            } else if (current and 0x80.toByte() == 0x80.toByte()) {
                if (foundStartByte) {
                    requireByte--
                    //出现多个0x10开头字节，个数满足，发现utf8编码字符，直接返回
                    if (requireByte == 0) {
                        return true
                    }
                    //虽然经当前以0x10开头，但前一字节不是以0x110|0x1110|0x11110肯定不是utf8编码，直接返回
                } else {
                    return false
                }
                //发现0x8000~0xC000之间字节，肯定不是utf8编码
            } else {
                return false
            }
        }
    }
    return false
}


fun Float.dp2px(): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        ViteConfig.get().context.resources.displayMetrics
    )
}


fun BigInteger.toLeftPadByteArray(leftPadSize: Int? = null): ByteArray {
    val rawdata = this.toByteArray()
    return if (leftPadSize != null && leftPadSize > rawdata.size) {
        val data = ByteArray(leftPadSize)
        val destPost = data.size - rawdata.size
        System.arraycopy(rawdata, 0, data, destPost, rawdata.size)
        data
    } else {
        rawdata
    }
}

fun BigInteger.toSafeByteArray(): ByteArray {
//  java`s bigint.tobytearry will add a 0 in the first sometime
    var data = this.toByteArray()
    if (data.size == 1 && data[0].toInt() == 0) {
        return ByteArray(0)
    }

    if (data.size != 1 && data[0].toInt() == 0) {
        val tmp = ByteArray(data.size - 1)
        System.arraycopy(data, 1, tmp, 0, tmp.size)
        data = tmp
    }
    return data
}

fun Disposable.addTo(c: CompositeDisposable): Boolean {
    return c.add(this)
}

fun String.fromBase64ToBytesOrNull(): ByteArray? {
    return kotlin.runCatching { Base64.decode(this, Base64.NO_WRAP) }.getOrNull()
}

fun ByteArray.toBase64String(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}

//Rfc4648
fun base64UrlSafeEncode(byteArray: ByteArray): String {
    val s = Base64.encodeToString(byteArray, Base64.NO_WRAP or Base64.NO_PADDING)
    return s.replace('+', '-')
        .replace('/', '_')
}

fun base64UrlSafeDecode(s: String): ByteArray {
    val ss = s.replace('-', '+').replace('_', '/')
    return Base64.decode(
        s.replace('-', '+')
            .replace('_', '/'), Base64.NO_WRAP or Base64.NO_PADDING
    )
}

fun File.mustMakeDirs(): File {
    if (exists() && !isDirectory) {
        delete()
    }
    if (!exists()) {
        mkdirs()
    }
    return this
}


fun sha256(b: ByteArray): ByteArray {
    val m = MessageDigest.getInstance("SHA-256")
    return m.digest(b)
}


