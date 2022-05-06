package net.vite.wallet.exchange


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.utils.CommonUtil.dip2px
import kotlinx.android.synthetic.main.fragment_order.view.*
import net.vite.wallet.R
import net.vite.wallet.network.http.vitex.OrderItem


class OrderRecyclerViewAdapter(
    private var mValues: List<OrderItem>
) : RecyclerView.Adapter<OrderRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_order, parent, false)
        return ViewHolder(view)
    }

    fun updateDatas(mValues: List<OrderItem>) {
        this.mValues = mValues
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.buyAmount.text = item.buyAmount
        holder.sellAmount.text = item.sellAmount
        holder.buyPrice.text = item.buyPrice
        holder.sellPrice.text = item.sellPrice

        holder.buyMiningBG.visibility = if (item.buyCanMining) View.VISIBLE else View.GONE
        holder.sellMiningBG.visibility = if (item.sellCanMining) View.VISIBLE else View.GONE

        val screenW = holder.buyPercentBG.context.resources.displayMetrics.widthPixels
        holder.buyPercentBG.pivotX = (screenW - dip2px(holder.buyPercentBG.context, 37f)) / 2.0f
        holder.buyPercentBG.scaleX = if (item.buyPercent.isNaN()) 0.0f else item.buyPercent

        holder.sellPercentBG.pivotX = 0.0f
        holder.sellPercentBG.scaleX = if (item.sellPercent.isNaN()) 0.0f else item.sellPercent

        holder.buyDashLine.visibility = if (item.buyHasLine) View.VISIBLE else View.GONE
        holder.sellDashLine.visibility = if (item.sellHasLine) View.VISIBLE else View.GONE

        holder.sellMyAvatar.visibility = if (item.hasSellAvatar) View.VISIBLE else View.GONE
        holder.buyMyAvatar.visibility = if (item.hasBuyAvatar) View.VISIBLE else View.GONE

    }

    override fun getItemCount(): Int = Math.min(mValues.size, 20)

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val buyAmount: TextView = mView.buyAmount
        val sellAmount: TextView = mView.sellAmount
        val buyPrice: TextView = mView.buyPrice
        val sellPrice: TextView = mView.sellPrice
        val sellMyAvatar: ImageView = mView.sellMyAvatar
        val buyMyAvatar: ImageView = mView.buyMyAvatar

        val buyMiningBG: View = mView.buyMiningBG
        val sellMiningBG: View = mView.sellMiningBG
        val buyPercentBG: View = mView.buyPercentBG
        val sellPercentBG: View = mView.sellPercentBG

        val buyDashLine: ImageView = mView.buyDashLine
        val sellDashLine: ImageView = mView.sellDashLine

    }
}
