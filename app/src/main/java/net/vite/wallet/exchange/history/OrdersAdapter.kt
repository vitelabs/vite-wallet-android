package net.vite.wallet.exchange.history

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.network.http.vitex.Order

internal class OrdersAdapter : RecyclerView.Adapter<VH>() {
    val orders = ArrayList<Order>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH.create(parent)

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(orders[position])

    override fun getItemCount() = orders.size
}