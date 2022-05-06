package net.vite.wallet.exchange

import android.annotation.SuppressLint
import net.vite.wallet.network.http.vitex.Kline
import java.util.concurrent.ConcurrentHashMap

object KlineCenter {

    val symbolMap = ConcurrentHashMap<String, MutableList<Kline>>()

    @SuppressLint("CheckResult")
    private fun updateMaps(kline: Kline, symbol: String) {
        if (symbolMap[symbol] == null)
            symbolMap[symbol] = mutableListOf()

        if (symbolMap[symbol]?.contains(kline) == true) {
            symbolMap[symbol]?.remove(kline)
            symbolMap[symbol]?.add(kline)
        } else {
            symbolMap[symbol]?.add(kline)
        }

        symbolMap[symbol]?.sortBy {
            it.t
        }

        println("xirtam updateMaps kline $symbol $kline")
    }

    fun onNewMessageArrived(kline: Kline, symbol: String) {
        println("xirtam onNewMessageArrived kline $symbol")
        updateMaps(kline, symbol)
    }
}