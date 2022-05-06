package net.vite.wallet.exchange.wiget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_switch_market_pair.*
import net.vite.wallet.MainActivity
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.exchange.MarketPageAdapter
import net.vite.wallet.exchange.MarketVM
import net.vite.wallet.exchange.search.MarketPairSearchActivity
import net.vite.wallet.kline.market.activity.KLineActivity
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.vitebridge.H5WebActivity
import java.util.concurrent.TimeUnit

class SwitchExchangeFragment : Fragment() {
    val marketFragments = ArrayList<SwitchMarketFragment>()

    init {
        marketFragments.add(SwitchFavMarketFragment())
        marketFragments.add(SwitchMarketFragment.new(SwitchMarketFragment.BTC))
        marketFragments.add(SwitchMarketFragment.new(SwitchMarketFragment.ETH))
        marketFragments.add(SwitchMarketFragment.new(SwitchMarketFragment.VITE))
        marketFragments.add(SwitchMarketFragment.new(SwitchMarketFragment.USDT))
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_switch_market_pair, container, false)


    val marketVm: MarketVM by lazy {
        ViewModelProviders.of(activity!!)[MarketVM::class.java]
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = MarketPageAdapter(childFragmentManager, marketFragments)
        pager.adapter = adapter
        tabLayout.setupWithViewPager(pager)
        tabLayout.getTabAt(0)?.setText(R.string.exchange_fav)
        tabLayout.getTabAt(1)?.text = "BTC"
        tabLayout.getTabAt(2)?.text = "ETH"
        tabLayout.getTabAt(3)?.text = "VITE"
        tabLayout.getTabAt(4)?.text = "USDT"

        val symbols = AccountCenter.getCurrentAccountFavExchangePairManager()?.getFav()
            ?: emptyList<String>()
        if (symbols.isEmpty()) {
            pager.currentItem = 1
        }

        startPoll()

        tokenSymbolTxt.setOnClickListener {
            val currentFragment = marketFragments[pager.currentItem]
            currentFragment.currentOrderMode = when (currentFragment.currentOrderMode) {
                TickerStatistics.nameDescendingFunc -> {
                    TickerStatistics.defaultOrderFunc
                }
                TickerStatistics.nameAscendingFunc -> {
                    TickerStatistics.nameDescendingFunc
                }
                else -> {
                    TickerStatistics.nameAscendingFunc
                }
            }

            refreshOrderIcon()
        }

        priceOrderText.setOnClickListener {
            val currentFragment = marketFragments[pager.currentItem]
            currentFragment.currentOrderMode = when (currentFragment.currentOrderMode) {
                TickerStatistics.priceDescendingFunc -> {
                    TickerStatistics.defaultOrderFunc
                }
                TickerStatistics.priceAscendingFunc -> {
                    TickerStatistics.priceDescendingFunc
                }
                else -> {
                    TickerStatistics.priceAscendingFunc
                }
            }
            refreshOrderIcon()
        }

        changeOrderText.setOnClickListener {
            val currentFragment = marketFragments[pager.currentItem]
            currentFragment.currentOrderMode = when (currentFragment.currentOrderMode) {
                TickerStatistics.priceChangePercentDescendingFunc -> {
                    TickerStatistics.defaultOrderFunc
                }
                TickerStatistics.priceChangePercentAscendingFunc -> {
                    TickerStatistics.priceChangePercentDescendingFunc
                }
                else -> {
                    TickerStatistics.priceChangePercentAscendingFunc
                }
            }

            refreshOrderIcon()
        }
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                refreshOrderIcon()
                marketFragments[pager.currentItem].refresh(false)
            }

        })

        exchangeFind.setOnClickListener {
            startActivityForResult(Intent(activity!!, MarketPairSearchActivity::class.java), 1233)
        }

        closeBtn.setOnClickListener {
            fragmentManager?.popBackStackImmediate()
        }
        coverTop.setOnClickListener {
            fragmentManager?.popBackStackImmediate()
        }

        marketVm.getAll24HPriceChangeByCategory(false)
        marketVm.getAllTokenExchangeRate(false)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1233 && resultCode == Activity.RESULT_OK) {
            val r = kotlin.runCatching {
                Gson().fromJson<TickerStatistics>(
                    data?.getStringExtra("result") ?: "", TickerStatistics::class.java
                )
            }.getOrNull() ?: return
            if (activity is KLineActivity) {
                (activity as KLineActivity).onExchangeMarketItemSwitched(r.symbol)
            } else if (activity is MainActivity) {
                (activity as MainActivity).onExchangeMarketItemSwitched(r.symbol)
            } else if (activity is H5WebActivity) {
                (activity as H5WebActivity).onExchangeMarketItemSwitched(r.symbol)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun startPoll() {
        disposable?.dispose()
        disposable = null
        disposable = Observable.fromCallable { 1 }
            .repeatWhen { it.delay(2, TimeUnit.SECONDS) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (!isHidden) {
                    pager?.let {
                        marketFragments[pager.currentItem].updateList()
                    }
                }
            }
    }

    var disposable: Disposable? = null

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            marketVm.getAll24HPriceChangeByCategory(false)
            marketVm.getAllTokenExchangeRate(false)
            startPoll()
        } else {
            disposable?.dispose()
            disposable = null
        }
    }


    fun refreshOrderIcon() {
        val currentFragment = marketFragments[pager.currentItem]
        when (currentFragment.currentOrderMode) {
            TickerStatistics.priceChangePercentDescendingFunc -> {
                priceOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                changeOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_descending),
                    null
                )
                tokenSymbolTxt.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
            }
            TickerStatistics.priceChangePercentAscendingFunc -> {
                priceOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                changeOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_ascending),
                    null
                )
                tokenSymbolTxt.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
            }
            TickerStatistics.priceAscendingFunc -> {
                priceOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_ascending),
                    null
                )
                changeOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                tokenSymbolTxt.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
            }
            TickerStatistics.priceDescendingFunc -> {
                priceOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_descending),
                    null
                )
                changeOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                tokenSymbolTxt.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
            }
            TickerStatistics.defaultOrderFunc -> {
                priceOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                changeOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                tokenSymbolTxt.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
            }
            TickerStatistics.nameAscendingFunc -> {
                priceOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                changeOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                tokenSymbolTxt.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_ascending),
                    null
                )
            }
            TickerStatistics.nameDescendingFunc -> {
                priceOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                changeOrderText.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_default),
                    null
                )
                tokenSymbolTxt.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    activity?.resources?.getDrawable(R.mipmap.exchange_order_descending),
                    null
                )
            }
        }
    }


}