package net.vite.wallet.exchange


import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.network.http.vitex.Trade
import java.text.SimpleDateFormat

class RecentTransactionsViewAdapter(
    val mValues: List<Trade>
) : RecyclerView.Adapter<RecentTransactionsViewAdapter.ViewHolder>() {

    internal var dataFormat = SimpleDateFormat("HH:mm:ss")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_transactions, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        if (item.side == 0) {// buy
            holder.price.setTextColor(Color.parseColor("#00D764"))
        } else {
            holder.price.setTextColor(Color.parseColor("#E5494D"))
        }
        holder.time.text = dataFormat.format(item.time * 1000)
        holder.price.text = item.price
        holder.amount.text = item.quantity
    }

    override fun getItemCount(): Int = Math.min(mValues.size, 30)

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val time: TextView = mView.findViewById(R.id.time)
        val price: TextView = mView.findViewById(R.id.price)
        val amount: TextView = mView.findViewById(R.id.amount)
    }
}
