package net.vite.wallet.exchange.history

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.network.http.vitex.Order
import net.vite.wallet.utils.symbolRemoveIndex
import net.vite.wallet.utils.viewbinding.findMyView
import java.text.SimpleDateFormat

internal class VH(view: View) : RecyclerView.ViewHolder(view) {
    val type by findMyView<TextView>(R.id.trade_type)
    val tradeTokenLeft by findMyView<TextView>(R.id.trade_token_left)
    val tradeTokenRightAndTime by findMyView<TextView>(R.id.trade_token_right_and_time)
    val tradeStatus by findMyView<TextView>(R.id.trade_status)
    val recall by findMyView<TextView>(R.id.recall)
    val amount by findMyView<TextView>(R.id.amount)
    val price by findMyView<TextView>(R.id.price)
    val filled by findMyView<TextView>(R.id.filled)
    val avg by findMyView<TextView>(R.id.avg)

    companion object {
        val dataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        fun create(parent: ViewGroup): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exchange, parent, false)
            return VH(view)
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(order: Order) {
        val context = itemView.context
        if (order.side == 0) {
            type.setText(R.string.buy)
            type.setTextColor(Color.parseColor("#FF01D764"))
            type.setBackgroundResource(R.drawable.item_exchange_buy_bg)
        } else {
            type.setText(R.string.sell)
            type.setTextColor(Color.parseColor("#FFE5494D"))
            type.setBackgroundResource(R.drawable.item_exchange_sell_bg)
        }

        tradeTokenLeft.text = order.tradeTokenSymbol ?: ""
        tradeTokenRightAndTime.text =
            " /${order.quoteTokenSymbol}  ${dataFormat.format((order.createTime ?: 0) * 1000L)} "

        amount.text =
            context.getString(R.string.quantity_is) + order.quantity.toString() + " " + "".symbolRemoveIndex(
                order.tradeTokenSymbol
            )

        price.text =
            context.getString(R.string.price_is) + order.price + " " + "".symbolRemoveIndex(
                order.quoteTokenSymbol
            )

        filled.text = context.getString(R.string.deal_is) +
                order.executedQuantity.toString() + " " + "".symbolRemoveIndex(order.tradeTokenSymbol)
        avg.text =
            context.getString(R.string.av_price_is) +
                    order.executedAvgPrice.toString() + " " + "".symbolRemoveIndex(
                order.quoteTokenSymbol
            )

        if (order.status == 3 || order.status == 5) {
            tradeStatus.visibility = View.GONE
            recall.visibility = View.VISIBLE
            tradeStatus.setText(R.string.order_filter_status_ongoing)
        } else {
            tradeStatus.visibility = View.VISIBLE
            recall.visibility = View.GONE
            tradeStatus.setText(
                when (order.status) {
                    4 -> R.string.order_filter_status_completed
                    7, 8 -> R.string.order_filter_status_cancelled
                    9 -> R.string.order_filter_status_failed
                    else -> R.string.order_filter_status_empty
                }
            )
        }

        recall.setOnClickListener {
            val tokenAddr =
                TokenInfoCenter.findTokenInfo { it.uniqueName() == order.tradeTokenSymbol }?.tokenAddress
                    ?: return@setOnClickListener
            (itemView.context as? TradeHistoryDetailsTabActivity)?.doRevoke(
                orderId = order.orderId,
                tradeTokenAddress = tokenAddr
            )
        }
    }
}