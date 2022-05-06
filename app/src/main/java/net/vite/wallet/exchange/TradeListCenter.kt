package net.vite.wallet.exchange

import android.annotation.SuppressLint
import net.vite.wallet.network.http.vitex.TradeList
import java.util.concurrent.ConcurrentHashMap

object TradeListCenter {

    val symbolMap = ConcurrentHashMap<String, TradeList>()

    @SuppressLint("CheckResult")
    private fun updateMaps(tradeList: TradeList) {
        symbolMap[tradeList.trade?.get(0)?.symbol ?: ""]?.trade?.addAll(
            tradeList?.trade ?: emptyList()
        )
        symbolMap[tradeList.trade?.get(0)?.symbol ?: ""]?.trade?.sortByDescending {
            it.time
        }
    }

    fun onNewMessageArrived(tradeList: TradeList) {
        updateMaps(tradeList)
    }
}