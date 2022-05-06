package net.vite.wallet.exchange.wiget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.MainActivity
import net.vite.wallet.R
import net.vite.wallet.exchange.TickerStatisticsCenter
import net.vite.wallet.kline.market.activity.KLineActivity
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.vitebridge.H5WebActivity

class SwitchMarketAdapter : RecyclerView.Adapter<SwitchMarketPairItemVH>() {
    val symbols = ArrayList<String>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwitchMarketPairItemVH {
        return SwitchMarketPairItemVH.create(parent)
    }

    override fun getItemCount(): Int {
        return symbols.size
    }

    override fun onBindViewHolder(holder: SwitchMarketPairItemVH, position: Int) {
        holder.bind(symbols[position])
    }
}

class SwitchMarketPairItemVH(val rootView: View) : RecyclerView.ViewHolder(rootView) {

    val pairFirst = rootView.findViewById<TextView>(R.id.pairFirst)
    val pairSecond = rootView.findViewById<TextView>(R.id.pairSecond)
    val priceText = rootView.findViewById<TextView>(R.id.priceText)
    val priceChangePercent = rootView.findViewById<TextView>(R.id.priceChangePercent)
    val operatorName = rootView.findViewById<TextView>(R.id.operatorName)
    val miningImg = rootView.findViewById<ImageView>(R.id.miningImg)
    val miningRate = rootView.findViewById<TextView>(R.id.miningRate)
    val zeroFeeIcon = rootView.findViewById<ImageView>(R.id.zeroFeeIcon)

    companion object {
        fun create(parent: ViewGroup): SwitchMarketPairItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_switch_market_pair, parent, false)
            return SwitchMarketPairItemVH(view)
        }
    }

    fun bind(symbol: String) {
        val tpPair = TickerStatistics.retrievePairsFromSymbolOrNull(symbol) ?: return
        val tradeTokenSymbol = tpPair.first
        val quoteTokenSymbol = tpPair.second

        val tickerStatistics = TickerStatisticsCenter.symbolMap[symbol]
        rootView.setOnClickListener {
            tickerStatistics?.let {
                if (rootView.context is KLineActivity) {
                    (rootView.context as KLineActivity).onExchangeMarketItemSwitched(symbol)
                } else if (rootView.context is MainActivity) {
                    (rootView.context as MainActivity).onExchangeMarketItemSwitched(symbol)
                } else if (rootView.context is H5WebActivity) {
                    (rootView.context as H5WebActivity).onExchangeMarketItemSwitched(symbol)
                }
            }
        }

        val miningRateData = TickerStatisticsCenter.getMiningRate(symbol)
        if (TickerStatisticsCenter.isOrderAndTradeMiningPair(symbol)) {
            miningImg.visibility = View.VISIBLE
            miningImg.setImageResource(if (miningRateData == null) R.mipmap.mining_all_icon_big else R.mipmap.mining_all_icon_small)
        } else if (TickerStatisticsCenter.isOrderMiningPair(symbol)) {
            miningImg.visibility = View.VISIBLE
            miningImg.setImageResource(if (miningRateData == null) R.mipmap.mining_order_icon_big else R.mipmap.mining_order_icon_small)
        } else if (TickerStatisticsCenter.isTradeMiningPair(symbol)) {
            miningImg.visibility = View.VISIBLE
            miningImg.setImageResource(R.mipmap.mining_trade_icon_big)
        } else {
            miningImg.visibility = View.GONE
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
        pairSecond.text = "/" + TickerStatistics.symbolRmIndex(quoteTokenSymbol)
        priceText.text = tickerStatistics?.closePrice ?: "- -"

        val fixedPercent =
            tickerStatistics?.priceChangePercent?.toBigDecimal()?.multiply(100.toBigDecimal())
        priceChangePercent.text = if (fixedPercent != null) {
            fixedPercent.toLocalReadableText(
                2,
                false
            ) + "%"

        } else {
            "- -"
        }
        operatorName.text = TickerStatisticsCenter.operatorInfoCache[symbol]?.name ?: "- -"

        if (tickerStatistics?.priceChangePercent ?: 0.0 < 0) {
            priceChangePercent.setTextColor(Color.parseColor("#ffff0008"))
        } else {
            priceChangePercent.setTextColor(Color.parseColor("#ff5bc500"))
        }

    }

}