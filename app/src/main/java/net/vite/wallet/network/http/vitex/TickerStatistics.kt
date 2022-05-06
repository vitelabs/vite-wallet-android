package net.vite.wallet.network.http.vitex

import androidx.annotation.Keep
import net.vite.wallet.ViteConfig
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.exchange.net.ws.DexPushMessage
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.utils.isChinese
import java.math.BigDecimal
import java.util.*

@Keep
data class TickerStatistics(
    val symbol: String,
    val tradeTokenSymbol: String,
    val quoteTokenSymbol: String,
    val tradeToken: String,
    val quoteToken: String,
    val openPrice: String,
    val prevClosePrice: String,
    val closePrice: String,
    val priceChange: String,
    val priceChangePercent: Double,
    val highPrice: String,
    val lowPrice: String,
    val quantity: String,
    val amount: String,
    val pricePrecision: Int,
    val quantityPrecision: Int
) {


    fun getH5ExchangeUrl(): String {
        return NetConfigHolder.netConfig.vitexH5UrlPrefix +
                "#/index?address=${AccountCenter.currentViteAddress()}" +
                "&currency=${ViteConfig.get().currentCurrency().toLowerCase(Locale.ENGLISH)}" +
                "&lang=${
                    if (ViteConfig.get().context.isChinese()) {
                        "zh"
                    } else {
                        "en"
                    }
                }&category=${symbolRmIndex(tradeTokenSymbol).toUpperCase(Locale.ENGLISH)}" +
                "&symbol=$symbol" +
                "&tradeTokenId=$tradeToken" +
                "&quoteTokenId=$quoteToken"
    }

    companion object {

        val ALL_TickerStatistics =
            TickerStatistics("All", "", "", "", "", "", "", "", "", 1.0, "", "", "", "", 1, 1)

        fun retrievePairsFromSymbolOrNull(symbol: String): Pair<String, String>? {
            try {
                val underlineIndex = symbol.indexOf("_")
                val tradeTokenSymbol = symbol.substring(0, underlineIndex)
                val quoteTokenSymbol = symbol.substring(underlineIndex + 1, symbol.length)
                if (tradeTokenSymbol.isEmpty() || quoteTokenSymbol.isEmpty()) {
                    return null
                }
                return tradeTokenSymbol to quoteTokenSymbol
            } catch (e: Exception) {
                return null
            }
        }

        fun symbolRmIndex(symbol: String): String {
            val i = symbol.indexOf("-")
            if (i == -1) {
                return symbol
            }
            return symbol.substring(0, i)
        }

        fun fromPb(from: DexPushMessage.TickerStatisticsProto) =
            TickerStatistics(
                symbol = from.symbol,
                tradeTokenSymbol = from.tradeTokenSymbol,
                quoteTokenSymbol = from.quoteTokenSymbol,
                tradeToken = from.tradeToken,
                quoteToken = from.quoteToken,
                openPrice = from.openPrice,
                prevClosePrice = from.prevClosePrice,
                closePrice = from.closePrice,
                priceChange = from.priceChange,
                priceChangePercent = from.priceChangePercent.toDoubleOrNull() ?: 0.0,
                highPrice = from.highPrice,
                lowPrice = from.lowPrice,
                quantity = from.quantity,
                amount = from.amount,
                pricePrecision = from.pricePrecision,
                quantityPrecision = from.quantityPrecision
            )

        val defaultOrderFunc = { left: TickerStatistics, right: TickerStatistics ->
            if (left.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO <= right.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) {
                1
            } else {
                -1
            }
        }
        val defaultFav = { left: TickerStatistics, right: TickerStatistics ->
            0
        }

        val priceAscendingFunc = { left: TickerStatistics, right: TickerStatistics ->
            if (left.closePrice.toBigDecimalOrNull() ?: BigDecimal.ZERO >= right.closePrice.toBigDecimalOrNull() ?: BigDecimal.ZERO) {
                1
            } else {
                -1
            }
        }

        val priceDescendingFunc = { left: TickerStatistics, right: TickerStatistics ->
            if (left.closePrice.toBigDecimalOrNull() ?: BigDecimal.ZERO <= right.closePrice.toBigDecimalOrNull() ?: BigDecimal.ZERO) {
                1
            } else {
                -1
            }
        }

        val priceChangePercentAscendingFunc = { left: TickerStatistics, right: TickerStatistics ->
            if (left.priceChangePercent >= right.priceChangePercent) {
                1
            } else {
                -1
            }
        }

        val priceChangePercentDescendingFunc = { left: TickerStatistics, right: TickerStatistics ->
            if (left.priceChangePercent <= right.priceChangePercent) {
                1
            } else {
                -1
            }
        }

        val nameAscendingFunc = { left: TickerStatistics, right: TickerStatistics ->
            if (left.symbol >= right.symbol) {
                1
            } else {
                -1
            }
        }

        val nameDescendingFunc = { left: TickerStatistics, right: TickerStatistics ->
            if (left.symbol <= right.symbol) {
                1
            } else {
                -1
            }
        }


    }
}