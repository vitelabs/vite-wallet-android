package org.walletconnect.impls

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import net.vite.wallet.utils.fromBase64ToBytesOrNull
import net.vite.wallet.utils.hexToBytes
import net.vite.wallet.utils.toHex
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PKCS7Padding
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.util.encoders.Base64
import org.walletconnect.Session
import org.walletconnect.nullOnThrow
import org.walletconnect.types.intoMap
import java.security.SecureRandom


class MoshiPayloadAdapter(val moshi: Moshi) : Session.PayloadAdapter {

    private val payloadAdapter = moshi.adapter(EncryptedPayload::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
    )

    private fun createRandomBytes(i: Int) = ByteArray(i).also { SecureRandom().nextBytes(it) }

    override fun parse(payload: String, key: String): Session.MethodCall {
        val encryptedPayload = payloadAdapter.fromJson(payload)
            ?: throw IllegalArgumentException("Invalid json payload!")

        val padding = PKCS7Padding()
        val aes = PaddedBufferedBlockCipher(
            CBCBlockCipher(AESEngine()),
            padding
        )
        val ivAndKey = ParametersWithIV(
            KeyParameter(key.hexToBytes()),
            encryptedPayload.iv.hexToBytes()
        )
        aes.init(false, ivAndKey)

        val encryptedData = encryptedPayload.data.hexToBytes()
        val minSize = aes.getOutputSize(encryptedData.size)
        val outBuf = ByteArray(minSize)
        var len = aes.processBytes(encryptedData, 0, encryptedData.size, outBuf, 0)
        len += aes.doFinal(outBuf, len)

        return outBuf.copyOf(len).toMethodCall()
    }

    override fun prepare(data: Session.MethodCall, key: String): String {
        val bytesData = data.toBytes()
        val hexKey = key.hexToBytes()
        val iv = createRandomBytes(16)

        val padding = PKCS7Padding()
        val aes = PaddedBufferedBlockCipher(
            CBCBlockCipher(AESEngine()),
            padding
        )
        aes.init(true, ParametersWithIV(KeyParameter(hexKey), iv))

        val minSize = aes.getOutputSize(bytesData.size)
        val outBuf = ByteArray(minSize)
        val length1 = aes.processBytes(bytesData, 0, bytesData.size, outBuf, 0)
        aes.doFinal(outBuf, length1)


        val hmac = HMac(SHA256Digest())
        hmac.init(KeyParameter(hexKey))

        val hmacResult = ByteArray(hmac.macSize)
        hmac.update(outBuf, 0, outBuf.size)
        hmac.update(iv, 0, iv.size)
        hmac.doFinal(hmacResult, 0)

        return payloadAdapter.toJson(
            EncryptedPayload(
                outBuf.toHex(),
                hmac = hmacResult.toHex(),
                iv = iv.toHex()
            )
        )
    }

    /**
     * Convert FROM request bytes
     */
    private fun ByteArray.toMethodCall(): Session.MethodCall =
        String(this).let { json ->
            mapAdapter.fromJson(json)?.let {
                try {
                    val method = it["method"]
                    when (method) {
                        "vc_sessionRequest" -> it.toSessionRequest()
                        "vc_sessionUpdate" -> it.toSessionUpdate()
                        "vc_peerPing" -> it.toPeerPing()
                        "vite_signAndSendTx" -> it.toSendTransaction()
                        "vite_signMessage" -> it.toSignMessage()
                        null -> it.toResponse()
                        else -> it.toCustom()
                    }
                } catch (e: Exception) {
                    throw Session.MethodCallException.InvalidRequest(
                        it.getId(), "$json (${
                            e.message
                                ?: "Unknown error"
                        })"
                    )
                }
            } ?: throw IllegalArgumentException("Invalid json")
        }

    private fun Map<String, *>.getId(): Long =
        (this["id"] as? Double)?.toLong() ?: throw IllegalArgumentException("id missing")

    private fun Map<String, *>.toSessionRequest(): Session.MethodCall.SessionRequest {
        val params = this["params"] as? List<*> ?: throw IllegalArgumentException("params missing")
        val data = params.firstOrNull() as? Map<*, *>
            ?: throw IllegalArgumentException("Invalid params")

        return Session.MethodCall.SessionRequest(
            getId(),
            data.extractPeerData()
        )
    }

    private fun Map<String, *>.toSessionUpdate(): Session.MethodCall.SessionUpdate {
        val params = this["params"] as? List<*> ?: throw IllegalArgumentException("params missing")
        val data = params.firstOrNull() as? Map<*, *>
            ?: throw IllegalArgumentException("Invalid params")
        val approved = data["approved"] as? Boolean
            ?: throw IllegalArgumentException("approved missing")
        val chainId = data["chainId"] as? Long
        val accounts = nullOnThrow { (data["accounts"] as? List<*>)?.toStringList() }
        return Session.MethodCall.SessionUpdate(
            getId(),
            Session.SessionParams(
                approved,
                chainId,
                accounts,
                nullOnThrow { data.extractPeerData() })
        )
    }

    private fun Map<String, *>.toPeerPing(): Session.MethodCall.Ping {
        return Session.MethodCall.Ping(getId())
    }

    private fun Map<String, *>.toSignMessage(): Session.MethodCall.SignMessage {
        val params = this["params"] as? List<*> ?: throw IllegalArgumentException("params missing")

        val params0 = params.firstOrNull() as? Map<*, *>
            ?: throw IllegalArgumentException("Invalid params")

        val message = params0["message"] as? String
            ?: throw IllegalArgumentException("Invalid block")

        return Session.MethodCall.SignMessage(
            getId(),
            message.fromBase64ToBytesOrNull()?.toHex() ?: ""
        )
    }

    private fun Map<String, *>.toSendTransaction(): Session.MethodCall.SendTransaction {
        val params = this["params"] as? List<*> ?: throw IllegalArgumentException("params missing")
        val params0 = params.firstOrNull() as? Map<*, *>
            ?: throw IllegalArgumentException("Invalid params")
        val block = kotlin.run {
            val block = params0["block"] as? Map<*, *>
                ?: throw IllegalArgumentException("Invalid block")
            val toAddress = block["toAddress"] as? String
                ?: throw IllegalArgumentException("missing toAddress")
            val tokenId = block["tokenId"] as? String
                ?: throw IllegalArgumentException("missing tokenId")
            val amount = block["amount"] as? String
                ?: throw IllegalArgumentException("missing amount")
            val blockType = (block["blockType"] as? Double)?.toInt()?.toByte()
            val fee = block["fee"] as? String
            val data = (block["data"] as? String)?.let {
                Base64.decode(it)
            }

            Session.MethodCall.ViteBlock(toAddress, tokenId, amount, fee, data, blockType)
        }

        val abi = params0["abi"] as? String

        val description = kotlin.run {
            val description = params0["description"] as? Map<*, *>?
            description?.let {
                val function = description["function"] as? Map<String, Map<String, String>>
                    ?: throw IllegalArgumentException("missing function")
                val inputs = description["inputs"] as? List<Map<String, Map<String, String>>>
                Session.MethodCall.Description(function, inputs)
            }
        }

        val extend = kotlin.run {
            val extend = params0["extend"] as? Map<*, *>?
            extend?.let {
                val type = extend["type"] as? String
                    ?: throw IllegalArgumentException("missing extend type")
                val side = (extend["side"] as? Double)?.toInt()
                val tradeTokenSymbol = extend["tradeTokenSymbol"] as? String
                val quoteTokenSymbol = extend["quoteTokenSymbol"] as? String
                val price = extend["price"] as? String
                val fee = extend["fee"] as? String
                val cost = extend["cost"] as? String
                val amount = extend["amount"] as? String
                val labelTitle = extend["labelTitle"] as? Map<String, Map<String, String>>

                Session.MethodCall.SendTransactionExtension(
                    type = type,
                    cost = cost,
                    fee = fee,
                    labelTitle = labelTitle,
                    side = side,
                    tradeTokenSymbol = tradeTokenSymbol,
                    quoteTokenSymbol = quoteTokenSymbol,
                    price = price,
                    amount = amount
                )
            }

        }
        return Session.MethodCall.SendTransaction(getId(), block, abi, description, extend)
    }

    private fun Map<String, *>.toCustom(): Session.MethodCall.Custom {
        val method = this["method"] as? String ?: throw IllegalArgumentException("method missing")
        val params = this["params"] as? List<*>
        return Session.MethodCall.Custom(getId(), method, params)
    }

    private fun Map<String, *>.toResponse(): Session.MethodCall.Response {
        val result = this["result"]
        val error = this["error"] as? Map<*, *>
        if (result == null && error == null) throw IllegalArgumentException("no result or error")
        return Session.MethodCall.Response(
            getId(),
            result,
            error?.extractError()
        )
    }

    private fun Map<*, *>.extractError(): Session.Error {
        val code = (this["code"] as? Double)?.toLong()
        val message = this["message"] as? String
        return Session.Error(code ?: 0, message ?: "Unknown error")
    }

    private fun Map<*, *>.extractPeerData(): Session.PeerData {
        val peerId = this["peerId"] as? String ?: throw IllegalArgumentException("peerId missing")
        val peerMeta = this["peerMeta"] as? Map<*, *>
        return Session.PeerData(peerId, peerMeta.extractPeerMeta())
    }

    private fun Map<*, *>?.extractPeerMeta(): Session.PeerMeta {
        val description = this?.get("description") as? String
        val url = this?.get("url") as? String
        val name = this?.get("name") as? String
        val lastAccount = this?.get("lastAccount") as? String
        val icons = nullOnThrow { (this?.get("icons") as? List<*>)?.toStringList() }
        return Session.PeerMeta(
            url = url,
            name = name,
            description = description,
            icons = icons,
            lastAccount = lastAccount
        )
    }

    private fun List<*>.toStringList(): List<String> =
        this.map {
            (it as? String) ?: throw IllegalArgumentException("List contains non-String values")
        }

    /**
     * Convert INTO request bytes
     */
    private fun Session.MethodCall.toBytes() =
        mapAdapter.toJson(
            when (this) {
                is Session.MethodCall.SessionRequest -> this.toMap()
                is Session.MethodCall.Response -> this.toMap()
                is Session.MethodCall.SendTransaction -> this.toMap()
                is Session.MethodCall.Custom -> this.toMap()
                is Session.MethodCall.Ping -> this.toMap()
                else -> mapOf()
            }
        ).toByteArray()


    private fun Session.MethodCall.Ping.toMap() =
        jsonRpc(id, "vc_peerPing")

    private fun Session.MethodCall.SessionRequest.toMap() =
        jsonRpc(id, "vc_sessionRequest", peer.intoMap())


    private fun Session.MethodCall.SendTransaction.toMap() =
        jsonRpc(
            id, "eth_sendTransaction", emptyMap<String, String>()
//                    mapOf(
//                            "from" to from,
//                            "to" to to,
//                            "nonce" to nonce,
//                            "gasPrice" to gasPrice,
//                            "gasLimit" to gasLimit,
//                            "value" to value,
//                            "data" to data
//                    )
        )


    private fun Session.MethodCall.Response.toMap() =
        mutableMapOf(
            "id" to id as Any,
            "jsonrpc" to "2.0"
        ).apply {
            result?.let { this["result"] = result }
            error?.let { this["error"] = error.intoMap() }
        }

    private fun Session.MethodCall.Custom.toMap() =
        jsonRpcWithList(
            id, method, params ?: emptyList<Any>()
        )

    private fun jsonRpc(id: Long, method: String, vararg params: Any) =
        jsonRpcWithList(id, method, params.asList())

    private fun jsonRpcWithList(id: Long, method: String, params: List<*>) =
        mapOf(
            "id" to id,
            "jsonrpc" to "2.0",
            "method" to method,
            "params" to params
        )

    data class EncryptedPayload(
        @Json(name = "data") val data: String,
        @Json(name = "iv") val iv: String,
        @Json(name = "hmac") val hmac: String
    )
}
