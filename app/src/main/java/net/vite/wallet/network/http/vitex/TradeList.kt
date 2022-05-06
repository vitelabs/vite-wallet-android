package net.vite.wallet.network.http.vitex

import androidx.annotation.Keep
import net.vite.wallet.exchange.net.ws.DexPushMessage

@Keep
data class TradeList(
    var trade: MutableList<Trade>?
) {
    companion object {
        fun fromPb(from: DexPushMessage.TradeListProto): TradeList {
            val tradeList: MutableList<Trade> = mutableListOf()
            from.tradeList.forEach {
                val fromPb = Trade.fromPb(it)
                tradeList.add(
                    Trade(
                        tradeId = fromPb.tradeId,
                        symbol = fromPb.symbol,
                        tradeTokenSymbol = fromPb.tradeTokenSymbol,
                        quoteTokenSymbol = fromPb.quoteTokenSymbol,
                        tradeToken = fromPb.tradeToken,
                        quoteToken = fromPb.quoteToken,
                        price = fromPb.price,
                        quantity = fromPb.quantity,
                        amount = fromPb.amount,
                        time = fromPb.time,
                        side = fromPb.side,
                        buyerOrderId = fromPb.buyerOrderId,
                        sellerOrderId = fromPb.sellerOrderId,
                        buyFee = fromPb.buyFee,
                        sellFee = fromPb.sellFee,
                        blockHeight = fromPb.blockHeight
                    )
                )
            }
            return TradeList(tradeList)
        }
    }
}