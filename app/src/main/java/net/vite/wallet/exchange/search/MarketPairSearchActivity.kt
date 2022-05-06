package net.vite.wallet.exchange.search

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_market_pair_search.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.exchange.AccountFavExchangePairManager
import net.vite.wallet.exchange.TickerStatisticsCenter
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.utils.blueWhiteGreyDarkSelect
import net.vite.wallet.utils.viewbinding.findMyView
import net.vite.wallet.widget.RxTextWatcher

class MarketPairSearchActivity : UnchangableAccountAwareActivity() {
    val marketShortcut by findMyView<ViewGroup>(R.id.market_shortcut)

    lateinit var accountFavPairManager: AccountFavExchangePairManager
    lateinit var adapter: MarketPairSearchListAdapter
    var currentQuoteToken: String? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_market_pair_search)
        marketShortcut.children.mapNotNull { it as? TextView }.forEach { txtView ->
            txtView.blueWhiteGreyDarkSelect(false)
            txtView.setOnClickListener {
                if (txtView.text.toString() == currentQuoteToken) {
                    currentQuoteToken = null
                    txtView.blueWhiteGreyDarkSelect(false)
                    triggerSearch(searchEditText.text.toString())
                    return@setOnClickListener
                }
                currentQuoteToken = txtView.text.toString()
                triggerSearch(searchEditText.text.toString())

                marketShortcut.children.mapNotNull { it as? TextView }.forEach {
                    it.blueWhiteGreyDarkSelect(it === txtView)
                }
            }
        }

        accountFavPairManager = AccountCenter.getCurrentAccountFavExchangePairManager() ?: run {
            finish()
            return
        }

        adapter = MarketPairSearchListAdapter(accountFavPairManager) {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("result", Gson().toJson(it))
            })
            finish()
        }

        RxTextWatcher.onTextChanged(searchEditText).subscribe { pattern ->
            triggerSearch(pattern)
        }
        mainList.adapter = adapter
        searchHistoryClearImg.setOnClickListener {
            accountFavPairManager.deleteAllSearchHistory()
            getSearchHistory()
        }
        cancelButton.setOnClickListener {
            finish()
        }
        getSearchHistory()
    }

    private fun triggerSearch(pattern: String) {
        if (pattern == "" && currentQuoteToken == null) {
            updateList(emptyList())
            return
        }
        val list = (currentQuoteToken?.let { currentTradeToken ->
            TickerStatisticsCenter.symbolMap.values.filter { tickerStatistics ->
                val simplyTradeTokenSymbol =
                    TickerStatistics.symbolRmIndex(tickerStatistics.tradeTokenSymbol)
                simplyTradeTokenSymbol.contains(
                    pattern,
                    true
                ) && tickerStatistics.quoteTokenSymbol == currentTradeToken
            }
        } ?: TickerStatisticsCenter.symbolMap.values.filter { tickerStatistics ->
            val simplyQuoteTokenName =
                TickerStatistics.symbolRmIndex(tickerStatistics.quoteTokenSymbol)
            if (simplyQuoteTokenName.contains(pattern, true)) {
                return@filter true
            }
            val simplyTradeTokenSymbol =
                TickerStatistics.symbolRmIndex(tickerStatistics.tradeTokenSymbol)
            if (simplyTradeTokenSymbol.contains(pattern, true)) {
                return@filter true
            }
            return@filter false
        }).map { it.symbol }
        if (list.isNotEmpty()) {
            searchHistoryGroup.visibility = View.GONE
        } else {
            getSearchHistory()
        }
        updateList(list)
    }

    fun updateList(list: List<String>) {
        adapter.setList(list)
        if (list.isEmpty()) {
            emptyGroup.visibility = View.VISIBLE
            mainList.visibility = View.GONE
        } else {
            emptyGroup.visibility = View.GONE
            mainList.visibility = View.VISIBLE
        }
    }

    fun getSearchHistory() {
        val history = accountFavPairManager.getSearchHistory()
        if (history.isNotEmpty()) {
            searchHistoryGroup.visibility = View.VISIBLE
            updateList(history)
        } else {
            searchHistoryGroup.visibility = View.GONE

            val emptyList = ArrayList<String>().apply {
                add("All")
                addAll(TickerStatisticsCenter.httpCategoryCacheMap.values.flatMap {
                    it.map { it.symbol }
                })
            }
            updateList(emptyList)
        }

    }

}