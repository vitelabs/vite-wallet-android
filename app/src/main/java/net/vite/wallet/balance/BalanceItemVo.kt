package net.vite.wallet.balance

import androidx.annotation.Keep
import net.vite.wallet.network.rpc.BalanceInfo
import net.vite.wallet.network.rpc.VcpTokenInfo
import net.vite.wallet.network.rpc.ViteChainTokenInfo
import net.vite.wallet.network.rpc.ViteTokenInfo

fun defaultBalanceList() = listOf(
    BalanceItemVo(null, BalanceInfo(ViteTokenInfo, "0", "0")),
    BalanceItemVo(null, BalanceInfo(VcpTokenInfo, "0", "0"))
)

//fun ViteAccountInfo.getBalanceVoList(): List<BalanceItemVo> {
//    val list = (balance?.getSortedBalanceList()?.map {
//        BalanceItemVo(null, it)
//    } ?: defaultBalanceList()).toMutableList()
//    onroadBalance?.tokenBalanceInfoMap?.forEach { onroadItem ->
//        list.find { it.balanceInfo?.viteChainTokenInfo?.tokenId == onroadItem.key }?.let { foundItem ->
//            foundItem.onroadInfo = onroadItem.value
//        } ?: run {
//            list.add(BalanceItemVo(onroadItem.value, null))
//        }
//    }
//    return list
//}

@Keep
data class BalanceItemVo(
    var onroadInfo: BalanceInfo?,
    var balanceInfo: BalanceInfo?
) {
    fun getTokenInfo(): ViteChainTokenInfo? {
        return onroadInfo?.viteChainTokenInfo ?: balanceInfo?.viteChainTokenInfo
    }
}


//data class NormalBalanceVo(
//    val tokenInfo: NormalTokenInfo,
//    val tokenAmount: BigDecimal
//)