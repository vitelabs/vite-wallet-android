package net.vite.wallet.trade

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_trade.*
import kotlinx.android.synthetic.main.fragment_trade.view.*
import net.vite.wallet.*
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.activities.BaseTxSendFragment
import net.vite.wallet.balance.DexAccountFundInfoPoll
import net.vite.wallet.balance.InternalTransferActivity
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.constants.DexCancelContractAddress
import net.vite.wallet.constants.DexContractAddress
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.dialog.TradeConfirmDialog
import net.vite.wallet.exchange.DepthListCenter
import net.vite.wallet.exchange.OrderCenter
import net.vite.wallet.exchange.SpecialMarketCenter
import net.vite.wallet.exchange.TickerStatisticsCenter
import net.vite.wallet.exchange.history.TradeHistoryDetailsTabActivity
import net.vite.wallet.exchange.net.ws.ExchangeWsHolder
import net.vite.wallet.exchange.wiget.SwitchExchangeFragment
import net.vite.wallet.kline.market.activity.KLineActivity
import net.vite.wallet.logi
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.http.vitex.*
import net.vite.wallet.network.rpc.*
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.utils.*
import net.vite.wallet.vitebridge.H5WebActivity
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class TradeFragment : BaseTxSendFragment() {
    private var placeOrderInfo: PlaceOrderInfo? = null
    private var depthBids: MutableList<Depth?> = mutableListOf()
    private var depthAsks: MutableList<Depth?> = mutableListOf()
    private var quoteAccountFundInfo: GetAccountFundInfoResp? = null
    private var tradeAccountFundInfo: GetAccountFundInfoResp? = null
    private var tradeTokenInfo: NormalTokenInfo? = null
    private var quoteTokenInfo: NormalTokenInfo? = null
    private lateinit var rootView: View
    private var symbolPair: String? = null
    private var tradeTokenSymbol: String? = null
    private var quoteTokenSymbol: String? = null
    private var isSell = false
    private var tradeBlanace: Double? = null
    private var quoteBlanace: Double? = null
    private var hasPercentSelected: Int = -1
    private var viteBalance = "--"

    private var tryRefreshVipStateCount = 10
    private var isPause = false
    private var curPrice: String = ""
    private var curQuantity: String = ""
    private var myOrders: MutableList<Order>? = null
    private var mustShowWarning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            symbolPair = symbolPair ?: it.getString(ARG_PARAM1)
            if (!isSell) {//default value will be replaced
                isSell = it.getBoolean(ARG_PARAM2)
            }

            tradeTokenSymbol = symbolPair?.substring(0, symbolPair?.indexOf("_") ?: 0)
            quoteTokenSymbol =
                symbolPair?.substring(
                    (symbolPair?.indexOf("_") ?: 0) + 1,
                    symbolPair?.length ?: 1
                )

            quoteTokenSymbol = when (quoteTokenSymbol) {
                "BTC" -> "BTC-000"
                "ETH" -> "ETH-000"
                "USDT" -> "USDT-000"
                else -> quoteTokenSymbol
            }
        }

    }

    val fragment = SwitchExchangeFragment()

    fun setPair(pair: String) {
        if (this.isAdded) {
            refreshNewPair(pair)
        } else {
            symbolPair = pair
        }
    }

    fun setIsSell(isSell: Boolean) {
        this.isSell = isSell
        if (this::rootView.isInitialized) {
            refreshBuyButton()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_trade, container, false)

        rootView.buySellRadioGroup.setOnCheckedChangeListener { radioGroup: RadioGroup, viewId: Int ->
            if (SpecialMarketCenter.isStopped(symbolPair!!)) {
                rootView.sellCoinTxt.visibility = View.GONE
                rootView.buyCoinTxt.visibility = View.GONE
                rootView.tradeSuspended.visibility = View.VISIBLE
            } else {
                if (viewId == R.id.radioButtonBuy) {
                    rootView.sellCoinTxt.visibility = View.GONE
                    rootView.tradeSuspended.visibility = View.GONE
                    rootView.buyCoinTxt.visibility = View.VISIBLE
                    rootView.buyPriceEdt.hint = getString(R.string.buyPrice)
                    rootView.buyAmountEdt.hint = getString(R.string.buyAmount)
                    isSell = false
                } else if (viewId == R.id.radioButtonSell) {
                    rootView.sellCoinTxt.visibility = View.VISIBLE
                    rootView.tradeSuspended.visibility = View.GONE
                    rootView.buyCoinTxt.visibility = View.GONE
                    rootView.buyPriceEdt.hint = getString(R.string.sellPrice)
                    rootView.buyAmountEdt.hint = getString(R.string.sellAmount)
                    isSell = true
                }
            }
            curQuantity = ""
            rootView.buyAmountEdt.setText("")
            refreshBalance()
            if (rootView.miningNotice.visibility == View.VISIBLE) {
                prepareShowMiningNotice()
            }
        }
        refreshBuyButton()

        rootView.switchPair.setOnClickListener {
            switchPair()
        }
        rootView.klineImg.setOnClickListener {
            KLineActivity.show(activity as Context, tradeTokenSymbol!!, quoteTokenSymbol!!)
        }
        rootView.mining.setOnClickListener {
            var url =
                NetConfigHolder.netConfig.vitexH5UrlPrefix + "#/mining?activeTab=mining&hideSelectTab=true"

            url += "&address=${AccountCenter.currentViteAddress()}"
            url += "&currency=${ViteConfig.get().currentCurrency().toLowerCase(Locale.ENGLISH)}"
            url += "&lang=${
                if (ViteConfig.get().context.isChinese()) {
                    "zh"
                } else {
                    "en"
                }
            }"

            H5WebActivity.show(activity as Context, url)
        }

        rootView.bonus.setOnClickListener {
            var url =
                NetConfigHolder.netConfig.vitexH5UrlPrefix + "#/mining?activeTab=dividend&hideSelectTab=true"
            url += "&address=${AccountCenter.currentViteAddress()}"
            url += "&currency=${ViteConfig.get().currentCurrency().toLowerCase(Locale.ENGLISH)}"
            url += "&lang=${
                if (ViteConfig.get().context.isChinese()) {
                    "zh"
                } else {
                    "en"
                }
            }"
            H5WebActivity.show(activity as Context, url)
        }


        rootView.historyOrder.setOnClickListener {

            startActivity(Intent(requireActivity(), TradeHistoryDetailsTabActivity::class.java))

//            var url =
//                NetConfigHolder.netConfig.vitexH5UrlPrefix + "#/order?activeTab=historyOrders&hideSelectTab=true"
//            url += "&address=${AccountCenter.currentViteAddress()}"
//            url += "&currency=${ViteConfig.get().currentCurrency().toLowerCase(Locale.ENGLISH)}"
//            url += "&lang=${
//                if (ViteConfig.get().context.isChinese()) {
//                    "zh"
//                } else {
//                    "en"
//                }
//            }"
//            H5WebActivity.show(activity as Context, url)
        }

        rootView.favImg.setOnClickListener {
            if (isFavPair(symbolPair!!)) {
                deleteFavPair(symbolPair!!)
            } else {
                addFavPair(symbolPair!!)
            }
            favImg.setImageResource(if (isFavPair(symbolPair!!)) R.mipmap.kline_favourite else R.mipmap.kline_unfavourite)
        }

        VitexApi.getTokenExchangeRate(listOf("VITE", "BTC-000", "ETH-000", "USDT-000")).subscribe({
            it.forEach {
                TickerStatisticsCenter.tokenRateCacheMap[it.tokenSymbol] = it
            }
//            refreshNewPair(symbolPair!!)
        }, {
            loge(it)
        })

        refreshUI()
        startPoll()

        rootView.priceAdd.setOnClickListener {
            val price = rootView.buyPriceEdt.text.toString().takeIf { it.isNotEmpty() }
            val precision =
                TickerStatisticsCenter.symbolMap[symbolPair!!]?.pricePrecision
                    ?: run { return@setOnClickListener }

            price?.let {
                val value =
                    price.toBigDecimal().plus(BigDecimal.ONE.divide(BigDecimal.TEN.pow(precision)))
                        .toPlainString()
                curPrice = value
                rootView.buyPriceEdt.setText(value)
            }
        }

        rootView.priceMin.setOnClickListener {
            val price = rootView.buyPriceEdt.text.toString().takeIf { it.isNotEmpty() }
            val precision =
                TickerStatisticsCenter.symbolMap[symbolPair!!]?.pricePrecision
                    ?: run { return@setOnClickListener }

            price?.let {
                val min = BigDecimal.ONE.divide(BigDecimal.TEN.pow(precision))
                val value = if (price.toBigDecimal() >= min)
                    price.toBigDecimal().minus(min).toPlainString() else "0"
                curPrice = value
                rootView.buyPriceEdt.setText(value)

            }
        }

        rootView.amountAdd.setOnClickListener {
            val quantity = rootView.buyAmountEdt.text.toString().takeIf { it.isNotEmpty() }
            val precision =
                TickerStatisticsCenter.symbolMap[symbolPair!!]?.quantityPrecision
                    ?: run { return@setOnClickListener }

            quantity?.let {
                val value =
                    quantity.toBigDecimal()
                        .plus(BigDecimal.ONE.divide(BigDecimal.TEN.pow(precision)))
                        .toPlainString()
                curQuantity = value
                rootView.buyAmountEdt.setText(value)
            }
        }

        rootView.amountMin.setOnClickListener {
            val quantity = rootView.buyAmountEdt.text.toString().takeIf { it.isNotEmpty() }
            val precision =
                TickerStatisticsCenter.symbolMap[symbolPair!!]?.quantityPrecision
                    ?: run { return@setOnClickListener }

            quantity?.let {
                val min = BigDecimal.ONE.divide(BigDecimal.TEN.pow(precision))
                val value = if (quantity.toBigDecimal() >= min)
                    quantity.toBigDecimal().minus(min).toPlainString() else "0"
                curQuantity = value
                rootView.buyAmountEdt.setText(value)
            }
        }
        mustShowWarning = true
        return rootView
    }

    private fun refreshUI() {

        resetDepth()

        OrderCenter.symbolMap[symbolPair]?.let {
            if (rootView.openOrders.adapter == null || rootView.openOrders.adapter !is OpenOrderListAdapter) {
                rootView.openOrders.adapter = OpenOrderListAdapter(it, this)
            } else {
                if ((rootView.openOrders.adapter as OpenOrderListAdapter).itemCount == 0) {
                    rootView.emptyGroup.visibility = View.VISIBLE
                    rootView.openOrders.visibility = View.GONE
                } else {
                    rootView.emptyGroup.visibility = View.GONE
                    rootView.openOrders.visibility = View.VISIBLE
                }

                (rootView.openOrders.adapter as OpenOrderListAdapter).newData(it)
            }
        }

        DepthListCenter.symbolMap[symbolPair]?.asks?.sortByDescending {
            //wrap bugs
            it.price
        }

        DepthListCenter.symbolMap[symbolPair]?.let {
            refreshDepth(it)
        }

        rootView.tradeTokenName.text = tradeTokenSymbol
        rootView.quoteTokenName.text = "/$quoteTokenSymbol"

        rootView.zeroFeeIcon.visibility =
            if (TickerStatisticsCenter.isZeroFee(symbolPair!!)) View.VISIBLE else View.GONE

        val mapSymbol = quoteTokenSymbol?.indexOf("-").let {
            if (it == -1) quoteTokenSymbol else quoteTokenSymbol?.substring(0, it!!)
        }

        val list =
            TickerStatisticsCenter.httpCategoryCacheMap[mapSymbol] ?: ArrayList()
        Collections.sort(list, currentOrderMode)
        refreshTickers(list)

        rootView.percent1.setOnClickListener {
            curQuantity = calcAmount(0.25)
            rootView.buyAmountEdt.setText(curQuantity)

            rootView.percent1.setBackgroundResource(R.drawable.trade_button_bg)
            rootView.percent2.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent3.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent4.setBackgroundResource(R.drawable.trade_button_bg_grey)

            rootView.percent1.setTextColor(Color.parseColor("#FF007AFF"))
            rootView.percent2.setTextColor(Color.parseColor("#723E4A59"))
            rootView.percent3.setTextColor(Color.parseColor("#723E4A59"))
            rootView.percent4.setTextColor(Color.parseColor("#723E4A59"))

            hasPercentSelected = 0
        }

        rootView.percent2.setOnClickListener {
            curQuantity = calcAmount(0.5)
            rootView.buyAmountEdt.setText(curQuantity)

            rootView.percent1.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent2.setBackgroundResource(R.drawable.trade_button_bg)
            rootView.percent3.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent4.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent1.setTextColor(Color.parseColor("#723E4A59"))
            rootView.percent2.setTextColor(Color.parseColor("#FF007AFF"))
            rootView.percent3.setTextColor(Color.parseColor("#723E4A59"))
            rootView.percent4.setTextColor(Color.parseColor("#723E4A59"))

            hasPercentSelected = 1
        }

        rootView.percent3.setOnClickListener {
            curQuantity = calcAmount(0.75)
            rootView.buyAmountEdt.setText(curQuantity)
            rootView.percent1.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent2.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent3.setBackgroundResource(R.drawable.trade_button_bg)
            rootView.percent4.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent1.setTextColor(Color.parseColor("#723E4A59"))
            rootView.percent2.setTextColor(Color.parseColor("#723E4A59"))
            rootView.percent3.setTextColor(Color.parseColor("#FF007AFF"))
            rootView.percent4.setTextColor(Color.parseColor("#723E4A59"))

            hasPercentSelected = 2
        }

        rootView.percent4.setOnClickListener {
            curQuantity = calcAmount(1.0)
            rootView.buyAmountEdt.setText(curQuantity)
            rootView.percent1.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent2.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent3.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent4.setBackgroundResource(R.drawable.trade_button_bg)
            rootView.percent1.setTextColor(Color.parseColor("#723E4A59"))
            rootView.percent2.setTextColor(Color.parseColor("#723E4A59"))
            rootView.percent3.setTextColor(Color.parseColor("#723E4A59"))
            rootView.percent4.setTextColor(Color.parseColor("#FF007AFF"))
            hasPercentSelected = 3
        }

    }

    private fun calcAmount(rate: Double): String {
        val amountPrecision = TickerStatisticsCenter.symbolMap[symbolPair!!]?.quantityPrecision ?: 0

        return calcCanbuy()?.multiply(rate.toBigDecimal())
            ?.setScale(amountPrecision, RoundingMode.DOWN)
            ?.toPlainString() ?: "--"
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

    fun switchPair() {
        if (fragment.isAdded) {
            return
        }

        val ft = activity?.supportFragmentManager?.beginTransaction()
        ft?.add(R.id.rootContainer, fragment, "SwitchExchangeFragment")
        ft?.addToBackStack(null)
        ft?.commit()
    }

    fun onExchangeMarketItemSwitched(symbol: String) {
//        lastSwitchMarketHandler?.complete(JsBridgeResp(data = mapOf("symbol" to symbol)))
//        lastSwitchMarketHandler = null
        refreshNewPair(symbol)
    }

    @SuppressLint("CheckResult")
    private fun refreshNewPair(symbol: String) {
        val hasChange = symbolPair != symbol
        curPrice = ""
        curQuantity = ""
        symbolPair?.let {
            ExchangeWsHolder.unsubDepth(it)
        }
        ExchangeWsHolder.subDepth(symbol)
        ExchangeWsHolder.subOrder(currentAccount.nowViteAddress()!!)

        placeOrderInfo = null

        symbolPair = symbol

        tradeTokenSymbol = symbolPair?.substring(0, symbolPair?.indexOf("_") ?: 0)
        quoteTokenSymbol =
            symbolPair?.substring(
                (symbolPair?.indexOf("_") ?: 0) + 1,
                symbolPair?.length ?: 1
            )

        quoteTokenSymbol = when (quoteTokenSymbol) {
            "BTC" -> "BTC-000"
            "ETH" -> "ETH-000"
            "USDT" -> "USDT-000"
            else -> quoteTokenSymbol
        }

        tradeTokenInfo = TokenInfoCenter.findTokenInfo { it.uniqueName() == tradeTokenSymbol }
        quoteTokenInfo = TokenInfoCenter.findTokenInfo { it.uniqueName() == quoteTokenSymbol }


        VitexApi.getDepth(symbolPair!!, limit = 100).subscribe({
            DepthListCenter.symbolMap[symbolPair!!] = it
            refreshDepth(it)
        }, {
            loge(it)
            logt("xirtam VitexApi.getDepth error ${it.message}")
        })

        VitexApi.getOpenOrders(
            AccountCenter.currentViteAddress()!!,
            tradeTokenSymbol!!,
            quoteTokenSymbol!!,
            null,
            0,
            100
        ).subscribe({
            myOrders = it.order ?: mutableListOf()
            OrderCenter.symbolMap[symbolPair!!] = it.order ?: mutableListOf()
            if (it.order?.size == 0) {
                rootView.emptyGroup.visibility = View.VISIBLE
                rootView.openOrders.visibility = View.GONE
            } else {
                rootView.emptyGroup.visibility = View.GONE
                rootView.openOrders.visibility = View.VISIBLE
            }

            val adapter = OpenOrderListAdapter(it.order ?: mutableListOf(), this)
            rootView.openOrders.adapter = adapter
            refreshUI()
        }, {
            loge(it)
            logt("xirtam VitexApi.getOpenOrders error ${it.message}")
        })

        VitexApi.get24HPriceChangeBySymbols(mutableListOf(symbolPair!!)).subscribe({
            it.forEach {
                TickerStatisticsCenter.updateMaps(it)
            }

            val pricePrecision = TickerStatisticsCenter.symbolMap[symbolPair!!]?.pricePrecision ?: 0
            val priceFilter = DecimalDigitsInputFilter(pricePrecision)
            rootView.buyPriceEdt.filters = arrayOf(priceFilter)

            val amountPrecision =
                TickerStatisticsCenter.symbolMap[symbolPair!!]?.quantityPrecision ?: 0
            val amountFilter = DecimalDigitsInputFilter(amountPrecision)
            rootView.buyAmountEdt.filters = arrayOf(amountFilter)

            val miningRateData = TickerStatisticsCenter.getMiningRate(symbol)
            if (TickerStatisticsCenter.isOrderAndTradeMiningPair(symbol)) {
                rootView.miningImg.visibility = View.VISIBLE
                rootView.miningImg.setImageResource(if (miningRateData == null) R.mipmap.mining_all_icon_big else R.mipmap.mining_all_icon_small)
            } else if (TickerStatisticsCenter.isOrderMiningPair(symbol)) {
                rootView.miningImg.visibility = View.VISIBLE
                rootView.miningImg.setImageResource(if (miningRateData == null) R.mipmap.mining_order_icon_big else R.mipmap.mining_order_icon_small)
            } else if (TickerStatisticsCenter.isTradeMiningPair(symbol)) {
                rootView.miningImg.visibility = View.VISIBLE
                rootView.miningImg.setImageResource(R.mipmap.mining_trade_icon_big)
            } else {
                rootView.miningImg.visibility = View.GONE
            }

            refreshTickers(it)
            it.forEach {
                if (it.symbol == symbolPair) {
                    rootView.buyPriceEdt.setText(it.closePrice)
                }

                val rate = TickerStatisticsCenter.tokenRateCacheMap[quoteTokenSymbol!!]?.getRate()

                var closePriceDecimalCurrencyFormat =
                    it.closePrice.toBigDecimalOrNull()?.multiply(rate)?.toLocalReadableText(6, true)
                        ?: "0"
                if (closePriceDecimalCurrencyFormat == "0") {
                    closePriceDecimalCurrencyFormat = "0.00"
                }
                currentPriceValue.text =
                    "≈" + ViteConfig.get().currentCurrencySymbol() + closePriceDecimalCurrencyFormat
            }

            VitexApi.getTokenExchangeRate(listOf("VITE", "BTC-000", "ETH-000", "USDT-000"))
                .subscribe({
                    it.forEach {
                        TickerStatisticsCenter.tokenRateCacheMap[it.tokenSymbol] = it
                    }
                }, {
                    loge(it)
                    logt("xirtam VitexApi.getTokenExchangeRate error ${it.message}")
                })

            if (it.isNotEmpty()) {
                VitexApi.getOperatorTradepair(
                    tradeToken = it[0].tradeToken,
                    quoteToken = it[0].quoteToken
                ).subscribe({ pair ->

                    if (pair.operatorInfo == null) {
                        rootView.operatorImg.setImageResource(R.mipmap.anonymous_operator)
                    } else {
                        Glide.with(this).load(pair.operatorInfo?.icon).into(rootView.operatorImg)
                    }

                    rootView.internal_transfer.setOnClickListener {
                        InternalTransferActivity.show(
                            activity as Context,
                            if (isSell) pair.tradeTokenDetail?.tokenId!! else pair.quoteTokenDetail?.tokenId!!
                        )
                    }

                    tradeTokenInfo =
                        TokenInfoCenter.getTokenInfoIncacheByTokenAddr(pair.tradeTokenDetail?.tokenId!!)
                    quoteTokenInfo =
                        TokenInfoCenter.getTokenInfoIncacheByTokenAddr(pair.quoteTokenDetail?.tokenId!!)

                    TokenInfoCenter.queryViteToken(pair.tradeTokenDetail?.tokenId!!).subscribe({
                        tradeTokenInfo = it
                        getPlaceOrderInfo(
                            tradeTokenInfo?.tokenAddress,
                            quoteTokenInfo?.tokenAddress
                        )
                    }, {
                        loge(it)
                    })

                    TokenInfoCenter.queryViteToken(pair.quoteTokenDetail?.tokenId!!).subscribe({
                        quoteTokenInfo = it
                        getPlaceOrderInfo(
                            tradeTokenInfo?.tokenAddress,
                            quoteTokenInfo?.tokenAddress
                        )
                    }, {
                        loge(it)
                    })

                    refreshBalance()

                    if (pair.operatorInfo?.level == null || pair.operatorInfo?.level == 0) {
                        if (hasChange or mustShowWarning) {
                            showWarning()
                        }
                    }

                }, {
                    logt("xirtam getOperatorTradepair error ${it.message}")
                    loge(it)
                })
            }

            refreshUI()
        }, {
            logt("xirtam vitex get24HPriceChangeBySymbols error ${it.message}")
            loge(it)
        })

        refreshUI()

        TickerStatisticsCenter.getMiningRate(symbolPair!!).let {
            if (it == null) {
                rootView.miningRate.visibility = View.GONE
            } else {
                rootView.miningRate.visibility = View.VISIBLE
                rootView.miningRate.text = "$it"
            }
        }

        rootView.favImg.setImageResource(if (isFavPair(symbolPair!!)) R.mipmap.kline_favourite else R.mipmap.kline_unfavourite)

        refreshVipState()

        rootView.buyCoinTxt.setOnClickListener {
            val price = rootView.buyPriceEdt.editableText.toString()
            val quantity = rootView.buyAmountEdt.editableText.toString()
            if (checkMinMaxAmount(price, quantity)) {
                doSend()
            }
        }

        rootView.sellCoinTxt.setOnClickListener {
            val price = rootView.buyPriceEdt.editableText.toString()
            val quantity = rootView.buyAmountEdt.editableText.toString()
            if (checkMinMaxAmount(price, quantity)) {
                doSend()
            }
        }

        rootView.buyPriceEdt.setOnFocusChangeListener { view: View, hasFocus ->
            if (hasFocus) {
                prepareShowMiningNotice()
            } else {
                dismissMiningNotice()
            }

        }
        rootView.buyPriceEdt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    val rate =
                        TickerStatisticsCenter.tokenRateCacheMap[quoteTokenSymbol!!]?.getRate()
                            ?: return

                    var closePriceDecimalCurrencyFormat =
                        s.toString().toBigDecimalOrNull()?.multiply(rate)
                            ?.toLocalReadableText(6, true)
                    if (closePriceDecimalCurrencyFormat == "0") {
                        closePriceDecimalCurrencyFormat = "0.00"
                    }
                    buyValueTxt.text = "≈" + ViteConfig.get()
                        .currentCurrencySymbol() + closePriceDecimalCurrencyFormat

                    val price = s.toString()
                    curPrice = price
                    if (curQuantity.isNotEmpty()) {
                        checkMinMaxAmount(price, curQuantity)
                    }
                }

                resetPercentage()
                refreshInputPriceAndQuantity()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {


            }
        })

        rootView.buyAmountEdt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    val quantity = s.toString()
                    curQuantity = quantity

                    if (curPrice.isNotEmpty()) {
                        if (quantity.isNotEmpty()) {
                            checkMinMaxAmount(curPrice, quantity)
                        }
                    }
                }
                resetPercentage()
                refreshInputPriceAndQuantity()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        rootView.buyAmountEdt.setText("")

        refreshBuyButton()
    }

    private fun refreshBuyButton() {
        if (SpecialMarketCenter.isStopped(symbolPair!!)) {
            rootView.sellCoinTxt.visibility = View.GONE
            rootView.buyCoinTxt.visibility = View.GONE
            rootView.tradeSuspended.visibility = View.VISIBLE
        } else {
            if (isSell) {
                rootView.buySellRadioGroup.check(R.id.radioButtonSell)
                rootView.sellCoinTxt.visibility = View.VISIBLE
                rootView.buyCoinTxt.visibility = View.GONE
                rootView.tradeSuspended.visibility = View.GONE
                rootView.buyPriceEdt.hint = getString(R.string.sellPrice)
                rootView.buyAmountEdt.hint = getString(R.string.sellAmount)
            } else {
                rootView.buySellRadioGroup.check(R.id.radioButtonBuy)
                rootView.sellCoinTxt.visibility = View.GONE
                rootView.buyCoinTxt.visibility = View.VISIBLE
                rootView.tradeSuspended.visibility = View.GONE
                rootView.buyPriceEdt.hint = getString(R.string.buyPrice)
                rootView.buyAmountEdt.hint = getString(R.string.buyAmount)
            }
        }
    }

    private fun prepareShowMiningNotice() {
        val pricePrecision =
            TickerStatisticsCenter.symbolMap[symbolPair!!]?.pricePrecision ?: 0
        if (TickerStatisticsCenter.getMiningOrderMiningSetting(symbolPair!!)?.sellRangeMax == "0" || TickerStatisticsCenter.getMiningOrderMiningSetting(
                symbolPair!!
            )?.buyRangeMax == "0"
        ) {
            return
        }

        val buyRangeMax =
            TickerStatisticsCenter.getMiningOrderMiningSetting(symbolPair!!)?.buyRangeMax?.toDoubleOrNull()
                ?: return
        val sellRangeMax =
            TickerStatisticsCenter.getMiningOrderMiningSetting(symbolPair!!)?.sellRangeMax?.toDoubleOrNull()
                ?: return

        val buy1Price =
            if (rootView.buyPrice0.visibility == View.VISIBLE) rootView.buyPrice0.text.toString()
                .toDoubleOrNull() ?: return else 0.0
        val sell1Price =
            if (rootView.sellPrice5.visibility == View.VISIBLE) rootView.sellPrice5.text.toString()
                .toDoubleOrNull() ?: return else 0.0

        if (isSell) {
            showMiningNotice(
                isSell,
                (buy1Price + sellRangeMax * buy1Price).toBigDecimal()
                    .add(BigDecimal.ONE.divide(BigDecimal.TEN.pow(pricePrecision + 1)))
                    .setScale(pricePrecision, RoundingMode.UP)
                    .toPlainString()
            )
        } else {
            showMiningNotice(
                isSell,
                (sell1Price - buyRangeMax * sell1Price).toBigDecimal()
                    .minus(BigDecimal.ONE.divide(BigDecimal.TEN.pow(pricePrecision + 1)))
                    .setScale(pricePrecision, RoundingMode.DOWN)
                    .toPlainString()
            )
        }
    }

    private fun refreshTickers(it: List<TickerStatistics>) {
        if (it.isNotEmpty()) {
            it.forEach { ticker ->
                if (ticker.symbol == symbolPair) {
                    rootView.textView40.text =
                        if (ticker.priceChangePercent < 0)
                            "${String.format("%.2f", ticker.priceChangePercent * 100)}%"
                        else
                            "+${String.format("%.2f", ticker.priceChangePercent * 100)}%"

                    rootView.textView40.setTextColor(
                        if (ticker.priceChange.startsWith("-")) Color.parseColor("#E5494D") else Color.parseColor(
                            "#01D764"
                        )
                    )

                    rootView.currentPrice.text = ticker.closePrice
                    rootView.currentPrice.setTextColor(
                        if (ticker.priceChangePercent < 0) Color.parseColor("#E5494D")
                        else if (ticker.priceChangePercent > 0) Color.parseColor("#01D764")
                        else Color.parseColor("#24272B")
                    )

                    val rate =
                        TickerStatisticsCenter.tokenRateCacheMap[quoteTokenSymbol!!]?.getRate()
                            ?: BigDecimal.ZERO

                    var closePriceDecimalCurrencyFormat =
                        ticker.closePrice.toBigDecimalOrNull()?.multiply(rate)
                            ?.toLocalReadableText(6, true)
                            ?: "0"
                    if (closePriceDecimalCurrencyFormat == "0") {
                        closePriceDecimalCurrencyFormat = "0.00"
                    }
                    rootView.currentPriceValue.text = "≈" + ViteConfig.get()
                        .currentCurrencySymbol() + closePriceDecimalCurrencyFormat
                }
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        isPause = hidden
        if (hidden) {
            if (!fragment.isHidden) {
                val ft = activity?.supportFragmentManager?.beginTransaction()
                ft?.remove(fragment)
                ft?.commit()
            }
        }
    }

    fun showMiningNotice(isSell: Boolean, priceString: String) {
        rootView.miningNotice.visibility = View.VISIBLE
        rootView.miningNotice.text =
            activity?.getString(
                if (isSell) R.string.mining_notice_sell else R.string.mining_notice_buy, priceString
            )
    }

    fun dismissMiningNotice() {
        rootView.miningNotice.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        symbolPair?.let { pair ->
            ExchangeWsHolder.unsubDepth(pair)
        }

        currentAccount.nowViteAddress()?.let {
            ExchangeWsHolder.unsubOrder(it)
        }
    }

    override fun onResume() {
        super.onResume()
        doResume()
    }

    private fun doResume() {
        refreshNewPair(symbolPair!!)

        symbolPair?.let { pair ->
            ExchangeWsHolder.subDepth(pair)
        }
        ExchangeWsHolder.subOrder(currentAccount.nowViteAddress()!!)

        ViteTokenInfo.tokenId?.let { tokenId ->
            AsyncNet.getAccountFundInfo(AccountCenter.currentViteAddress()!!, "").applyIoScheduler()
                .subscribe({
                    val obj = Gson().fromJson<Map<String, GetAccountFundInfoResp>?>(
                        it.resp,
                        object : TypeToken<Map<String, GetAccountFundInfoResp>>() {}.type
                    )
                    viteBalance = obj?.get(tokenId)?.getAvailableBaseText() ?: "0"
                }, {
                    loge(it)
                })
        }
    }

    private fun checkMinMaxAmount(price: String, quantity: String): Boolean {
        if (isPause) {
            return false
        }

        if (price == "" || quantity == "") {
            return false
        }

        val tradeMaxValue = tradeAccountFundInfo?.getAvailableBase() ?: BigDecimal.ZERO
        val quoteMaxValue = quoteAccountFundInfo?.getAvailableBase() ?: BigDecimal.ZERO

        if (price.toBigDecimalOrNull() == null || quantity.toBigDecimalOrNull() == null) {
            return false
        }

        var minmount = placeOrderInfo?.minTradeAmount?.toBigDecimalOrNull()?.divide(
            BigDecimal.TEN.pow(quoteTokenInfo?.decimal ?: 1)
        ) ?: BigDecimal.ZERO

        if (isSell) {
            if (tradeMaxValue < quantity.toBigDecimal()) {
                activity?.showToast(R.string.exchange_balance_not_enough)
                return false
            }

            if (minmount > quantity.toBigDecimal().multiply(price.toBigDecimal())) {
                activity?.showToast(
                    activity!!.getString(
                        R.string.exchange_balance_lower_than_min,
                        minmount,
                        quoteTokenSymbol
                    )
                )
                return false
            }
        } else {
            if (quoteMaxValue < calcDealAmount()) {
                activity?.showToast(R.string.exchange_balance_not_enough)
                return false
            }

            if (minmount > calcDealAmount()) {
                activity?.showToast(
                    activity!!.getString(
                        R.string.exchange_balance_lower_than_min,
                        minmount,
                        quoteTokenSymbol
                    )
                )
                return false
            }
        }
        return true
    }

    var refreshVipStateDisposable: Disposable? = null
    private fun refreshVipState() {
        refreshVipStateDisposable?.dispose()
        refreshVipStateDisposable =
            AsyncNet.dexHasStackedForVIP(currentAccount.nowViteAddress()!!).applyIoScheduler()
                .subscribe({
                    val isVip = it.resp?.toBoolean() == true
                    rootView.openVip.text =
                        if (isVip) getString(R.string.cancelVip) else getString(R.string.openVip)
                    rootView.vipIconImg.setImageResource(if (isVip) R.mipmap.vip_icon else R.mipmap.vip_icon_grey)

                    rootView.openVip.setOnClickListener {
                        if (isVip) {
                            cancelVip()
                        } else {
                            openVip()
                        }
                    }
                }, {
                    loge(it)
                })
    }

    fun hasSamePrice(price: String?): Boolean {
        return (!price.isNullOrEmpty()) && myOrders?.find {
            it.price == price
        } != null
    }

    private fun showWarning() {
        if (!this.isHidden) {
            mustShowWarning = false
            with(TextTitleNotifyDialog(activity as Activity)) {
                setTitle(R.string.warm_notice)
                setMessage(
                    (activity as Activity).getString(
                        R.string.trade_warning,
                        symbolPair?.replace("_", "/")
                    )
                )
                setBottom(R.string.close) { dialog ->
                    dialog.dismiss()
                }
                show()
            }
        }
    }

    private fun calcDealAmount(): BigDecimal {
        val price: BigDecimal? = buyPriceEdt.text.toString().toBigDecimalOrNull()
        val quantity: BigDecimal? = buyAmountEdt.text.toString().toBigDecimalOrNull()
        if (price == null || quantity == null)
            return BigDecimal.ZERO
        return price.multiply(quantity)?.multiply(BigDecimal.ONE.plus(getFee())) ?: BigDecimal.ZERO
    }

    private fun calcCanbuy(): BigDecimal? {
        val price: BigDecimal = curPrice.toBigDecimalOrNull()
            ?: return BigDecimal.ZERO
        return if (isSell) {
            tradeAccountFundInfo?.getAvailableBase() ?: BigDecimal.ZERO
        } else {
            val value = price.multiply(BigDecimal.ONE.plus(getFee()))

            if (BigDecimal.ZERO.compareTo(value) == 0) {
                null
            } else {
                quoteAccountFundInfo?.getAvailableBase()?.divide(
                    value,
                    30,
                    BigDecimal.ROUND_HALF_EVEN
                ) ?: BigDecimal.ZERO
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshInputPriceAndQuantity() {
        val pricePrecision =
            TickerStatisticsCenter.symbolMap[symbolPair!!]?.pricePrecision ?: 0

        if (buyPriceEdt == null || buyAmountEdt == null)
            return
        if (buyPriceEdt.text.isNotEmpty() && buyAmountEdt.text.isNotEmpty()) {

            availableCoin.text =
                getString(R.string.deal_amount) +
                        calcDealAmount().setScale(
                            pricePrecision,
                            BigDecimal.ROUND_HALF_DOWN
                        ).toPlainString() + " ${"".symbolRemoveIndex(quoteTokenSymbol)}"

            canbuyCoin.text =
                "${getString(R.string.available)}${
                    (if (isSell) tradeAccountFundInfo else quoteAccountFundInfo)?.getAvailableBase()
                        ?.toLocalReadableText(
                            8
                        )
                        ?: "0"
                } ${
                    if (isSell) "".symbolRemoveIndex(tradeTokenSymbol) else "".symbolRemoveIndex(
                        quoteTokenSymbol
                    )
                }"
        } else {
            if (buyPriceEdt.text.isEmpty()) {
                availableCoin.text =
                    "${getString(if (isSell) R.string.can_sell else R.string.can_buy)}--"
                canbuyCoin.text =
                    "${getString(R.string.available)}${
                        (if (isSell) tradeAccountFundInfo else quoteAccountFundInfo)?.getAvailableBase()
                            ?.toLocalReadableText(
                                8
                            )
                            ?: "0"
                    } ${
                        if (isSell) "".symbolRemoveIndex(tradeTokenSymbol) else "".symbolRemoveIndex(
                            quoteTokenSymbol
                        )
                    }"
            } else {
                if (isSell) {
                    val price: BigDecimal? = buyPriceEdt.text.toString().toBigDecimalOrNull()
                    val value = calcCanbuy()?.multiply(price)
                        ?.setScale(pricePrecision, BigDecimal.ROUND_HALF_DOWN)?.toPlainString()
                        ?: "--"
                    availableCoin.text =
                        "${getString(R.string.can_sell)}" + value + " ${
                            "".symbolRemoveIndex(
                                quoteTokenSymbol
                            )
                        }"
                } else {
                    availableCoin.text =
                        "${getString(R.string.can_buy)}${
                            calcCanbuy()?.setScale(
                                pricePrecision,
                                BigDecimal.ROUND_HALF_DOWN
                            )?.toPlainString()
                                ?: "--"
                        } ${"".symbolRemoveIndex(tradeTokenSymbol)}"
                }

                canbuyCoin.text =
                    "${getString(R.string.available)}${
                        (if (isSell) tradeAccountFundInfo else quoteAccountFundInfo)?.getAvailableBase()
                            ?.toLocalReadableText(
                                8
                            )
                            ?: "0"
                    } ${
                        if (isSell) "".symbolRemoveIndex(tradeTokenSymbol) else "".symbolRemoveIndex(
                            quoteTokenSymbol
                        )
                    }"
            }
        }

    }


    private fun resetPercentage() {
        if (hasPercentSelected >= 0) {
            hasPercentSelected = -1
            rootView.percent1.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent1.setTextColor(Color.parseColor("#733E4A59"))
            rootView.percent2.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent2.setTextColor(Color.parseColor("#733E4A59"))
            rootView.percent3.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent3.setTextColor(Color.parseColor("#733E4A59"))
            rootView.percent4.setBackgroundResource(R.drawable.trade_button_bg_grey)
            rootView.percent4.setTextColor(Color.parseColor("#733E4A59"))
        }
    }

    var getAccountFundInfoDisposable: Disposable? = null
    private fun refreshBalance() {
        tradeAccountFundInfo =
            DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                ?.get(tradeTokenInfo?.tokenAddress ?: "")

        tradeBlanace =
            (tradeAccountFundInfo?.getAvailableBase()?.toLocalReadableText(8)
                ?: "0").toDouble()

        quoteAccountFundInfo =
            DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                ?.get(quoteTokenInfo?.tokenAddress ?: "")
        quoteBlanace =
            (quoteAccountFundInfo?.getAvailableBase()?.toLocalReadableText(8)
                ?: "0").toDouble()

        getAccountFundInfoDisposable?.dispose()
        getAccountFundInfoDisposable =
            AsyncNet.getAccountFundInfo(AccountCenter.currentViteAddress()!!, "").applyIoScheduler()
                .subscribe({
                    val obj = Gson().fromJson<Map<String, GetAccountFundInfoResp>?>(
                        it.resp,
                        object : TypeToken<Map<String, GetAccountFundInfoResp>>() {}.type
                    )
                    viteBalance = obj?.get(ViteTokenInfo.tokenId!!)?.getAvailableBaseText() ?: "0"
                }, {
                    loge(it)
                })

        refreshInputPriceAndQuantity()
    }

    @SuppressLint("CheckResult")
    private fun getPlaceOrderInfo(tradeTokenId: String?, quoteTokenId: String?) {
        if (tradeTokenId == null || quoteTokenId == null)
            return

        AsyncNet.dexGetPlaceOrderInfo(
            currentAccount.nowViteAddress()!!,
            tradeTokenId,
            quoteTokenId,
            isSell
        ).applyIoScheduler().subscribe({
            if (it.resp == null) {
                logi("getPlaceOrderInfo error $it")
            }
            placeOrderInfo = Gson().fromJson(it.resp, PlaceOrderInfo::class.java)
        }, {
            loge(it)
        })


    }

    @SuppressLint("CheckResult")
    private fun cancelVip() {
        if (!activity?.hasNetWork()!!) {
            activity?.showToast(R.string.net_work_error)
            return
        }

        ViteAccountInfoPoll.dexGetVIPStakeInfoList(
            currentAccount.nowViteAddress()!!,
            0, 100
        ).applyIoScheduler().subscribe({
            it.resp?.let { resp ->
                resp.stakeList?.find { it.bid == 2 }?.let { bidStackList ->
                    if (bidStackList.expirationTime == null || System.currentTimeMillis() / 1000 < bidStackList.expirationTime) {
                        activity?.showToast(R.string.cant_cancel_vip)
                    } else {
                        if (bidStackList.id == null) {
                            cancelVipByActionType()
                        } else {
                            cancelVipById(bidStackList.id)
                        }
                    }
                }
            }
        }, {
            loge(it)
        })
    }

    private fun cancelVipByActionType() {
        tradeTokenInfo?.tokenAddress?.let { tokenId ->
            lastSendParams = NormalTxParams(
                toAddr = DexContractAddress,
                amountInSu = BigInteger.ZERO,
                data = BuildInContractEncoder.stakeForVIP(2),
                tokenId = tokenId,
                accountAddr = currentAccount.nowViteAddress()!!
            )

            verifyIdentityDirectlyFinger(getString(R.string.cancelVip), {
                lastSendParams?.let {
                    sendTx(it)
                    tryRefreshVipStateCount = 10
                }
            }, {})
        }
    }

    private fun cancelVipById(id: String) {
        tradeTokenInfo?.tokenAddress?.let { tokenId ->
            lastSendParams = NormalTxParams(
                toAddr = DexContractAddress,
                amountInSu = BigInteger.ZERO,
                data = BuildInContractEncoder.cancelStakeById(id.hexToBytes()),
                tokenId = tokenId,
                accountAddr = currentAccount.nowViteAddress()!!
            )

            verifyIdentityDirectlyFinger(getString(R.string.cancelVip), {
                lastSendParams?.let {
                    sendTx(it)
                    tryRefreshVipStateCount = 10
                }
            }, {})
        }
    }

    private fun getFee(): BigDecimal {
        return if (isSell) BigDecimal.ZERO else ((placeOrderInfo?.feeRate
            ?: -1) / 100000.0).toBigDecimal()
    }

    private fun openVip() {
        if (!activity?.hasNetWork()!!) {
            activity?.showToast(R.string.net_work_error)
            return
        }

        tradeTokenInfo?.tokenAddress?.let { tokenId ->
            lastSendParams = NormalTxParams(
                toAddr = DexContractAddress,
                amountInSu = BigInteger.ZERO,
                data = BuildInContractEncoder.stakeForVIP(1),
                tokenId = tokenId,
                accountAddr = currentAccount.nowViteAddress()!!
            )

            val dialogParams = BigDialog.Params(
                bigTitle = getString(R.string.openVip),
                stakeAmount = "10000",
                stakeSymbol = "VITE",
                inviteCostHint = activity!!.getString(R.string.open_vip_notice),
                balance = viteBalance,
                balanceSymbol = "VITE",
                firstButtonTxt = getString(R.string.confirm_pay)
            )

            verifyIdentity(dialogParams, {
                lastSendParams?.let {
                    if (viteBalance.toDoubleOrNull() == null || viteBalance.toDouble() < 10000) {
                        activity?.showToast(R.string.vip_amount_not_enough)
                    } else {
                        sendTx(it)
                        tryRefreshVipStateCount = 10
                    }

                }
            }, {})
        }
    }

    private fun resetDepth() {
        val bidsBg = mutableListOf<View>()
        bidsBg.add(rootView.depthBuyBg0)
        bidsBg.add(rootView.depthBuyBg1)
        bidsBg.add(rootView.depthBuyBg2)
        bidsBg.add(rootView.depthBuyBg3)
        bidsBg.add(rootView.depthBuyBg4)
        bidsBg.add(rootView.depthBuyBg5)

        val bidsPriceTxt = mutableListOf<TextView>()
        bidsPriceTxt.add(rootView.buyPrice0)
        bidsPriceTxt.add(rootView.buyPrice1)
        bidsPriceTxt.add(rootView.buyPrice2)
        bidsPriceTxt.add(rootView.buyPrice3)
        bidsPriceTxt.add(rootView.buyPrice4)
        bidsPriceTxt.add(rootView.buyPrice5)

        val bidsAmountTxt = mutableListOf<TextView>()
        bidsAmountTxt.add(rootView.buyAmount0)
        bidsAmountTxt.add(rootView.buyAmount1)
        bidsAmountTxt.add(rootView.buyAmount2)
        bidsAmountTxt.add(rootView.buyAmount3)
        bidsAmountTxt.add(rootView.buyAmount4)
        bidsAmountTxt.add(rootView.buyAmount5)

        val asksBg = mutableListOf<View>()
        asksBg.add(rootView.depthSellBg0)
        asksBg.add(rootView.depthSellBg1)
        asksBg.add(rootView.depthSellBg2)
        asksBg.add(rootView.depthSellBg3)
        asksBg.add(rootView.depthSellBg4)
        asksBg.add(rootView.depthSellBg5)

        val asksPriceTxt = mutableListOf<TextView>()
        asksPriceTxt.add(rootView.sellPrice0)
        asksPriceTxt.add(rootView.sellPrice1)
        asksPriceTxt.add(rootView.sellPrice2)
        asksPriceTxt.add(rootView.sellPrice3)
        asksPriceTxt.add(rootView.sellPrice4)
        asksPriceTxt.add(rootView.sellPrice5)

        val asksAmountTxt = mutableListOf<TextView>()
        asksAmountTxt.add(rootView.sellAmount0)
        asksAmountTxt.add(rootView.sellAmount1)
        asksAmountTxt.add(rootView.sellAmount2)
        asksAmountTxt.add(rootView.sellAmount3)
        asksAmountTxt.add(rootView.sellAmount4)
        asksAmountTxt.add(rootView.sellAmount5)


        val avatars = mutableListOf<ImageView>()
        avatars.add(rootView.sellMyAvatar0)
        avatars.add(rootView.sellMyAvatar1)
        avatars.add(rootView.sellMyAvatar2)
        avatars.add(rootView.sellMyAvatar3)
        avatars.add(rootView.sellMyAvatar4)
        avatars.add(rootView.sellMyAvatar5)
        avatars.add(rootView.sellMyAvatar6)
        avatars.add(rootView.sellMyAvatar7)
        avatars.add(rootView.sellMyAvatar8)
        avatars.add(rootView.sellMyAvatar9)
        avatars.add(rootView.sellMyAvatar10)
        avatars.add(rootView.sellMyAvatar11)

        avatars.forEach {
            it.visibility = View.GONE
        }
        bidsPriceTxt.forEach {
            it.setOnClickListener(null)
            it.text = ""
        }

        asksPriceTxt.forEach {
            it.setOnClickListener(null)
            it.text = ""
        }

        asksAmountTxt.forEach {
            it.text = ""
        }

        bidsAmountTxt.forEach {
            it.text = ""
        }

        bidsBg.forEach {
            it.scaleX = 0.0f
        }

        asksBg.forEach {
            it.scaleX = 0.0f
        }

    }

    private fun refreshDepth(depthList: DepthList?) {
        depthList?.let {
            val bidsBg = mutableListOf<View>()
            bidsBg.add(rootView.depthBuyBg0)
            bidsBg.add(rootView.depthBuyBg1)
            bidsBg.add(rootView.depthBuyBg2)
            bidsBg.add(rootView.depthBuyBg3)
            bidsBg.add(rootView.depthBuyBg4)
            bidsBg.add(rootView.depthBuyBg5)

            val bidsPriceTxt = mutableListOf<TextView>()
            bidsPriceTxt.add(rootView.buyPrice0)
            bidsPriceTxt.add(rootView.buyPrice1)
            bidsPriceTxt.add(rootView.buyPrice2)
            bidsPriceTxt.add(rootView.buyPrice3)
            bidsPriceTxt.add(rootView.buyPrice4)
            bidsPriceTxt.add(rootView.buyPrice5)

            val bidsAmountTxt = mutableListOf<TextView>()
            bidsAmountTxt.add(rootView.buyAmount0)
            bidsAmountTxt.add(rootView.buyAmount1)
            bidsAmountTxt.add(rootView.buyAmount2)
            bidsAmountTxt.add(rootView.buyAmount3)
            bidsAmountTxt.add(rootView.buyAmount4)
            bidsAmountTxt.add(rootView.buyAmount5)

            val asksBg = mutableListOf<View>()
            asksBg.add(rootView.depthSellBg0)
            asksBg.add(rootView.depthSellBg1)
            asksBg.add(rootView.depthSellBg2)
            asksBg.add(rootView.depthSellBg3)
            asksBg.add(rootView.depthSellBg4)
            asksBg.add(rootView.depthSellBg5)

            val asksPriceTxt = mutableListOf<TextView>()
            asksPriceTxt.add(rootView.sellPrice0)
            asksPriceTxt.add(rootView.sellPrice1)
            asksPriceTxt.add(rootView.sellPrice2)
            asksPriceTxt.add(rootView.sellPrice3)
            asksPriceTxt.add(rootView.sellPrice4)
            asksPriceTxt.add(rootView.sellPrice5)

            val asksAmountTxt = mutableListOf<TextView>()
            asksAmountTxt.add(rootView.sellAmount0)
            asksAmountTxt.add(rootView.sellAmount1)
            asksAmountTxt.add(rootView.sellAmount2)
            asksAmountTxt.add(rootView.sellAmount3)
            asksAmountTxt.add(rootView.sellAmount4)
            asksAmountTxt.add(rootView.sellAmount5)

            val avatars = mutableListOf<ImageView>()
            avatars.add(rootView.sellMyAvatar0)
            avatars.add(rootView.sellMyAvatar1)
            avatars.add(rootView.sellMyAvatar2)
            avatars.add(rootView.sellMyAvatar3)
            avatars.add(rootView.sellMyAvatar4)
            avatars.add(rootView.sellMyAvatar5)
            avatars.add(rootView.sellMyAvatar6)
            avatars.add(rootView.sellMyAvatar7)
            avatars.add(rootView.sellMyAvatar8)
            avatars.add(rootView.sellMyAvatar9)
            avatars.add(rootView.sellMyAvatar10)
            avatars.add(rootView.sellMyAvatar11)

            bidsBg.forEach {
                it.setOnClickListener(priceListener)
            }

            val asksBgClicker = mutableListOf<View>()
            asksBgClicker.add(rootView.depthSellBg0Clicker)
            asksBgClicker.add(rootView.depthSellBg1Clicker)
            asksBgClicker.add(rootView.depthSellBg2Clicker)
            asksBgClicker.add(rootView.depthSellBg3Clicker)
            asksBgClicker.add(rootView.depthSellBg4Clicker)
            asksBgClicker.add(rootView.depthSellBg5Clicker)

            val bidsBgClicker = mutableListOf<View>()
            bidsBgClicker.add(rootView.depthBuyBg0Clicker)
            bidsBgClicker.add(rootView.depthBuyBg1Clicker)
            bidsBgClicker.add(rootView.depthBuyBg2Clicker)
            bidsBgClicker.add(rootView.depthBuyBg3Clicker)
            bidsBgClicker.add(rootView.depthBuyBg4Clicker)
            bidsBgClicker.add(rootView.depthBuyBg5Clicker)

            asksBgClicker.forEach {
                it.setOnClickListener(priceListener)
            }

            bidsBgClicker.forEach {
                it.setOnClickListener(priceListener)
            }

            bidsPriceTxt.forEach {
                it.setOnClickListener(priceListener)
            }
            bidsAmountTxt.forEach {
                it.setOnClickListener(priceListener)
            }

            asksBg.forEach {
                it.setOnClickListener(priceListener)
            }
            asksPriceTxt.forEach {
                it.setOnClickListener(priceListener)
            }
            asksAmountTxt.forEach {
                it.setOnClickListener(priceListener)
            }

            val tmpAsks = mutableListOf<Depth?>();
            for (i in 0 until 6) {
                tmpAsks.add(null)
            }
            val tmpBids = mutableListOf<Depth?>();
            for (i in 0 until 6) {
                tmpBids.add(null)
            }

            var allAsks = it.asks
            val asksLen = min(6, allAsks?.size ?: 0)
            allAsks?.sortBy { it.price.toBigDecimal() }
            var asks = allAsks?.subList(0, asksLen)

            var biggestQuantity = .0f
            for (i in 0 until asksLen) {
                //Sell
                biggestQuantity =
                    Math.max(asks?.get(i)?.quantity?.toFloat() ?: 0f, biggestQuantity)
            }

            for (i in 0 until asksLen) {
                asksBg[5 - i].scaleX =
                    (asks?.get(i)?.quantity?.toFloat() ?: 0f) / biggestQuantity
                asksBg[5 - i].pivotX = asksBg[i].width + 0.0f
                asksPriceTxt[5 - i].text = asks?.get(i)?.price
                asksAmountTxt[5 - i].text = asks?.get(i)?.quantityView()
                avatars[5 - i].visibility =
                    if (hasSamePrice(asks?.get(i)?.price)) View.VISIBLE else View.GONE
                tmpAsks[5 - i] = asks?.get(i)
            }
            depthAsks = tmpAsks


            var allBids = it.bids
            val bidsLen = min(6, allBids?.size ?: 0)
            var bids = allBids?.subList(0, bidsLen)
            for (i in 0 until bidsLen) {
                //Sell
                biggestQuantity =
                    Math.max(bids?.get(i)?.quantity?.toFloat() ?: 0f, biggestQuantity)
            }

            for (i in 0 until bidsLen) {
                bidsBg[i].scaleX =
                    (bids?.get(i)?.quantity?.toFloat() ?: 0f) / biggestQuantity
                bidsBg[i].pivotX = bidsBg[i].width + 0.0f
                bidsPriceTxt[i].text = bids?.get(i)?.price
                bidsAmountTxt[i].text = bids?.get(i)?.quantityView()
                avatars[6 + i].visibility =
                    if (hasSamePrice(bids?.get(i)?.price)) View.VISIBLE else View.GONE
                tmpBids[i] = bids?.get(i)
            }

            depthBids = tmpBids
        }
    }

    val priceListener = View.OnClickListener {
        var price = ""
        var quantity = ""
        curPrice = ""
        curQuantity = ""

        when (it.id) {
            R.id.depthBuyBg0, R.id.depthBuyBg0Clicker, R.id.buyPrice0, R.id.buyAmount0 -> {
                curPrice = rootView.buyPrice0.text.toString()
                price = curPrice
                if (isSell) {
                    quantity = getAllQuantity(false, 0).toString()
                }
            }
            R.id.depthBuyBg1Clicker, R.id.depthBuyBg1, R.id.buyPrice1, R.id.buyAmount1 -> {
                curPrice = rootView.buyPrice1.text.toString()
                price = curPrice
                if (isSell) {
                    quantity = getAllQuantity(false, 1).toString()
                }
            }

            R.id.depthBuyBg2Clicker, R.id.depthBuyBg2, R.id.buyPrice2, R.id.buyAmount2 -> {
                curPrice = rootView.buyPrice2.text.toString()
                price = curPrice
                if (isSell) {
                    quantity = getAllQuantity(false, 2).toString()
                }
            }
            R.id.depthBuyBg3Clicker, R.id.depthBuyBg3, R.id.buyPrice3, R.id.buyAmount3 -> {
                curPrice = rootView.buyPrice3.text.toString()
                price = curPrice
                if (isSell) {
                    quantity = getAllQuantity(false, 3).toString()
                }
            }
            R.id.depthBuyBg4Clicker, R.id.depthBuyBg4, R.id.buyPrice4, R.id.buyAmount4 -> {
                curPrice = rootView.buyPrice4.text.toString()
                price = curPrice
                if (isSell) {
                    quantity = getAllQuantity(false, 4).toString()
                }
            }
            R.id.depthBuyBg5Clicker, R.id.depthBuyBg5, R.id.buyPrice5, R.id.buyAmount5 -> {
                curPrice = rootView.buyPrice5.text.toString()
                price = curPrice
                if (isSell) {
                    quantity = getAllQuantity(false, 5).toString()
                }
            }

            R.id.depthSellBg0, R.id.depthSellBg0Clicker, R.id.sellPrice0, R.id.sellAmount0 -> {
                curPrice = rootView.sellPrice0.text.toString()
                price = curPrice
                if (!isSell) {
                    quantity = getAllQuantity(true, 0).toString()
                }
            }
            R.id.depthSellBg1, R.id.depthSellBg1Clicker, R.id.sellPrice1, R.id.sellAmount1 -> {
                curPrice = rootView.sellPrice1.text.toString()
                price = curPrice
                if (!isSell) {
                    quantity = getAllQuantity(true, 1).toString()
                }
            }
            R.id.depthSellBg2, R.id.depthSellBg2Clicker, R.id.sellPrice2, R.id.sellAmount2 -> {
                curPrice = rootView.sellPrice2.text.toString()
                price = curPrice
                if (!isSell) {
                    quantity = getAllQuantity(true, 2).toString()
                }
            }
            R.id.depthSellBg3, R.id.depthSellBg3Clicker, R.id.sellPrice3, R.id.sellAmount3 -> {
                curPrice = rootView.sellPrice3.text.toString()
                price = curPrice
                if (!isSell) {
                    quantity = getAllQuantity(true, 3).toString()
                }
            }
            R.id.depthSellBg4, R.id.depthSellBg4Clicker, R.id.sellPrice4, R.id.sellAmount4 -> {
                curPrice = rootView.sellPrice4.text.toString()
                price = curPrice
                if (!isSell) {
                    quantity = getAllQuantity(true, 4).toString()
                }
            }
            R.id.depthSellBg5, R.id.depthSellBg5Clicker, R.id.sellPrice5, R.id.sellAmount5 -> {
                curPrice = rootView.sellPrice5.text.toString()
                price = curPrice
                if (!isSell) {
                    quantity = getAllQuantity(true, 5).toString()
                }
            }
        }

        val pricePrecision = TickerStatisticsCenter.symbolMap[symbolPair!!]?.pricePrecision ?: 0
        val amountPrecision = TickerStatisticsCenter.symbolMap[symbolPair!!]?.quantityPrecision ?: 0

        if (price.isNotEmpty()) {
            rootView.buyPriceEdt.setText(price)
        }

        if (quantity.isNotEmpty()) {
            curQuantity = String.format("%.${amountPrecision}f", quantity.toDouble())
            rootView.buyAmountEdt.setText(curQuantity)
        }
    }

    private fun getAllQuantity(isSell: Boolean, index: Int): Double {
        return if (isSell) {
            var all = 0.0
            for (i in index until Math.min(depthAsks.size, 6)) {
                all += depthAsks[i]?.quantity?.toDoubleOrNull() ?: 0.0
            }
            Math.min(all, calcAmount(1.0).toDouble())
        } else {
            var all = 0.0
            for (i in 0 until Math.min(index + 1, depthBids.size)) {
                all += depthBids[i]?.quantity?.toDoubleOrNull() ?: 0.0
            }
            Math.min(all, calcAmount(1.0).toDouble())
        }
    }

    var currentOrderMode = TickerStatistics.defaultOrderFunc
        set(value) {
            field = value
            refreshUI()
        }

    var disposable: Disposable? = null
    private fun startPoll() {
        DexAccountFundInfoPoll.start()

        disposable?.dispose()
        disposable = Observable.fromCallable { 1 }
            .repeatWhen { it.delay(2, TimeUnit.SECONDS) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (!isPause) {
                    refreshUI()
                    refreshBalance()
                }
                if (tryRefreshVipStateCount > 0) {
                    refreshVipState()
                    tryRefreshVipStateCount--
                }
            }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TradeFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: Boolean = false) =
            TradeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putBoolean(ARG_PARAM2, param2)
                }
            }
    }

    private fun doSend() {
        if (!activity?.hasNetWork()!!) {
            activity?.showToast(R.string.net_work_error)
            return
        }

        val tradeMaxValue = tradeAccountFundInfo?.getAvailableBase() ?: BigDecimal.ZERO
        val quoteMaxValue = quoteAccountFundInfo?.getAvailableBase() ?: BigDecimal.ZERO

        val price = rootView.buyPriceEdt.editableText.toString()
        val quantity = rootView.buyAmountEdt.editableText.toString()

        if (price.isEmpty() || quantity.isEmpty()) {
            activity?.showToast(R.string.please_input_transfer_amount)
            return
        }

        if (isSell) {
            if (tradeMaxValue < quantity.toBigDecimal()) {
                activity?.showToast(R.string.exchange_balance_not_enough)
                return
            }
        } else {
            if (quoteMaxValue < price.toBigDecimal().multiply(quantity.toBigDecimal())) {
                activity?.showToast(R.string.exchange_balance_not_enough)
                return
            }
        }

        if (price.isEmpty() || quantity.isEmpty()) {
            activity?.showToast(R.string.please_input_correct_amount)
            return
        }

        val sendAmountInBase = quantity.toBigDecimalOrNull() ?: kotlin.run {
            activity?.showToast(R.string.please_input_correct_amount)
            return
        }

        val normalTokenInfo = if (isSell) tradeTokenInfo else quoteTokenInfo
        normalTokenInfo?.let { normalTokenInfo ->
            val quantityBD = tradeTokenInfo?.baseToSmallestUnit(quantity.toBigDecimal())
            lastSendParams = NormalTxParams(
                toAddr = DexContractAddress,
                amountInSu = BigInteger.ZERO,
                data = BuildInContractEncoder.dexPlaceOrder(
                    tradeToken = tradeTokenInfo?.tokenAddress!!,
                    quoteToken = quoteTokenInfo?.tokenAddress!!,
                    side = isSell,
                    orderType = 0,
                    price = price,
                    quantity = quantityBD!!.toBigInteger()
                ),
                tokenId = tradeTokenInfo?.tokenAddress!!,
                accountAddr = currentAccount.nowViteAddress()!!
            )


            var saveTime = GlobalKVCache.read("dontTellMe24h")?.toLongOrNull()
            if (saveTime != null && System.currentTimeMillis() - saveTime <= 24L * 60 * 60 * 100) {
                lastSendParams?.let { sendTx(it) }
                curQuantity = ""
                rootView.buyAmountEdt.setText("")
            } else {
                TradeConfirmDialog(activity!!).apply {
                    setBigTitle(
                        getString(if (isSell) R.string.exchange_sell else R.string.exchange_buy) + "".symbolRemoveIndex(
                            tradeTokenSymbol
                        )
                    )
                    setPrice(curPrice)
                    setQuantity(curQuantity)
                    setPriceSymbol("".symbolRemoveIndex(quoteTokenSymbol))
                    setQuantitySymbol("".symbolRemoveIndex(tradeTokenSymbol))
                    setFirstButton(getString(R.string.confirm)) {
                        lastSendParams?.let { sendTx(it) }
                        curQuantity = ""
                        rootView.buyAmountEdt.setText("")
                        dismiss()
                    }
                    setSecondButton(getString(R.string.cancel)) {
                        dismiss()
                    }
                    show()
                }
            }
        }
    }

    fun doRevoke(orderId: String?) {
        if (!activity?.hasNetWork()!!) {
            activity?.showToast(R.string.net_work_error)
            return
        }

        orderId ?: run {
            activity?.showToast(R.string.can_find_order)
            return
        }

        tradeTokenInfo?.tokenAddress?.let { tokenAddress ->
            lastSendParams = NormalTxParams(
                toAddr = DexCancelContractAddress,
                amountInSu = BigInteger.ZERO,
                data = BuildInContractEncoder.dexCancelOrderByTransactionHash(
                    sendHash = orderId.hexToBytes()
                ),
                tokenId = tokenAddress,
                accountAddr = currentAccount.nowViteAddress()!!
            )

            lastSendParams?.let { sendTx(it) }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
