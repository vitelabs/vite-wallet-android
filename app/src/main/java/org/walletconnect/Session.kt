package org.walletconnect

import java.net.URLDecoder
import java.nio.ByteBuffer

interface Session {

    data class Config(
        val handshakeTopic: String,
        val bridge: String,
        val key: String,
        val protocol: String = "wc",
        val version: Int = 1
    ) {
        companion object {
            fun fromWCUri(uri: String): Config {
                val protocolSeparator = uri.indexOf(':')
                val handshakeTopicSeparator = uri.indexOf('@', startIndex = protocolSeparator)
                val versionSeparator = uri.indexOf('?')
                val protocol = uri.substring(0, protocolSeparator)
                val handshakeTopic = uri.substring(protocolSeparator + 1, handshakeTopicSeparator)
                val version =
                    Integer.valueOf(uri.substring(handshakeTopicSeparator + 1, versionSeparator))
                val params = uri.substring(versionSeparator + 1).split("&").associate {
                    it.split("=")
                        .let { param -> param.first() to URLDecoder.decode(param[1], "UTF-8") }
                }
                val bridge = params["bridge"]
                    ?: throw IllegalArgumentException("Missing bridge param in URI")
                val key = params["key"]
                    ?: throw IllegalArgumentException("Missing key param in URI")
                return Config(handshakeTopic, bridge, key, protocol, version)
            }
        }
    }

    interface Callback {

        fun handleMethodCall(call: MethodCall)

        fun sessionApproved()

        fun sessionInterrupt()

        fun sessionClosed()
    }

    interface PayloadAdapter {
        fun parse(payload: String, key: String): MethodCall
        fun prepare(data: MethodCall, key: String): String

    }


    sealed class MethodCallException(val id: Long, message: String) :
        IllegalArgumentException(message) {
        class InvalidRequest(id: Long, request: String) :
            MethodCallException(id, "Invalid request: $request")
    }

    sealed class MethodCall(private val internalId: Long) {
        fun id() = internalId

        data class SessionRequest(val id: Long, val peer: PeerData) : MethodCall(id)

        data class SessionUpdate(val id: Long, val params: SessionParams) : MethodCall(id)

        data class ViteBlock(
            val toAddress: String,
            val tokenId: String,
            val amount: String,
            val fee: String? = null,
            val data: ByteArray? = null,
            val blockType: Byte? = null
        ) {
            fun dataType(): Short? {
                val d = data ?: return null
                if (d.size < 2) {
                    return null
                }
                val type = ByteArray(2)
                type[0] = d[0]
                type[1] = d[1]
                return ByteBuffer.wrap(type).getShort()
            }

        }


        class Description(
            val function: Map<String, Map<String, String>>? = null,
            val inputs: List<Map<String, Map<String, String>>>? = null
        )


        // 要和 DATA的前几个字段比较
        data class SendTransactionExtension(
            val type: String,
            val cost: String? = null, // for dex newinvite
            val fee: String? = null, //for crosschain transfer and dexNewMarket
            val labelTitle: Map<String, Map<String, String>>? = null,//for crosschain transfer
            val side: Int? = null, // for dexCancel
            val tradeTokenSymbol: String? = null,// for dexCancel
            val quoteTokenSymbol: String? = null,// for dexCancel
            val price: String? = null,// for dexCancel
            val amount: String? = null // for dexFundPledgeForVip
        ) {
            companion object {
                const val CrossChainTransfer = "crossChainTransfer"
                const val DexCancel = "dexCancel"
                const val DexNewInviter = "dexNewInviter"
                const val DexNewMarket = "dexNewMarket"
                const val DexFundPledgeForVip = "dexFundPledgeForVip"
            }
        }


        data class SendTransaction(
            val id: Long,
            val block: ViteBlock,
            val abi: String? = null,
            val description: Description? = null,
            val extend: SendTransactionExtension? = null
        ) : MethodCall(id)

        data class SignMessage(
            val id: Long,
            val message: String
        ) : MethodCall(id)


        data class VCTask(val type: Int) {
            companion object {
                val TYPE_SIGN_MESSAGE = 1
                val TYPE_SEND_TRANSACTION = 2
            }

            fun getObject(): Any? {
                return when (type) {
                    VCTask.TYPE_SIGN_MESSAGE -> signMessage
                    VCTask.TYPE_SEND_TRANSACTION -> sendTransaction
                    else -> null
                }
            }

            var signMessage: SignMessage? = null
            var sendTransaction: SendTransaction? = null
        }

        data class Custom(val id: Long, val method: String, val params: List<*>?) : MethodCall(id)

        data class Response(val id: Long, val result: Any?, val error: Error? = null) :
            MethodCall(id)

        data class Ping(val id: Long) : MethodCall(id)
    }

    data class PeerData(val id: String, val meta: PeerMeta?)
    data class PeerMeta(
        val url: String? = null,
        val lastAccount: String? = null,
        val name: String? = null, // client
        val description: String? = null,
        val icons: List<String>? = null,
        val version: String? = null, // client
        val versionCode: String? = null, // client
        val bundleId: String? = null, // client packagename
        val platform: String? = null,// client
        val language: String? = null// client
    )

    data class SessionParams(
        val approved: Boolean,
        val chainId: Long?,
        val accounts: List<String>?,
        val peerData: PeerData?
    )

    data class Error(val code: Long, val message: String) {
        companion object {
            fun rejectRequestError(message: String): Error {
                return Error(11011, message)
            }

            fun rejectSessionError(message: String): Error {
                return Error(11010, message)
            }

            fun requestCancel(message: String = ""): Error {
                return Error(11012, message)
            }
        }
    }
}
