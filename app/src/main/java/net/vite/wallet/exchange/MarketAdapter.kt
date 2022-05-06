package net.vite.wallet.exchange

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class MarketAdapter : RecyclerView.Adapter<MarketPairItemVH>() {
    val symbols = ArrayList<String>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketPairItemVH {
        return MarketPairItemVH.create(parent)
    }

    override fun getItemCount(): Int {
        return symbols.size
    }

    override fun onBindViewHolder(holder: MarketPairItemVH, position: Int) {
        holder.bind(symbols[position])
    }
}
