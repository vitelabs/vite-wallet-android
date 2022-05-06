package net.vite.wallet.network.rpc

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import net.vite.wallet.network.toLocalReadableText
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap


@Keep
data class EthAccountInfo(
    val ethTokenBalance: ConcurrentHashMap<String, BigInteger> = ConcurrentHashMap()
)

@Keep
data class ViteAccountInfo(
    val balance: AccountBalanceInfo?,
    val onroadBalance: AccountBalanceInfo?,
    val stakeToSBPAll: BigInteger? = BigInteger.ZERO,
    val stateToFullNodeAll: BigInteger? = BigInteger.ZERO,
    val stakeForDexMining: BigInteger? = BigInteger.ZERO,
    val stakeForDexVip: BigInteger? = BigInteger.ZERO,
)

fun ViteAccountInfo?.getBalance(tokenId: String) =
    this?.balance?.tokenBalanceInfoMap?.get(tokenId)?.totalAmount?.toBigIntegerOrNull()
        ?: BigInteger.ZERO

fun ViteAccountInfo?.getBalanceReadableText(scale: Int, tokenId: String): String {
    return this?.balance?.tokenBalanceInfoMap?.get(tokenId)?.let {
        it.viteChainTokenInfo?.amountText(
            it.totalAmount?.toBigIntegerOrNull() ?: BigInteger.ZERO,
            scale
        )
    } ?: BigDecimal.ZERO.toLocalReadableText(scale)
}


@Keep
data class AccountBalanceInfo(
    @SerializedName("accountAddress")
    val accountAddress: String? = null,
    @SerializedName("totalNumber")
    val totalNumber: String? = null,
    @SerializedName("tokenBalanceInfoMap")
    val tokenBalanceInfoMap: Map<String, BalanceInfo> = HashMap()
)

@Keep
data class BalanceInfo(
    @SerializedName("tokenInfo")
    val viteChainTokenInfo: ViteChainTokenInfo?,
    @SerializedName("totalAmount")
    val totalAmount: String?,
    @SerializedName("number")
    val number: String?
)