package net.vite.wallet.network.http.vitex

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import net.vite.wallet.TokenInfoCenter


@Keep
data class CCurrencyInfoDetail(
    val symbol: String? = null,
    val name: String? = null,
    val tokenCode: String? = null,
    val platform: CCurrencyPlatform? = null,
    val icon: String? = null,
    val overview: Map<String, String>? = null,
    val states: Map<String, Int>? = null,
    val total: String? = null,
    @SerializedName("init_price")
    val initPrice: Map<String, String>? = null,
    val tokenDigit: Int? = null,
    val tokenAccuracy: String? = null,
    val publisher: String? = null,
    val publisherDate: String? = null,
    val website: String? = null,
    val whitepaper: String? = null,
    val links: Map<String, List<String>>? = null,
    val remark: String? = null,
    val updateTime: String? = null,
    val gatewayInfo: TokenGatewayInfo? = null
) {

    companion object {
        const val IssueDefaultCode = 0
        const val IssueFloatCode = 1
        const val IssueLimitCode = 2

        const val TradeDefaultCode = 0
        const val TradeNormalCode = 1
        const val TradeLockedCode = 2
    }


    fun isGatewayToken(): Boolean {
        return TokenInfoCenter.getTokenInfoInCache(tokenCode ?: "")?.isGatewayToken() ?: false
    }

    fun family(): Int {
        return when (platform?.symbol) {
            "ETH" -> TokenFamily.ETH
            "VITE" -> TokenFamily.VITE
            "GRIN" -> TokenFamily.GRIN
            else -> TokenFamily.OTHER
        }
    }
}

@Keep
data class CCurrencyPlatform(
    val name: String? = null,
    val symbol: String? = null, // = NormalTokenInfo platform
    val tokenAddress: String? = null,
    val tokenIndex: Int? = null
)

@Keep
data class TokenGatewayInfo(
    val name: String?,
    val url: String?,
    val mappedToken: NormalTokenInfo?,
    val icon: String?,
    val overview: Map<String, String>?,
    val links: Map<String, List<String>>?, // website, explorer, whitepaper, github, twitter, facebook
    private val policy: Map<String, String>?,
    val serviceSupport: String?,
    val isOfficial: Boolean?,
    val standard: String?
) {

    fun getPolicy(): String {
        return policy?.get("en") ?: ""
    }
}