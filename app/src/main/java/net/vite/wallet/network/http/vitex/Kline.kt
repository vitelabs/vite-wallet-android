package net.vite.wallet.network.http.vitex

import androidx.annotation.Keep
import net.vite.wallet.exchange.net.ws.DexPushMessage

@Keep
data class Kline(
    val t: Long,
    val c: Double,
    val o: Double,
    val h: Double,
    val l: Double,
    val v: Double
) {

    companion object {
        fun fromPb(from: DexPushMessage.KlineProto) =
            Kline(
                t = from.t,
                c = from.c,
                o = from.o,
                h = from.h,
                l = from.l,
                v = from.v
            )
    }

    override fun hashCode(): Int {
        return t.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return hashCode() == other.hashCode()
    }
}