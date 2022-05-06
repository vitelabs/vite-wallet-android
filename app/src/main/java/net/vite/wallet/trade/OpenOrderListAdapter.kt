package net.vite.wallet.trade

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.network.http.vitex.Order
import net.vite.wallet.utils.symbolRemoveIndex
import java.text.SimpleDateFormat

class OpenOrderListAdapter(
    private var mValues: MutableList<Order>,
    private val tradeFragment: TradeFragment
) : RecyclerView.Adapter<OpenOrderListAdapter.OpenOrderVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpenOrderVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.open_order, parent, false)
        return OpenOrderVH(view)
    }

    var dataFormat = SimpleDateFormat("yy.MM.dd HH:mm:ss")
    override fun onBindViewHolder(holder: OpenOrderVH, position: Int) {
        holder.buyOrSellTxt.text =
            if (mValues[position].side == 0) holder.buyOrSellTxt.context.getString(
                R.string.buy
            ) else holder.buyOrSellTxt.context.getString(R.string.sell)

        holder.buyOrSellTxt.setBackgroundColor(
            if (mValues[position].side == 0) Color.parseColor("#1A01D764") else Color.parseColor("#1AE5494D")
        )

        holder.buyOrSellTxt.setTextColor(
            if (mValues[position].side == 0) Color.parseColor("#FF01D764") else Color.parseColor("#FFE5494D")
        )
        holder.amount.text =
            holder.buyOrSellTxt.context.getString(R.string.quantity_is) + mValues[position].quantity.toString() + " " + "".symbolRemoveIndex(
                mValues[position].tradeTokenSymbol
            )
        holder.price.text =
            holder.buyOrSellTxt.context.getString(R.string.price_is) + mValues[position].price + " " + "".symbolRemoveIndex(
                mValues[position].quoteTokenSymbol
            )

        holder.deal.text = holder.buyOrSellTxt.context.getString(R.string.deal_is) +
                mValues[position].executedQuantity.toString() + " " + "".symbolRemoveIndex(mValues[position].tradeTokenSymbol)
        holder.avPrice.text =
            holder.buyOrSellTxt.context.getString(R.string.av_price_is) +
                    mValues[position].executedAvgPrice.toString() + " " + "".symbolRemoveIndex(
                mValues[position].quoteTokenSymbol
            )

        holder.quoteTokenName.text = "/" + mValues[position].quoteTokenSymbol
        holder.tradeTokenName.text = mValues[position].tradeTokenSymbol
        holder.orderTimeTxt.text = dataFormat.format((mValues[position].createTime ?: 0) * 1000L)

        holder.recall.setOnClickListener {
            tradeFragment.doRevoke(mValues[position].orderId)
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    fun newData(mValues: MutableList<Order>) {
        this.mValues = mValues
        notifyDataSetChanged()
    }

    inner class OpenOrderVH(view: View) : RecyclerView.ViewHolder(view) {
        val buyOrSellTxt = view.findViewById<TextView>(R.id.buyOrSellTxt)
        val tradeTokenName = view.findViewById<TextView>(R.id.tradeTokenName)
        val quoteTokenName = view.findViewById<TextView>(R.id.quoteTokenName)
        val orderTimeTxt = view.findViewById<TextView>(R.id.orderTimeTxt)
        val amount = view.findViewById<TextView>(R.id.amount)
        val price = view.findViewById<TextView>(R.id.price)
        val deal = view.findViewById<TextView>(R.id.deal)
        val avPrice = view.findViewById<TextView>(R.id.avPrice)
        val recall = view.findViewById<TextView>(R.id.recall)

    }
}
