package net.vite.wallet.vep

import net.vite.wallet.isValidEthAddress
import java.net.URL


data class QrcodeParseResult(
    val result: Any,
    val rawData: String,
    val requestCode: Int? = null,
    val isCancel: Boolean = false
)

object QrcodeParser {
    fun parse(originUrl: String): Any {
        val url = originUrl.trim()

        if (isValidEthAddress(url)) {
            return EthUrlTransferParams(toAddr = url)
        }

        try {
            return decodeViteTransferUrl(url)
        } catch (e: Exception) {
        }

        try {
            return decodeEthErc20CommunityUrl(url)
        } catch (e: Exception) {
        }

        try {
            return decodeEthTransferUrl(url)
        } catch (e: Exception) {
        }

        try {
            return decodeViteBridgeUrl(url)
        } catch (e: Exception) {
        }

        try {
            val u = URL(url)
            if (u.host.isNullOrEmpty()) {
                throw Exception("empty host")
            }
            return u

        } catch (e: Exception) {
        }

        return originUrl
    }
}