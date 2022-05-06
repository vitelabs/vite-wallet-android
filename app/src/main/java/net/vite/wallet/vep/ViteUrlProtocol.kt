package net.vite.wallet.vep

import androidx.annotation.Keep
import net.vite.wallet.constants.DexCancelContractAddress
import net.vite.wallet.constants.DexContractAddress
import net.vite.wallet.network.rpc.ViteTokenInfo
import net.vite.wallet.utils.base64UrlSafeDecode
import org.vitelabs.mobile.Mobile

// THIS IS VERY SIMPLE URL DECODE/ENCODE FOR URL TRANSFER.NEXT WE WILL PUBLISH STANDARD VEP-URL IMPLEMENTATION

class InvalidViteUrlException(msg: String) : Exception(msg)

@Keep
data class ViteUrlTransferParams(
    val toAddr: String,
    val tti: String,
    val amount: String? = null,
    val data: ByteArray? = null,
    val funcName: String? = null
) {
    fun isFunctionCall() = funcName != null
}

fun encodeToVepUrl(p: ViteUrlTransferParams): String {
    return "vite:${p.toAddr}" +
            (p.tti.let { "?tti=$it" }) +
            (p.amount.let { "&amount=$it" }) +
            (p.data?.let { "&data=${String(it)}" } ?: "")
}

fun mustDecodeViteTransferUrl(url: String): ViteUrlTransferParams {
    val scheme = url.split(":")
    if (scheme.size != 2 || scheme[0] != "vite") {
        throw InvalidViteUrlException("Decode : error")
    }

    val addrWithFunc = scheme[1].split("?")

    if (addrWithFunc.size > 2) {
        throw InvalidViteUrlException("Decode ? error")
    }

    val func = addrWithFunc[0].split("/")
    val toAddr = Mobile.isValidAddress(func[0])
    if (!toAddr) {
        throw InvalidViteUrlException("Decode / error")
    }

    val funcName = if (func.size == 2) {
        func[1]
    } else {
        null
    }

    if (addrWithFunc.size == 1) {
        return ViteUrlTransferParams(
            toAddr = func[0],
            tti = ViteTokenInfo.tokenId!!,
            funcName = funcName
        )
    }

    if (addrWithFunc.size == 2) {
        val params = addrWithFunc[1].split("&").map {
            val kv = it.split("=")
            if (kv.size != 2) {
                throw InvalidViteUrlException("Decode kv error")
            }
            kv[0] to kv[1]
        }.toMap()

        val tti = params["tti"] ?: ViteTokenInfo.tokenId!!
        if (!Mobile.isValidTokenTypeId(tti)) {
            throw  InvalidViteUrlException("wrong tti")
        }

        if (params["amount"] != null && params["amount"]!!.toBigDecimalOrNull() == null) {
            throw  InvalidViteUrlException("wrong amount")
        }

        val dataArray = params["data"]?.let { data ->
            base64UrlSafeDecode(data)
        }

        val result = ViteUrlTransferParams(
            toAddr = func[0],
            tti = tti,
            amount = params["amount"],
            data = dataArray,
            funcName = funcName
        )

        if (Mobile.isContactAddress(result.toAddr) && !result.isFunctionCall()) {
            throw InvalidViteUrlException("target address is Contact but it is not a functionCall")
        }
        if (!Mobile.isContactAddress(result.toAddr) && result.isFunctionCall()) {
            throw InvalidViteUrlException("target address is Account but it is a functionCall")
        }

        return result
    }
    throw InvalidViteUrlException(scheme[1])
}

fun decodeViteTransferUrl(url: String): ViteUrlTransferParams {
    val params = mustDecodeViteTransferUrl(url)
    if (params.toAddr == DexContractAddress || params.toAddr == DexCancelContractAddress) {
        throw InvalidViteUrlException("not support dex url")
    }
    return params
}

