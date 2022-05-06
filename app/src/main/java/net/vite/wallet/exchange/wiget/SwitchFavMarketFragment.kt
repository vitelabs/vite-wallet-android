package net.vite.wallet.exchange.wiget

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_market.*
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.exchange.TickerStatisticsCenter
import net.vite.wallet.network.http.vitex.TickerStatistics
import java.util.*
import kotlin.collections.ArrayList

class SwitchFavMarketFragment : SwitchMarketFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainList.adapter = adapter

        marketVm.httpMarketLd.observe(this, Observer {
            if (it.isLoading()) {
                return@Observer
            }
            updateList()
        })

        marketVm.symbolsMarketLd.observe(this, Observer {
            swipeRefresh.isRefreshing = it.isLoading()
            if (it.isLoading()) {
                return@Observer
            }
            updateList()
        })

        marketVm.tokenRateLd.observe(this, Observer {
            adapter.notifyDataSetChanged()
        })

        swipeRefresh.setOnRefreshListener {
            refresh(true)
        }

        currentOrderMode = TickerStatistics.defaultFav
        updateList()
    }

    override fun updateList() {
        val symbols = AccountCenter.getCurrentAccountFavExchangePairManager()?.getFav()
            ?: emptyList<String>()
        if (symbols.isEmpty()) {
            adapter.symbols.clear()
            emptyGroup.visibility = View.VISIBLE
            mainList.visibility = View.GONE
        } else {
            val list = symbols.mapNotNull { TickerStatisticsCenter.symbolMap[it] }
            Collections.sort(list, currentOrderMode)
            val remainSymbols = symbols.filter { symbol ->
                list.find { it.symbol == symbol } == null
            }
            val viewSymbols = ArrayList<String>().apply {
                addAll(list.map { it.symbol })
                addAll(remainSymbols)
            }

            adapter.symbols.clear()
            adapter.symbols.addAll(viewSymbols)
            adapter.notifyDataSetChanged()

            emptyGroup.visibility = View.GONE
            mainList.visibility = View.VISIBLE
        }

    }

    override fun onStart() {
        super.onStart()
        updateList()
    }

    override fun refresh(isForce: Boolean) {
        val symbols =
            AccountCenter.getCurrentAccountFavExchangePairManager()?.favPairs ?: emptyList<String>()
        if (symbols.isEmpty()) {
            swipeRefresh?.isRefreshing = false
            return
        }
        marketVm.get24HPriceChangeBySymbols(symbols, isForce)
        marketVm.getAllTokenExchangeRate(isForce)
        marketVm.getAllOperatorInfo(isForce)
    }
}