package net.vite.wallet.exchange.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.exchange.AccountFavExchangePairManager
import net.vite.wallet.exchange.TickerStatisticsCenter
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.network.http.vitex.TickerStatistics.Companion.ALL_TickerStatistics

class MarketPairSearchListAdapter(
    val accountFavExchangePairManager: AccountFavExchangePairManager,
    val onTickerClickListener: (tickerStatistics: TickerStatistics) -> Unit
) :
    RecyclerView.Adapter<MarketPairSearchVH>() {
    private val data = ArrayList<String>()
    fun setList(list: List<String>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketPairSearchVH {
        return MarketPairSearchVH.create(
            parent,
            accountFavExchangePairManager,
            onTickerClickListener
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: MarketPairSearchVH, position: Int) {
        holder.bind(data[position])
    }

}

class MarketPairSearchVH(
    val rootView: View,
    val accountFavExchangePairManager: AccountFavExchangePairManager,
    val onTickerClickListener: (tickerStatistics: TickerStatistics) -> Unit

) : RecyclerView.ViewHolder(rootView) {
    val pairFirst = rootView.findViewById<TextView>(R.id.pairFirst)
    val pairSecond = rootView.findViewById<TextView>(R.id.pairSecond)
    val isFav = rootView.findViewById<ImageView>(R.id.isFav)

    companion object {
        fun create(
            parent: ViewGroup,
            accountFavExchangePairManager: AccountFavExchangePairManager,
            onTickerClickListener: (tickerStatistics: TickerStatistics) -> Unit
        ): MarketPairSearchVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_market_pair_search, parent, false)
            return MarketPairSearchVH(view, accountFavExchangePairManager, onTickerClickListener)
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(symbol: String) {
        if(symbol == "All") {
            pairFirst.text = symbol
            pairSecond.visibility = View.GONE
            isFav.visibility = View.GONE
            rootView.setOnClickListener {
                onTickerClickListener.invoke(ALL_TickerStatistics)
            }
            return
        }
        val tpPair = TickerStatistics.retrievePairsFromSymbolOrNull(symbol) ?: return
        val tradeTokenSymbol = tpPair.first
        val quoteTokenSymbol = tpPair.second
        pairFirst.text = TickerStatistics.symbolRmIndex(tradeTokenSymbol)
        pairSecond.text = "/" + TickerStatistics.symbolRmIndex(quoteTokenSymbol)
        if (accountFavExchangePairManager.favPairs.find { it == symbol } != null) {
            isFav.setImageResource(R.mipmap.add_fav_light)
            isFav.setOnClickListener {
                accountFavExchangePairManager.deleteFav(symbol)
                isFav.setImageResource(R.mipmap.add_fav_default)
            }
        } else {
            isFav.setImageResource(R.mipmap.add_fav_default)
            isFav.setOnClickListener {
                accountFavExchangePairManager.addFav(symbol)
                isFav.setImageResource(R.mipmap.add_fav_light)
            }
        }

        rootView.setOnClickListener {
            val tick = TickerStatisticsCenter.symbolMap[symbol] ?: return@setOnClickListener
            accountFavExchangePairManager.addSearchHistory(symbol)
            onTickerClickListener.invoke(tick)
        }

    }

}