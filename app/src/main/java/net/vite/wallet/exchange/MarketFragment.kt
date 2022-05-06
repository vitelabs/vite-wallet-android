package net.vite.wallet.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_market.*
import net.vite.wallet.R
import net.vite.wallet.network.http.vitex.TickerStatistics
import java.util.*

open class MarketFragment : Fragment() {

    companion object {
        const val BTC = "BTC"
        const val ETH = "ETH"
        const val VITE = "VITE"
        const val USDT = "USDT"
        val AllTypeArray = arrayOf(BTC, ETH, VITE, USDT)
        fun getRequestCode(type: String): Int {
            return AllTypeArray.indexOf(type)
        }

        fun new(type: String): MarketFragment {
            return MarketFragment().apply {
                arguments = Bundle().apply {
                    putString("type", type)
                }
            }
        }
    }

    var currentOrderMode = TickerStatistics.defaultOrderFunc
        set(value) {
            field = value
            updateList()
        }

    val marketVm by viewModels<MarketVM>()

    val adapter = MarketAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_market, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val type = arguments?.getString("type") ?: ""
        mainList.adapter = adapter
        marketVm.httpMarketLd.observe(viewLifecycleOwner, Observer {
            if (it.requestCode != getRequestCode(type)) {
                return@Observer
            }
            swipeRefresh.isRefreshing = it.isLoading()
            if (it.isLoading()) {
                return@Observer
            }
            updateList()
        })
        marketVm.tokenRateLd.observe(viewLifecycleOwner, Observer {
            adapter.notifyDataSetChanged()
        })

        swipeRefresh.setOnRefreshListener {
            refresh(true)
        }

        updateList()
        refresh(false)
    }

    open fun updateList() {
        val type = arguments?.getString("type") ?: ""
        val list =
            TickerStatisticsCenter.httpCategoryCacheMap[type] ?: ArrayList<TickerStatistics>()
        Collections.sort(list, currentOrderMode)
        adapter.symbols.clear()
        adapter.symbols.addAll(list.map { it.symbol })
        if (adapter.symbols.isEmpty()) {
            emptyGroup.visibility = View.VISIBLE
        } else {
            emptyGroup.visibility = View.GONE
            adapter.notifyDataSetChanged()
        }
    }

    override fun onStart() {
        super.onStart()
        updateList()
    }


    open fun refresh(isForce: Boolean) {
        val type = arguments?.getString("type") ?: ""
        marketVm.get24HPriceChangeByCategory(type, isForce, getRequestCode(type))
        marketVm.getAllTokenExchangeRate(isForce)
        marketVm.getMiningSetting(isForce)
    }

    override fun onPause() {
        super.onPause()
        swipeRefresh.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        swipeRefresh.isEnabled = true
    }
}



