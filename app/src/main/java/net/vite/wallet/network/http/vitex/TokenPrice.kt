package net.vite.wallet.network.http.vitex

import androidx.annotation.Keep
import java.math.BigDecimal

@Keep
data class TokenPrice(
    val name: String?,
    val symbol: String?,
    val tokenCode: String?,
    val usd: BigDecimal?,
    val btc: BigDecimal?,
    val rub: BigDecimal?,
    val vnd: BigDecimal?,
    val krw: BigDecimal?,
    val `try`: BigDecimal?,
    val cny: BigDecimal?,
    val eur: BigDecimal?,
    val gbp: BigDecimal?,
)