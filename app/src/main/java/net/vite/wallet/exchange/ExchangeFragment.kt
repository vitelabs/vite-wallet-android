package net.vite.wallet.exchange

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_exchange.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.exchange.search.MarketPairSearchActivity
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.nut.HorizontalDivider
import net.vite.wallet.vitebridge.H5WebActivity
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class ExchangeFragment : Fragment() {
    val marketFragments = ArrayList<MarketFragment>()

    init {
        marketFragments.add(FavMarketFragment())
        marketFragments.add(MarketFragment.new(MarketFragment.BTC))
        marketFragments.add(MarketFragment.new(MarketFragment.ETH))
        marketFragments.add(MarketFragment.new(MarketFragment.VITE))
        marketFragments.add(MarketFragment.new(MarketFragment.USDT))
    }

    private val exchangeViewModel by activityViewModels<ExchangeViewModel>()
    private val marketVm by activityViewModels<MarketVM>()

    private val timer = Timer()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_exchange, container, false)

    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bannerAdapter = ExchangeBannerAdapter(Glide.with(this))
        val manager = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        exchangeBannerList.layoutManager = manager
        exchangeBannerList.adapter = bannerAdapter
        exchangeBannerList.addItemDecoration(HorizontalDivider())
        Handler().post {
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(exchangeBannerList)
        }

        timer.schedule(object : TimerTask() {
            override fun run() {
                try {
                    if (bannerAdapter.list.size > 1) {
                        exchangeBannerList.smoothScrollToPosition(
                            (manager.findFirstCompletelyVisibleItemPosition() + 1).rem(
                                bannerAdapter.list.size
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 2 * 1000, 5 * 1000)

        exchangeViewModel.bannerReqLd.observe(viewLifecycleOwner, {
            if (it.resp?.isNotEmpty() == true) {
                bannerAdapter.setList(it.resp!!)
            }
        })

        appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (verticalOffset + appBarLayout.totalScrollRange == 0) {
                topToolBar.visibility = View.VISIBLE
            } else {
                topToolBar.visibility = View.GONE
            }
        })

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
            startActivityForResult(Intent(requireActivity(), MarketPairSearchActivity::class.java), 1233)
        }
        exchangeFind1.setOnClickListener {
            startActivityForResult(Intent(requireActivity(), MarketPairSearchActivity::class.java), 1233)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1233 && resultCode == RESULT_OK) {
            val r = kotlin.runCatching {
                Gson().fromJson<TickerStatistics>(
                    data?.getStringExtra("result") ?: "", TickerStatistics::class.java
                )
            }.getOrNull() ?: return
            H5WebActivity.show(requireActivity(), r.getH5ExchangeUrl())
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
            exchangeViewModel.loadBanner( "en")
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
