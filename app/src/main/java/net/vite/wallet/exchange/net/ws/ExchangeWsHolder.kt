package net.vite.wallet.exchange.net.ws

import net.vite.wallet.exchange.*
import net.vite.wallet.logt
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.http.vitex.*
import java.util.*

object ExchangeWsHolder {
    fun hasEstablishedWs() = exchangeWs?.connected == true
    @Volatile
    private var exchangeWs: ExchangeWs? = null

    fun connect() {
        if (exchangeWs?.connected == true || exchangeWs?.isConnecting == true) {
            return
        }
        exchangeWs =
            ExchangeWs(
                NetConfigHolder.netConfig.vitexWsUrl,
                ::exchangeWsStatus,
                ::onNewMessageArrived,
                ::onNewTradeDataArrived,
                ::onNewDepthDataArrived,
                ::onNewKlineDataArrived,
                ::onNewOrderDataArrived
            )
        exchangeWs?.connect()
    }

    fun reconnect() {
        exchangeWs?.mustConnect()
    }

    private fun retry() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                logt("ExchangeWsHolder retry")
                connect()
            }
        }, 5 * 1000)
    }


    private fun exchangeWsStatus(status: ExchangeWs.Status) {
        when (status) {
            ExchangeWs.Status.CONNECTED -> {

            }
            ExchangeWs.Status.DISCONNECTED -> {

            }
            ExchangeWs.Status.INTERRUPT -> {
                retry()
            }
        }
    }

    fun onNewMessageArrived(tickerStatistics: TickerStatistics) {
        TickerStatisticsCenter.onNewMessageArrived(tickerStatistics)
    }

    fun onNewTradeDataArrived(tradeList: TradeList) {
        TradeListCenter.onNewMessageArrived(tradeList)
    }

    fun onNewDepthDataArrived(depthList: DepthList, symbol: String) {
        DepthListCenter.onNewMessageArrived(depthList, symbol)
    }

    fun onNewKlineDataArrived(kline: Kline, symbol: String) {
        KlineCenter.onNewMessageArrived(kline, symbol)
    }

    fun onNewOrderDataArrived(order: Order) {
        OrderCenter.onNewMessageArrived(order)
    }

    fun subTrade(symbol: String) {
        exchangeWs?.sub("market.$symbol.trade")
    }

    fun unsubTrade(symbol: String) {
        exchangeWs?.unsub("market.$symbol.trade")
    }

    fun subDepth(symbol: String) {
        exchangeWs?.sub("market.$symbol.depth")
    }

    fun unsubDepth(symbol: String) {
        exchangeWs?.unsub("market.$symbol.depth")
    }

    fun subKline(symbol: String, timeInterval: String) {
        exchangeWs?.sub("market.$symbol.kline.$timeInterval")
    }

    fun unsubKline(symbol: String, timeInterval: String) {
        exchangeWs?.unsub("market.$symbol.kline.$timeInterval")
    }

    fun subOrder(address: String) {
        exchangeWs?.sub("order.$address")
    }

    fun unsubOrder(address: String) {
        exchangeWs?.unsub("order.$address")
    }
}