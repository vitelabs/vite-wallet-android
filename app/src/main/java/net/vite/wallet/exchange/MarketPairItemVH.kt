package net.vite.wallet.exchange

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.kline.market.activity.KLineActivity
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.network.toLocalReadableText
import java.math.BigDecimal
import java.math.RoundingMode


class MarketPairItemVH(val rootView: View) : RecyclerView.ViewHolder(rootView) {

    val pairFirst = rootView.findViewById<TextView>(R.id.pairFirst)
    val pairSecond = rootView.findViewById<TextView>(R.id.pairSecond)
    val mineLogo = rootView.findViewById<ImageView>(R.id.mineLogo)
    val miningRate = rootView.findViewById<TextView>(R.id.miningRate)
    val zeroFeeIcon = rootView.findViewById<ImageView>(R.id.zeroFeeIcon)
    val timeText = rootView.findViewById<TextView>(R.id.timeText)
    val amountText = rootView.findViewById<TextView>(R.id.amountText)
    val priceText = rootView.findViewById<TextView>(R.id.priceText)
    val priceValueText = rootView.findViewById<TextView>(R.id.priceValueText)
    val priceChangePercent = rootView.findViewById<TextView>(R.id.priceChangePercent)


    companion object {
        fun create(parent: ViewGroup): MarketPairItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_market_pair, parent, false)
            return MarketPairItemVH(view)
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(symbol: String) {
        val tpPair = TickerStatistics.retrievePairsFromSymbolOrNull(symbol) ?: return
        val tradeTokenSymbol = tpPair.first
        val quoteTokenSymbol = tpPair.second

        val tickerStatistics = TickerStatisticsCenter.symbolMap[symbol]

        val miningRateData = TickerStatisticsCenter.getMiningRate(symbol)
        if (TickerStatisticsCenter.isOrderAndTradeMiningPair(symbol)) {
            mineLogo.visibility = View.VISIBLE
            mineLogo.setImageResource(if (miningRateData == null) R.mipmap.mining_all_icon_big else R.mipmap.mining_all_icon_small)
        } else if (TickerStatisticsCenter.isOrderMiningPair(symbol)) {
            mineLogo.visibility = View.VISIBLE
            mineLogo.setImageResource(if (miningRateData == null) R.mipmap.mining_order_icon_big else R.mipmap.mining_order_icon_small)
        } else if (TickerStatisticsCenter.isTradeMiningPair(symbol)) {
            mineLogo.visibility = View.VISIBLE
            mineLogo.setImageResource(R.mipmap.mining_trade_icon_big)
        } else {
            mineLogo.visibility = View.GONE
        }

        miningRateData.let {
            if (it == null) {
                miningRate.visibility = View.GONE
            } else {
                miningRate.visibility = View.VISIBLE
                miningRate.text = "$it"
            }
        }

        zeroFeeIcon.visibility =
            if (TickerStatisticsCenter.isZeroFee(symbol)) View.VISIBLE else View.GONE


        pairFirst.text = TickerStatistics.symbolRmIndex(tradeTokenSymbol)
        val pairSecondTxt = TickerStatistics.symbolRmIndex(quoteTokenSymbol)
        pairSecond.text = "/$pairSecondTxt"
        timeText.text = "24H"

        val amountPrecision = when (pairSecondTxt) {
            "BTC" -> 3
            "ETH" -> 2
            else -> 1
        }
        amountText.text = tickerStatistics?.amount?.toBigDecimalOrNull()?.setScale(
            amountPrecision,
            RoundingMode.HALF_UP
        )?.toLocalReadableText(amountPrecision, true) ?: "- -"


        priceText.text = tickerStatistics?.closePrice ?: "- -"

        val closePriceDecimal =
            tickerStatistics?.closePrice?.toBigDecimalOrNull()

        val rate =
            TickerStatisticsCenter.tokenRateCacheMap[quoteTokenSymbol]?.getRate() ?: BigDecimal.ZERO

        if (rate != null && closePriceDecimal != null) {
            var closePriceDecimalCurrencyFormat =
                closePriceDecimal.multiply(rate).toLocalReadableText(6, true)
            if (closePriceDecimalCurrencyFormat == "0") {
                closePriceDecimalCurrencyFormat = "0.00"
            }
            priceValueText.text = ViteConfig.get().currentCurrencySymbol() + closePriceDecimalCurrencyFormat
        } else {
            priceValueText.text = "- -"
        }


        val fixedPercent =
            tickerStatistics?.priceChangePercent?.toBigDecimal()?.multiply(100.toBigDecimal())
        priceChangePercent.text = if (fixedPercent != null) {
            "${if (fixedPercent >= BigDecimal.ZERO) "+" else ""}" +
                    fixedPercent.toLocalReadableText(
                        2,
                        false
                    ) + "%"

        } else {
            "- -"
        }


        if (tickerStatistics?.priceChangePercent ?: 0.0 < 0) {
//            priceChangePercent.setTextColor(Color.parseColor("#ffff0008"))
            priceChangePercent.background =
                priceChangePercent.context.getDrawable(R.drawable.market_down_bg)
        } else {
//            priceChangePercent.setTextColor(Color.parseColor("#ff5bc500"))
            priceChangePercent.background =
                priceChangePercent.context.getDrawable(R.drawable.market_up_bg)
        }

        rootView.setOnClickListener {
            tickerStatistics?.let {
                //                H5WebActivity.show(rootView.context, it.getH5ExchangeUrl())
                KLineActivity.show(rootView.context, tradeTokenSymbol, quoteTokenSymbol)
            }
        }

    }


}