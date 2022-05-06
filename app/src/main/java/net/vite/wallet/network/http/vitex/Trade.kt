package net.vite.wallet.network.http.vitex

import androidx.annotation.Keep
import net.vite.wallet.exchange.net.ws.DexPushMessage

@Keep
data class Trade(
    val tradeId: String,
    val symbol: String,
    val tradeTokenSymbol: String,
    val quoteTokenSymbol: String,
    val tradeToken: String,
    val quoteToken: String,
    val price: String,
    val quantity: String,
    val amount: String,
    val time: Long,
    val side: Int,
    val buyerOrderId: String,
    val sellerOrderId: String,
    val buyFee: String,
    val sellFee: String,
    val blockHeight: Long
) {


    companion object {
        fun fromPb(from: DexPushMessage.TradeProto) =
            Trade(
                tradeId = from.tradeId,
                symbol = from.symbol,
                tradeTokenSymbol = from.tradeTokenSymbol,
                quoteTokenSymbol = from.quoteTokenSymbol,
                tradeToken = from.tradeToken,
                quoteToken = from.quoteToken,
                price = from.price,
                quantity = from.quantity,
                amount = from.amount,
                time = from.time,
                side = from.side,
                buyerOrderId = from.buyerOrderId,
                sellerOrderId = from.sellerOrderId,
                buyFee = from.buyFee,
                sellFee = from.sellFee,
                blockHeight = from.blockHeight
            )
    }
}