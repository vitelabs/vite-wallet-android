package net.vite.wallet.network.http.vitex

import androidx.annotation.Keep
import net.vite.wallet.exchange.net.ws.DexPushMessage

@Keep
data class DepthList(
    var asks: MutableList<Depth>?,
    var bids: MutableList<Depth>?
) {
    companion object {
        fun fromPb(from: DexPushMessage.DepthListProto): DepthList {
            val asks: MutableList<Depth> = mutableListOf()
            val bids: MutableList<Depth> = mutableListOf()
            from.asksList.forEach {
                val fromPb = Depth.fromPb(it)
                asks.add(
                    Depth(
                        price = fromPb.price,
                        quantity = fromPb.quantity,
                        amount = fromPb.amount
                    )
                )
            }
            from.bidsList.forEach {
                val fromPb = Depth.fromPb(it)
                bids.add(
                    Depth(
                        price = fromPb.price,
                        quantity = fromPb.quantity,
                        amount = fromPb.amount
                    )
                )
            }
            return DepthList(asks, bids)
        }
    }
}