package net.vite.wallet.network

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

interface UnitCastTokenInfo {
    fun baseToSmallestUnit(value: BigDecimal): BigDecimal

    fun smallestToBaseUnit(value: BigDecimal): BigDecimal

    fun amountText(amount: BigInteger, scale: Int): String

    fun amountTextWithSymbol(amount: BigInteger, scale: Int): String
}

fun BigInteger.amountInBase(decimal: Int, scale: Int? = null): BigDecimal {
    val d = toBigDecimal().let {
        if (it == BigDecimal.ZERO) {
            it
        } else {
            val decimal0 = decimal
            it.divide(
                BigDecimal.TEN.pow(decimal0),
                kotlin.math.min(scale ?: decimal0, decimal0),
                RoundingMode.DOWN
            )
        }
    }
    return d
}


private fun String.stripTrailingZeros(): String {
    if (this.toBigDecimal().toBigInteger().toString() == this) {
        return this
    }
    return if (this == "0") {
        return "0"
    } else {
        this.trimEnd { it == '0' }
    }
}

fun BigDecimal?.toLocalReadableText(scale: Int = 2, stripTrailingZeros: Boolean = true): String {
    return this?.setScale(scale, RoundingMode.DOWN)?.toPlainString()?.let {
        if (stripTrailingZeros) {
            it.stripTrailingZeros().trimEnd { it == '.' }
        } else {
            it
        }
    } ?: "null"
}

fun BigDecimal?.toLocalReadableTextWithThouands(scale: Int = 2): String {
    val df = DecimalFormat.getNumberInstance(Locale.US) as DecimalFormat
    df.isParseBigDecimal = true
    return try {
        this?.setScale(scale, RoundingMode.DOWN)
        return df.format(this)
    } catch (e: Exception) {
        this?.toString() ?: "null"
    }
}