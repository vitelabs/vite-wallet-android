package net.vite.wallet.buycoin.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.network.http.coinpurchase.PurchaseRecordItem
import net.vite.wallet.network.toLocalReadableText
import java.text.SimpleDateFormat


class BuyCoinRecordVH(val view: View) : RecyclerView.ViewHolder(view) {
    val ethAmount = view.findViewById<TextView>(R.id.ethAmount)
    val viteAmount = view.findViewById<TextView>(R.id.viteAmount)
    val priceText = view.findViewById<TextView>(R.id.priceText)
    val timeText = view.findViewById<TextView>(R.id.timeText)


    companion object {
        val dataFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        fun create(parent: ViewGroup): BuyCoinRecordVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_buycoin_list, parent, false)
            return BuyCoinRecordVH(view)
        }
    }

    fun bind(record: PurchaseRecordItem?) {
        if (record == null) return
        timeText.text = dataFormat.format(record.ctime ?: 0L)
        priceText.text = view.context.getString(
            R.string.buy_coin_price_record,
            record.ratePrice?.toBigDecimal()?.toLocalReadableText(8) ?: ""
        )
        ethAmount.text = record.xAmount?.toBigDecimal()?.toLocalReadableText(4)
        viteAmount.text = record.viteAmount?.toBigDecimal()?.toLocalReadableText(4)
    }

}