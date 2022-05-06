package net.vite.wallet.exchange

import android.annotation.SuppressLint
import net.vite.wallet.network.http.vitex.DepthList
import java.util.concurrent.ConcurrentHashMap

object DepthListCenter {

    val symbolMap = ConcurrentHashMap<String, DepthList>()

    @SuppressLint("CheckResult")
    private fun updateMaps(depthList: DepthList, symbol: String) {
        symbolMap[symbol] = depthList
//        symbolMap[symbol]?.asks?.sortByDescending {
//            it.price.toDouble()
//        }
    }

    fun onNewMessageArrived(depthList: DepthList, symbol: String) {
        updateMaps(depthList, symbol)
    }

}