package net.vite.wallet.network.http.vitex

import android.graphics.Color
import androidx.annotation.Keep
import net.vite.wallet.ExternalPriceCenter
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.ViteConfig
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.DexAccountFundInfoPoll
import net.vite.wallet.balance.poll.EthAccountInfoPoll
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.balance.quota.QuotaPledgeInfoPoll
import net.vite.wallet.network.UnitCastTokenInfo
import net.vite.wallet.network.amountInBase
import net.vite.wallet.network.rpc.ViteTokenInfo
import net.vite.wallet.network.rpc.getBalance
import net.vite.wallet.network.toLocalReadableText
import java.math.BigDecimal
import java.math.BigInteger

object TokenFamily {
    const val VITE = 923
    const val ETH = 924
    const val GRIN = 925
    const val OTHER = 900
}


@Keep
data class PinnedTokenInfo(
    val tokenCode: String,
    val family: Int,
    val name: String,
    val symbol: String
) {
    companion object {
        fun from(normalTokenInfo: NormalTokenInfo): PinnedTokenInfo {
            return PinnedTokenInfo(
                tokenCode = normalTokenInfo.tokenCode ?: "",
                family = normalTokenInfo.family(),
                name = normalTokenInfo.name ?: "",
                symbol = normalTokenInfo.symbol ?: ""
            )
        }
    }

    fun familyName(): String {
        if (TokenInfoCenter.EthEthTokenCodes.tokenCode == tokenCode) {
            return "Ethereum Coin"
        }
        if (TokenInfoCenter.ViteViteTokenCodes.tokenCode == tokenCode) {
            return "Vite Coin"
        }
        return when (family) {
            TokenFamily.ETH -> "ERC20 Token"
            TokenFamily.VITE -> "Vite Token"
            TokenFamily.GRIN -> "Grin Coin"
            else -> ""
        }
    }

    fun mainColor(): Int {
        return Color.parseColor(
            when (family) {
                TokenFamily.ETH -> "#ff5bc500"
                TokenFamily.VITE -> "#ff007aff"
                TokenFamily.GRIN -> "#ffFFDB4D"
                else -> "#ff5bc500"
            }
        )
    }
}


@Keep
data class NormalTokenInfo(
    val decimal: Int? = null,
    val icon: String? = null,
    val name: String? = null,
    val platform: String? = null,
    val symbol: String? = null,
    val tokenAddress: String? = null,
    val tokenCode: String? = null,
    val tokenIndex: Int? = null,
    val standard: String? = null,
    val gatewayInfo: TokenGatewayInfo? = null,
    val mappedTokenExtras: List<NormalTokenInfo?>? = null,
    val url: String? = null
) : UnitCastTokenInfo {

    fun allMappedToken(): List<NormalTokenInfo> {
        val list = ArrayList<NormalTokenInfo>()
        gatewayInfo?.mappedToken?.let {
            list.add(it)
        }
        gatewayInfo?.mappedToken?.mappedTokenExtras?.filterNotNull()?.let {
            list.addAll(it)
        }
        return list
    }

    fun standard() = standard?.takeIf { it.isNotEmpty() } ?: "Native"

    fun uniqueName(): String {
        if (symbol == "VITE" || symbol == "VX" || symbol == "VCP" || tokenIndex == null) {
            return symbol ?: ""
        }
        if (tokenIndex >= 1000) {
            return "$symbol-$tokenIndex"
        }
        return "$symbol-${("000$tokenIndex").substring(tokenIndex.toString().length)}"
    }

    fun isGatewayToken(): Boolean {
        return gatewayInfo?.mappedToken?.tokenCode != null
    }

    fun tokenAddressEqual(other: String): Boolean {
        return other.equals(tokenAddress, ignoreCase = true)
    }

    fun family(): Int {
        return when (platform) {
            "ETH" -> TokenFamily.ETH
            "VITE" -> TokenFamily.VITE
            "GRIN" -> TokenFamily.GRIN
            else -> TokenFamily.OTHER
        }
    }


    fun getGasLimit(): BigInteger {
        return 200000.toBigInteger()
    }

    fun familyName(): String? {
        if (TokenInfoCenter.EthEthTokenCodes.tokenCode == tokenCode) {
            return "Ethereum Coin"
        }
        if (TokenInfoCenter.ViteViteTokenCodes.tokenCode == tokenCode) {
            return "Vite Coin"
        }
        return when (family()) {
            TokenFamily.ETH -> "ERC20 Token"
            TokenFamily.VITE -> "Vite Token"
            TokenFamily.GRIN -> "Grin Coin"
            else -> null
        }
    }


    fun balance(): BigInteger {
        return when (family()) {
            TokenFamily.ETH -> EthAccountInfoPoll.myLatestEthAccountInfo()?.ethTokenBalance?.get(
                tokenCode ?: ""
            )
            TokenFamily.VITE -> (ViteAccountInfoPoll.myLatestViteAccountInfo()?.getBalance(
                tokenAddress ?: ""
            ) ?: BigInteger.ZERO) + (if (tokenAddress == ViteTokenInfo.tokenId) {
                val quotaVite = AccountCenter.currentViteAddress()?.let { addr ->
                    QuotaPledgeInfoPoll.getLatestData(addr)
                }?.totalPledgeAmount?.toBigIntegerOrNull() ?: BigInteger.ZERO

                quotaVite + (ViteAccountInfoPoll.myLatestViteAccountInfo()?.let {
                    (it.stakeToSBPAll ?: BigInteger.ZERO + (it.stateToFullNodeAll
                        ?: BigInteger.ZERO))
                } ?: BigInteger.ZERO)
            } else BigInteger.ZERO)

            else -> BigInteger.ZERO
        } ?: BigInteger.ZERO
    }

    fun walletTotalBalance(): BigDecimal {
        val all = balance().toBigDecimal()
        return smallestToBaseUnit(all)
    }

    fun walletTotalBalanceValue(): BigDecimal {
        return walletTotalBalance().multiply(ExternalPriceCenter.getPrice(tokenCode ?: ""))
    }

    fun balanceText(scale: Int): String {
        return smallestToBaseUnit(balance().toBigDecimal()).toLocalReadableText(scale)
    }

    fun balanceValue(): BigDecimal {
        return smallestToBaseUnit(balance().toBigDecimal())
            .multiply(ExternalPriceCenter.getPrice(tokenCode ?: ""))
    }

    fun balanceValueBTC(): BigDecimal {
        return smallestToBaseUnit(balance().toBigDecimal())
            .multiply(ExternalPriceCenter.getTokenBtcEquivalentPrice(tokenCode ?: ""))
    }

    fun dexAllBalance(): BigDecimal {
        return DexAccountFundInfoPoll.myLatestViteAccountFundInfo()?.get(tokenAddress)
            ?.getAllBase() ?: BigDecimal.ZERO
    }

    fun dexAvailableBalance(): BigDecimal {
        return DexAccountFundInfoPoll.myLatestViteAccountFundInfo()?.get(tokenAddress)
            ?.getAvailableBase() ?: BigDecimal.ZERO
    }

    fun dexLockedBalance(): BigDecimal {
        return DexAccountFundInfoPoll.myLatestViteAccountFundInfo()?.get(tokenAddress)
            ?.getLockedBase() ?: BigDecimal.ZERO

    }

    fun dexAllBalanceValue(): BigDecimal {
        return dexAllBalance().multiply(ExternalPriceCenter.getPrice(tokenCode ?: ""))
            ?: BigDecimal.ZERO
    }


    fun dexAllBalanceValueBTC(): BigDecimal {
        return dexAllBalance().multiply(
            ExternalPriceCenter.getTokenBtcEquivalentPrice(
                tokenCode ?: ""
            )
        ) ?: BigDecimal.ZERO
    }

    override fun baseToSmallestUnit(value: BigDecimal): BigDecimal {
        return value.multiply(BigDecimal.TEN.pow(decimal!!))
    }

    override fun smallestToBaseUnit(value: BigDecimal): BigDecimal {
        return value.divide(BigDecimal.TEN.pow(decimal!!))
    }

    override fun amountText(amount: BigInteger, scale: Int): String {
        return amount.amountInBase(decimal!!, scale).toLocalReadableText(scale)
    }

    override fun amountTextWithSymbol(amount: BigInteger, scale: Int): String {
        return amountText(amount, scale) + " " + symbol
    }


    companion object {
        val EMPTY = NormalTokenInfo()
    }
}

fun BigDecimal?.toCurrencyText(scale: Int = 2, needApproximate: Boolean = false): String {
    return if (needApproximate) {
        "≈"
    } else {
        ""
    } + ViteConfig.get().currentCurrencySymbol() + this.toLocalReadableText(scale, false)
}
