package net.vite.wallet.network.rpc

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import net.vite.wallet.network.UnitCastTokenInfo
import net.vite.wallet.network.amountInBase
import net.vite.wallet.network.toLocalReadableText
import java.math.BigDecimal
import java.math.BigInteger

val ViteTokenInfo = ViteChainTokenInfo(
    tokenName = "Vite Token",
    symbol = "VITE",
    totalSupply = "1000000000000000000000000000",
    decimals = 18,
    owner = "vite_60e292f0ac471c73d914aeff10bb25925e13b2a9fddb6e6122",
    pledgeAmount = "0",
    withdrawHeight = "0",
    tokenId = "tti_5649544520544f4b454e6e40"
)

val VcpTokenInfo = ViteChainTokenInfo(
    tokenName = "Vite Community Point",
    symbol = "VCP",
    totalSupply = "10000000000",
    decimals = 0,
    owner = "vite_60e292f0ac471c73d914aeff10bb25925e13b2a9fddb6e6122",
    pledgeAmount = "0",
    withdrawHeight = "0",
    tokenId = "tti_251a3e67a41b5ea2373936c8"
)

val VxTokenInfo = ViteChainTokenInfo(
    tokenName = "ViteX Coin",
    symbol = "VX",
    totalSupply = "29328807800509062115871669",
    decimals = 18,
    owner = "vite_050697d3810c30816b005a03511c734c1159f50907662b046f",
    pledgeAmount = "0",
    withdrawHeight = "0",
    tokenId = "tti_564954455820434f494e69b5"
)

@Keep
data class ViteChainTokenInfo(
    @SerializedName("tokenName")
    val tokenName: String?,
    @SerializedName("tokenSymbol")
    val symbol: String?,
    @SerializedName("totalSupply")
    val totalSupply: String?,
    @SerializedName("decimals")
    val decimals: Int?,
    @SerializedName("owner")
    val owner: String?,
    @SerializedName("pledgeAmount")
    val pledgeAmount: String?,
    @SerializedName("withdrawHeight")
    val withdrawHeight: String?,
    @SerializedName("tokenId")
    val tokenId: String?
) : UnitCastTokenInfo {
    override fun baseToSmallestUnit(value: BigDecimal): BigDecimal {
        return value.multiply(BigDecimal.TEN.pow(decimals!!))
    }

    override fun smallestToBaseUnit(value: BigDecimal): BigDecimal {
        return value.divide(BigDecimal.TEN.pow(decimals!!))
    }

    override fun amountText(amount: BigInteger, scale: Int): String {
        return amount.amountInBase(decimals!!, scale).toLocalReadableText(scale)
    }

    override fun amountTextWithSymbol(amount: BigInteger, scale: Int): String {
        return amountText(amount, scale) + " " + symbol
    }

}
