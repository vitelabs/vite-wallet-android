package net.vite.wallet.exchange

import android.annotation.SuppressLint
import net.vite.wallet.network.http.vitex.ClosedMarket
import net.vite.wallet.network.http.vitex.HiddenMarkets

object SpecialMarketCenter {

    private var closedMarkets = emptySet<String>()
    private var hiddenMarkets = emptySet<String>()

    @SuppressLint("CheckResult")
    fun updateClosed(markets: List<ClosedMarket>) {
        closedMarkets = markets.filter { it.stopped==1 }.map { it.symbol?:"" }.toSet()
    }

    fun isStopped(symbol: String) =
        closedMarkets.contains(symbol)


    fun updateHidden(markets: HiddenMarkets){
        hiddenMarkets = markets.hideSymbols?.toSet()?: emptySet()
    }

    fun isHidden(symbol:String) =
        hiddenMarkets.contains(symbol)
}