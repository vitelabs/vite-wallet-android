package net.vite.wallet.vep

import androidx.annotation.Keep
import java.net.URLDecoder


class InvalidViteBridgeUrlTException(msg: String) : Exception(msg)


@Keep
data class ViteBridgeUrlTransferParams(
    val handshakeTopic: String,
    val bridge: String,
    val key: String,
    val version: Int? = null
)


fun decodeViteBridgeUrl(uri: String): ViteBridgeUrlTransferParams {
    val protocolSeparator = uri.indexOf(':')
    val handshakeTopicSeparator = uri.indexOf('@', startIndex = protocolSeparator)
    val versionSeparator = uri.indexOf('?')
    val protocol = uri.substring(0, protocolSeparator)
    val handshakeTopic = uri.substring(protocolSeparator + 1, handshakeTopicSeparator)
    val version = Integer.valueOf(uri.substring(handshakeTopicSeparator + 1, versionSeparator))
    val params = uri.substring(versionSeparator + 1).split("&").associate {
        it.split("=").let { param -> param.first() to URLDecoder.decode(param[1], "UTF-8") }
    }
    val bridge = params["bridge"] ?: throw IllegalArgumentException("Missing bridge param in URI")
    val key = params["key"] ?: throw IllegalArgumentException("Missing key param in URI")
    return ViteBridgeUrlTransferParams(
        handshakeTopic = handshakeTopic,
        bridge = bridge,
        key = key,
        version = version
    )
}
