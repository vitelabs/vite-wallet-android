package net.vite.wallet.kline.market.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.github.mikephil.charting.stockChart.dataManage.KLineDataManage
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_kline.*
import kotlinx.android.synthetic.main.activity_kline.view.*
import net.vite.wallet.*
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.exchange.*
import net.vite.wallet.exchange.net.ws.ExchangeWsHolder
import net.vite.wallet.exchange.wiget.SwitchExchangeFragment
import net.vite.wallet.network.http.vitex.Klines
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.network.http.vitex.VitexApi
import net.vite.wallet.network.toLocalReadableText
import org.json.JSONObject
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class KLineActivity : BaseActivity() {

    private var firstKlines: Klines? = null
    private lateinit var quoteTokenSymbol: String
    private lateinit var tradeTokenSymbol: String
    private lateinit var kTimeData: KLineDataManage

    @Volatile
    private var hasPullTradepairDate = false

    val buyListener = View.OnClickListener {
        finish()
        MainActivity.toTradeFragment(this, true, pairSymbol)
    }

    val sellListener = View.OnClickListener {
        finish()
        MainActivity.toTradeFragment(this, false, pairSymbol)
    }

    val marketVm: MarketVM by lazy {
        ViewModelProviders.of(this)[MarketVM::class.java]
    }

    val fragments = ArrayList<Fragment>()

    private lateinit var pairSymbol: String
    private lateinit var timeInterval: TimeInterval

    val fragment = SwitchExchangeFragment()

    fun switchPair() {
        if (fragment.isAdded) {
            return
        }
        val ft = supportFragmentManager.beginTransaction()
        ft.add(R.id.rootContainer, fragment, "SwitchExchangeFragment")
        ft.addToBackStack(null)
        ft.commit()
    }

    fun onExchangeMarketItemSwitched(symbol: String) {
//        lastSwitchMarketHandler?.complete(JsBridgeResp(data = mapOf("symbol" to symbol)))
//        lastSwitchMarketHandler = null
        kotlin.runCatching {
            supportFragmentManager?.popBackStackImmediate()
        }
        refreshNewPair(symbol)
    }

    override fun onPause() {
        super.onPause()
        ExchangeWsHolder.unsubTrade(pairSymbol)
        ExchangeWsHolder.unsubDepth(pairSymbol)
        ExchangeWsHolder.unsubKline(pairSymbol, TimeInterval.day.toString())
    }

    fun refreshNewPair(symbol: String) {
        ExchangeWsHolder.unsubTrade(pairSymbol)
        ExchangeWsHolder.unsubDepth(pairSymbol)
        ExchangeWsHolder.unsubKline(pairSymbol, TimeInterval.day.toString())

        updatePairSymbol(symbol);
//        klineVM.getTradeInfoLd(pairSymbol, false)
        marketVm.getAll24HPriceChangeByCategory(false)
        marketVm.getAllTokenExchangeRate(false)

        (fragments[0] as OrderFragment).updatePairSymbol(symbol)
        (fragments[1] as RecentTransactionsFragment).updatePairSymbol(symbol)
        (fragments[2] as TokenInfoFragment).updatePairSymbol(symbol)
        (fragments[3] as OperatorFragment).updatePairSymbol(symbol)

        (fragments[0] as OrderFragment).setGlobalListeners(buyListener, sellListener)
        (fragments[1] as RecentTransactionsFragment).setGlobalListeners(buyListener, sellListener)
        (fragments[2] as TokenInfoFragment).setGlobalListeners(buyListener, sellListener)
        (fragments[3] as OperatorFragment).setGlobalListeners(buyListener, sellListener)

        hasPullTradepairDate = false

        (fragments[2] as TokenInfoFragment).updatePairSymbol(symbol)
        (fragments[3] as OperatorFragment).updatePairSymbol(symbol)

        timeInterval = TimeInterval.day

        ExchangeWsHolder.subTrade(pairSymbol)
        ExchangeWsHolder.subDepth(pairSymbol)
        ExchangeWsHolder.subKline(pairSymbol, TimeInterval.day.toString())

        tradePairTxt.text = "$tradeTokenSymbol/$quoteTokenSymbol"
        switchPair.setOnClickListener {
            switchPair()
        }

        spinner.setSelection(0)
        chart.resetMA()
        pullKlineData()
        updateList()

    }

    fun updatePairSymbol(symbol: String) {
        tradeTokenSymbol = symbol.split("_")[0]
        quoteTokenSymbol = symbol.split("_")[1]
        pairSymbol = tradeTokenSymbol + "_" + quoteTokenSymbol
        refreshFragmentTopbarUI(pager.currentItem)
    }

    override fun onResume() {
        super.onResume()
        ExchangeWsHolder.subTrade(pairSymbol)
        ExchangeWsHolder.subDepth(pairSymbol)
        ExchangeWsHolder.subKline(pairSymbol, TimeInterval.day.toString())
    }

    fun addFavPair(favPair: String) {
        AccountCenter.getCurrentAccountFavExchangePairManager()?.addFav(favPair)
    }


    fun deleteFavPair(favPair: String) {
        AccountCenter.getCurrentAccountFavExchangePairManager()?.deleteFav(favPair)
    }


    fun getAllFavPairs(): List<String> {
        return AccountCenter.getCurrentAccountFavExchangePairManager()?.favPairs
            ?: emptyList()
    }

    fun isFavPair(symbol: String): Boolean {
        return getAllFavPairs().contains(symbol)
    }

    companion object {
        fun show(
            context: Context,
            tradeTokenSymbol: String,
            quoteTokenSymbol: String
        ) {
            val quoteToken = when (quoteTokenSymbol) {
                "BTC" -> "BTC-000"
                "ETH" -> "ETH-000"
                "USDT" -> "USDT-000"
                else -> quoteTokenSymbol
            }
            val i = Intent(context, KLineActivity::class.java)
            i.putExtra("tradeTokenSymbol", tradeTokenSymbol)
                .putExtra("quoteTokenSymbol", quoteToken)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kline)

        tradeTokenSymbol = intent.getStringExtra("tradeTokenSymbol")!!
        quoteTokenSymbol = intent.getStringExtra("quoteTokenSymbol")!!

        pairSymbol = tradeTokenSymbol + "_" + quoteTokenSymbol

        fragments.add(OrderFragment.newInstance(pairSymbol))
        fragments.add(RecentTransactionsFragment.newInstance(pairSymbol))
        fragments.add(TokenInfoFragment.newInstance(pairSymbol))
        fragments.add(OperatorFragment.newInstance(pairSymbol))

        timeInterval = TimeInterval.day

        tradePairTxt.text = "$tradeTokenSymbol/$quoteTokenSymbol"
        switchPair.setOnClickListener {
            switchPair()
        }

        kTimeData = KLineDataManage(this)

        chart.initChart(false)
//        //test data
//        try {
//            obj = JSONObject(ChartData.KLINEDATA)
//        } catch (e: JSONException) {
//            e.printStackTrace()
//        }

        startPoll()

        val adapter = KlineBottomPageAdapter(supportFragmentManager, fragments)
        pager.adapter = adapter
        klineTabLayout.setupWithViewPager(pager)
        klineTabLayout.getTabAt(0)?.text = getString(R.string.order)
        klineTabLayout.getTabAt(1)?.text = getString(R.string.recently_traded)
        klineTabLayout.getTabAt(2)?.text = getString(R.string.token)
        klineTabLayout.getTabAt(3)?.text = getString(R.string.operactor)

        val data_list = ArrayList<String>()
        data_list.add(getString(R.string.kline_time_interval_day))
        data_list.add(getString(R.string.kline_time_interval_minute))
        data_list.add(getString(R.string.kline_time_interval_minute30))
        data_list.add(getString(R.string.kline_time_interval_hour))
        data_list.add(getString(R.string.kline_time_interval_hour2))
        data_list.add(getString(R.string.kline_time_interval_hour4))
        data_list.add(getString(R.string.kline_time_interval_hour6))
        data_list.add(getString(R.string.kline_time_interval_hour12))
        data_list.add(getString(R.string.kline_time_interval_week))

        refreshFragmentTopbarUI(0);


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
                if (position == 3) {
                    (fragments[3] as OperatorFragment).updateUI()
                    fragment_topbar.visibility = View.GONE
                } else if (position == 2) {
                    (fragments[2] as TokenInfoFragment).updateUI()
                    fragment_topbar.visibility = View.GONE
                } else if (position == 1) {
                    fragment_topbar.visibility = View.VISIBLE
                    refreshFragmentTopbarUI(position)
                } else if (position == 0) {
                    fragment_topbar.visibility = View.VISIBLE
                    refreshFragmentTopbarUI(position)
                }
            }

        })


        marketVm.httpMarketLd.observe(this, Observer {
            if (it.isLoading()) {
                return@Observer
            }
            updateList()

        })

        marketVm.getAll24HPriceChangeByCategory(false)
        marketVm.getAllTokenExchangeRate(false)

//        klineVM.tradeInfoLd.observe(this, Observer {
//            logt("xirtam got new tickers ${it.tryGetErrorMsg()} ${it.resp}")
//        })

//        klineVM.getTradeInfoLd(pairSymbol, false)
//
//        Observable.fromCallable {
//            klineVM.getTradeInfoLd(pairSymbol, false)
//        }.subscribeOn(Schedulers.io())
//            .repeatWhen { it.delay(5, TimeUnit.SECONDS) }
//            .retryWhen { it.delay(5, TimeUnit.SECONDS) }
//            .subscribe({
//
//            }, {
//                logi("xirtam kline get getTradeInfoLd err ${it.message}")
//            })

        refreshNewPair(pairSymbol)

        val arr_adapter =
            ArrayAdapter<String>(this, R.layout.spinner_layout, data_list)
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arr_adapter
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, index: Int, arg3: Long) {
                ExchangeWsHolder.unsubKline(pairSymbol, timeInterval.toString())
                timeInterval = when (index) {
                    0 -> TimeInterval.day
                    1 -> TimeInterval.minute
                    2 -> TimeInterval.minute30
                    3 -> TimeInterval.hour
                    4 -> TimeInterval.hour2
                    5 -> TimeInterval.hour4
                    6 -> TimeInterval.hour6
                    7 -> TimeInterval.hour12
                    8 -> TimeInterval.week
                    else -> TimeInterval.day
                }
                ExchangeWsHolder.subKline(pairSymbol, timeInterval.toString())

//            updateText(kLineData.getKLineDatas().size() - 1, false);
                chart.resetMA()
                pullKlineData()

            }

        }

        klineBuy.setOnClickListener(buyListener)
        klineSell.setOnClickListener(sellListener)
    }

    private fun refreshFragmentTopbarUI(position: Int) {
        if (position == 0) {
            fragment_topbar.fragment_topbar_text1.text =
                getString(R.string.amount) + "($tradeTokenSymbol)"
            fragment_topbar.fragment_topbar_text2.text =
                getString(R.string.price) + "($quoteTokenSymbol)"
            fragment_topbar.fragment_topbar_text3.text =
                getString(R.string.amount) + "($tradeTokenSymbol)"
        } else if (position == 1) {
            fragment_topbar.fragment_topbar_text1.text =
                getString(R.string.time)
            fragment_topbar.fragment_topbar_text2.text =
                getString(R.string.price)
            fragment_topbar.fragment_topbar_text3.text =
                getString(R.string.amount)
        }
    }


    private fun pullKlineData() {

        val limit = 500
//        val nowTime = System.currentTimeMillis() / 1000
//        val startTime = nowTime - getTimeIntervalValue() * limit

        VitexApi.getKlines(
            symbol = pairSymbol,
            interval = timeInterval.toString(),
            limit = limit
        ).subscribe({
            KlineCenter.symbolMap.clear()

            val pricePrecision =
                TickerStatisticsCenter.symbolMap[pairSymbol!!]?.pricePrecision ?: 0
            val quantityPrecision =
                TickerStatisticsCenter.symbolMap[pairSymbol!!]?.quantityPrecision ?: 0
            chart.setQuantityPrecision(quantityPrecision)

            firstKlines = it
            if (it.t.size == 0) {
                chart.reset()
            } else {
                kTimeData = KLineDataManage(this)
                kTimeData.parseKlineData(JSONObject(Gson().toJson(it)), pricePrecision, false)
                chart.isFirst = true
                chart.setDataToChart(kTimeData)
                chart.notifyDataSetChanged()
            }
        }, {
            logt("get kline error ${it.message}")
            KlineCenter.symbolMap.clear()
            firstKlines?.clear()
            chart.reset()
        })
    }

    private fun updateKlineView() {
        firstKlines?.let { firstKlines ->
            KlineCenter.symbolMap[pairSymbol]?.forEach { kline ->
                if (firstKlines.t[firstKlines.t.size - 1] < kline.t) {// need combo, add
                    firstKlines.t.add(kline.t)
                    firstKlines.c.add(kline.c.toString())
                    firstKlines.p.add(kline.o.toString())
                    firstKlines.h.add(kline.h.toString())
                    firstKlines.l.add(kline.l.toString())
                    firstKlines.v.add(kline.v.toString())
                }

                if (firstKlines.t[firstKlines.t.size - 1] == kline.t) { // update
                    firstKlines.c[firstKlines.t.size - 1] = kline.c.toString()
                    firstKlines.p[firstKlines.t.size - 1] = kline.o.toString()
                    firstKlines.h[firstKlines.t.size - 1] = kline.h.toString()
                    firstKlines.l[firstKlines.t.size - 1] = kline.l.toString()
                    firstKlines.v[firstKlines.t.size - 1] = kline.v.toString()
                }
            }

//            chart.reset()
            kTimeData = KLineDataManage(this)
            val pricePrecision =
                TickerStatisticsCenter.symbolMap[pairSymbol!!]?.pricePrecision ?: 0
            kTimeData.parseKlineData(JSONObject(Gson().toJson(firstKlines)), pricePrecision, false)
            chart.setDataToChart(kTimeData)
            chart.notifyDataSetChanged()
        }
    }

    private fun getTimeIntervalValue(): Int {
        return when (timeInterval) {
            TimeInterval.minute30 -> 30 * 60
            TimeInterval.minute -> 60
            TimeInterval.hour -> 60 * 60
            TimeInterval.day -> 60 * 60 * 24
            TimeInterval.week -> 60 * 60 * 24 * 7
            TimeInterval.hour2 -> 60 * 60 * 2
            TimeInterval.hour4 -> 60 * 60 * 4
            TimeInterval.hour6 -> 60 * 60 * 6
            TimeInterval.hour12 -> 60 * 60 * 12
            else -> 0
        }
    }

    var currentOrderMode = TickerStatistics.defaultOrderFunc
        set(value) {
            field = value
            updateList()
        }

    fun updateList() {
        val mapSymbol = quoteTokenSymbol.indexOf("-").let {
            if (it == -1) quoteTokenSymbol else quoteTokenSymbol.substring(0, it)
        }

        val list =
            TickerStatisticsCenter.httpCategoryCacheMap[mapSymbol] ?: java.util.ArrayList()
        Collections.sort(list, currentOrderMode)

        list.forEach {
            if (it.symbol == pairSymbol) {
                priceTxt.text = it.closePrice
                priceTxt.setTextColor(
                    if (it.priceChange.startsWith("-")) Color.parseColor("#E5494D") else Color.parseColor(
                        "#01D764"
                    )
                )

                upDownTxt.text =
                    if (it.priceChangePercent < 0)
                        "${String.format("%.2f", it.priceChangePercent * 100)}%"
                    else
                        "+${String.format("%.2f", it.priceChangePercent * 100)}%"
                upDownTxt.setTextColor(
                    if (it.priceChange.startsWith("-")) Color.parseColor("#E5494D") else Color.parseColor(
                        "#01D764"
                    )
                )
                upDownImg.setImageResource(if (it.priceChange.startsWith("-")) R.mipmap.kline_down else R.mipmap.kline_up)
                highTxt.text = it.highPrice
                lowTxt.text = it.lowPrice
                vol24hTxt.text = it.amount

                val closePriceDecimal =
                    it.closePrice.toBigDecimalOrNull()

                val rate = TickerStatisticsCenter.tokenRateCacheMap[quoteTokenSymbol]?.getRate()
                    ?: BigDecimal.ZERO

                if (rate != null && closePriceDecimal != null) {
                    var closePriceDecimalCurrencyFormat =
                        closePriceDecimal.multiply(rate).toLocalReadableText(6, true)
                    if (closePriceDecimalCurrencyFormat == "0") {
                        closePriceDecimalCurrencyFormat = "0.00"
                    }
                    valuesTxt.text = "≈" + ViteConfig.get()
                        .currentCurrencySymbol() + closePriceDecimalCurrencyFormat
                } else {
                    valuesTxt.text = "- -"
                }

                val miningRateData = TickerStatisticsCenter.getMiningRate(pairSymbol)
                if (TickerStatisticsCenter.isOrderAndTradeMiningPair(pairSymbol)) {
                    minerImg.visibility = View.VISIBLE
                    minerImg.setImageResource(if (miningRateData == null) R.mipmap.mining_all_icon_big else R.mipmap.mining_all_icon_small)
                } else if (TickerStatisticsCenter.isOrderMiningPair(pairSymbol)) {
                    minerImg.visibility = View.VISIBLE
                    minerImg.setImageResource(if (miningRateData == null) R.mipmap.mining_order_icon_big else R.mipmap.mining_order_icon_small)
                } else if (TickerStatisticsCenter.isTradeMiningPair(pairSymbol)) {
                    minerImg.visibility = View.VISIBLE
                    minerImg.setImageResource(R.mipmap.mining_trade_icon_big)
                } else {
                    minerImg.visibility = View.GONE
                }

                TickerStatisticsCenter.getMiningRate(pairSymbol).let {
                    if (it == null) {
                        miningRate.visibility = View.GONE
                    } else {
                        miningRate.visibility = View.VISIBLE
                        miningRate.text = "$it"
                    }
                }

                favImg.setImageResource(if (isFavPair(pairSymbol)) R.mipmap.kline_favourite else R.mipmap.kline_unfavourite)
                favImg.setOnClickListener {
                    if (isFavPair(pairSymbol)) {
                        deleteFavPair(pairSymbol)
                    } else {
                        addFavPair(pairSymbol)
                    }
                    favImg.setImageResource(if (isFavPair(pairSymbol)) R.mipmap.kline_favourite else R.mipmap.kline_unfavourite)
                }

                if (!hasPullTradepairDate) {

                    val tradeToken = it.tradeToken
                    val quoteToken = it.quoteToken
                    VitexApi.getOperatorTradepair(
                        tradeToken = tradeToken,
                        quoteToken = quoteToken
                    ).subscribe({

                        hasPullTradepairDate = true
                        if (it.operatorInfo == null) {
                            operatorIcon.setImageResource(R.mipmap.anonymous_operator)
                        } else {
                            Glide.with(applicationContext).load(it.operatorInfo?.icon)
                                .into(operatorIcon)
                        }

                        (fragments[2] as TokenInfoFragment).updateData(it)
                        (fragments[3] as OperatorFragment).updateData(it)


                    }, {
                        logt("xirtam getOperatorTradepair error ${it.message}")
                    })
                }
            }
        }


        val depthList = DepthListCenter.symbolMap[pairSymbol]
        depthList?.let {
            (fragments[0] as OrderFragment).updateData(it)
        }


        val tradeList = TradeListCenter.symbolMap[pairSymbol]
        (fragments[1] as RecentTransactionsFragment).updateData(tradeList)

    }

    var disposable: Disposable? = null
    private fun startPoll() {
        disposable?.dispose()
        disposable = null
        disposable = Observable.fromCallable { 1 }
            .repeatWhen { it.delay(2, TimeUnit.SECONDS) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                updateList()
                updateKlineView()
            }
    }

    fun toTradePage(pairSymbol: String) {
        finish()
        MainActivity.toTradeFragment(this, true, pairSymbol)
    }
}

enum class TimeInterval {
    minute, minute30, hour, day, week, hour2, hour4, hour6, hour12
}
