package net.vite.wallet.network.http.vitex

import androidx.annotation.Keep
import net.vite.wallet.exchange.net.ws.DexPushMessage
import java.math.BigDecimal

@Keep
data class Depth(
    val price: String,
    val quantity: String,
    val amount: String
) {

    companion object {
        private val k1: BigDecimal = BigDecimal.valueOf(100000)
        private val m1: BigDecimal = BigDecimal.valueOf(100000000)

        fun fromPb(from: DexPushMessage.DepthProto) =
            Depth(
                price = from.price,
                quantity = from.quantity,
                amount = from.amount
            )
    }

    fun quantityView(): String {
        if (quantity == "") {
            return quantity
        }
        var num = quantity.toBigDecimal()
        return if (num < k1) {
            quantity
        } else if (num >= k1 && num < m1) {
            num.movePointLeft(3).setScale(1, BigDecimal.ROUND_DOWN).toString() + "K"
        } else {
            num.movePointLeft(6).setScale(1, BigDecimal.ROUND_DOWN).toString() + "M"
        }
    }
}