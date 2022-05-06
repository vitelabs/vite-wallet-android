package net.vite.wallet.dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_trade_pair_picker.*
import net.vite.wallet.R
import net.vite.wallet.exchange.TickerStatisticsCenter
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.toLocalReadableText
import java.math.BigDecimal

class ChooseTradePairDialog(
    context: Context,
    val items: List<TickerStatistics>,
    val onChooseListener: (t: TickerStatistics, dialog: AlertDialog) -> Unit
) : AlertDialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setContentView(R.layout.dialog_trade_pair_picker)
        pairList.adapter = ChooseTradePairAdapter(items) {
            onChooseListener.invoke(it, this)
        }
        closeBtn.setOnClickListener {
            dismiss()
        }
    }
}

private class ChooseTradePairAdapter(
    val items: List<TickerStatistics>,
    val onClick: (t: TickerStatistics) -> Unit
) :
    RecyclerView.Adapter<ChooseTradePairItemVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ChooseTradePairItemVH.create(parent, onClick)

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ChooseTradePairItemVH, position: Int) {
        holder.bind(items[position])
    }

}

private class ChooseTradePairItemVH(val view: View, val onClick: (t: TickerStatistics) -> Unit) :
    RecyclerView.ViewHolder(view) {
    val leftToken = view.findViewById<TextView>(R.id.leftToken)
    val rightToken = view.findViewById<TextView>(R.id.rightToken)
    val price = view.findViewById<TextView>(R.id.price)
    val priceValueText = view.findViewById<TextView>(R.id.priceValueText)
    val priceChangePercent = view.findViewById<TextView>(R.id.priceChangePercent)

    companion object {
        fun create(
            parent: ViewGroup,
            onClick: (t: TickerStatistics) -> Unit
        ): ChooseTradePairItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.dialog_trade_pair_item, parent, false)
            return ChooseTradePairItemVH(view, onClick)
        }
    }

    fun bind(t: TickerStatistics) {
        view.setOnClickListener {
            onClick(t)
        }
        val tpPair = TickerStatistics.retrievePairsFromSymbolOrNull(t.symbol) ?: return
        val tradeTokenSymbol = tpPair.first
        val quoteTokenSymbol = tpPair.second
        leftToken.text = TickerStatistics.symbolRmIndex(tradeTokenSymbol)
        rightToken.text = "/" + TickerStatistics.symbolRmIndex(quoteTokenSymbol)
        price.text = t.closePrice

        val fixedPercent =
            t.priceChangePercent.toBigDecimal().multiply(100.toBigDecimal())
        priceChangePercent.text = if (fixedPercent != null) {
            fixedPercent.toLocalReadableText(
                2,
                false
            ) + "%"

        } else {
            "- -"
        }
        if (t.priceChangePercent < 0) {
            priceChangePercent.setTextColor(Color.parseColor("#FFE5494D"))
        } else {
            priceChangePercent.setTextColor(Color.parseColor("#FF01D764"))
        }

        val rate = TickerStatisticsCenter.tokenRateCacheMap[quoteTokenSymbol]?.getRate() ?: BigDecimal.ZERO

        priceValueText.text =
            t.closePrice.toBigDecimalOrNull()?.multiply(rate)?.toCurrencyText(6) ?: ""


    }

}