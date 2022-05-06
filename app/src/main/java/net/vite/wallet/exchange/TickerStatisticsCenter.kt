package net.vite.wallet.exchange

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import net.vite.wallet.ViewObject
import net.vite.wallet.exchange.net.ws.ExchangeWsHolder
import net.vite.wallet.network.http.vitex.*
import java.util.concurrent.ConcurrentHashMap

object TickerStatisticsCenter {

    val symbolMap = ConcurrentHashMap<String, TickerStatistics>()
    val httpCategoryCacheMap = ConcurrentHashMap<String, ArrayList<TickerStatistics>>()
    val tokenRateCacheMap = ConcurrentHashMap<String, ExchangeTokenRate>()
    val operatorInfoCache = ConcurrentHashMap<String, OperatorInfo>()

    @Volatile
    var dexTokensCache = listOf<NormalTokenInfo>()

    @Volatile
    var miningSettingCacheMap: MiningSetting? = null

    fun updateMaps(tickerStat: TickerStatistics) {
        if (SpecialMarketCenter.isHidden(tickerStat.symbol)) {
            return
        }
        symbolMap[tickerStat.symbol] = tickerStat
    }

    fun updateCategoryMap(category: String, tickerStats: ArrayList<TickerStatistics>) {
        httpCategoryCacheMap[category] = ArrayList(tickerStats.filter { !SpecialMarketCenter.isHidden(it.symbol) })
    }

    fun updateCategoryMap(tickerStat: TickerStatistics) {
        if (SpecialMarketCenter.isHidden(tickerStat.symbol)) {
            return
        }
        httpCategoryCacheMap.values.forEach { list ->
            val index = list.indexOfFirst { it.symbol == tickerStat.symbol }
            if (index != -1) {
                list[index] = tickerStat
            }
        }
    }

    fun onNewMessageArrived(tickerStatistics: TickerStatistics) {
        updateMaps(tickerStatistics)
        updateCategoryMap(tickerStatistics)
    }

    @SuppressLint("CheckResult")
    fun get24HPriceChangeByCategory(
        ld: MutableLiveData<ViewObject<List<TickerStatistics>>>,
        category: String,
        isForce: Boolean = false,
        requestCode: Int? = null
    ) {
        val cache = httpCategoryCacheMap[category]
        if (!isForce && cache != null && ExchangeWsHolder.hasEstablishedWs()) {
            ld.postValue(ViewObject.Loaded(cache, requestCode))
            return
        }
        ld.postValue(ViewObject.Loading(requestCode))
        VitexApi.get24HPriceChangeByCategory(category).subscribe({
            it.forEach {
                updateMaps(it)
            }
            val list = ArrayList<TickerStatistics>()
            list.addAll(it)
            updateCategoryMap(category, list)
            ld.postValue(ViewObject.Loaded(it, requestCode))
        }, {
            ld.postValue(ViewObject.Error(it, requestCode))
        })

    }

    @SuppressLint("CheckResult")
    fun get24HPriceChangeBySymbols(
        ld: MutableLiveData<ViewObject<List<TickerStatistics>>>,
        symbols: List<String>,
        force: Boolean,
        requestCode: Int?
    ) {
        var foundAll = true
        val cache = ArrayList<TickerStatistics>()
        symbols.forEach {
            val maybeCache = symbolMap[it]?.also {
                cache.add(it)
            }
            foundAll = foundAll && maybeCache != null
        }
        if (!force && foundAll && ExchangeWsHolder.hasEstablishedWs()) {
            ld.postValue(ViewObject.Loaded(cache, requestCode))
            return
        }
        ld.postValue(ViewObject.Loading(requestCode))
        VitexApi.get24HPriceChangeBySymbols(symbols).subscribe({
            it.forEach {
                updateMaps(it)
            }
            ld.postValue(ViewObject.Loaded(it, requestCode))
        }, {
            ld.postValue(ViewObject.Error(it, requestCode))
        })
    }


    @SuppressLint("CheckResult")
    fun getAllTokenExchangeRate(
        ld: MutableLiveData<ViewObject<Unit>>,
        isForce: Boolean,
        requestCode: Int? = null
    ) {
        if (!isForce && tokenRateCacheMap.isNotEmpty()) {
            ld.postValue(ViewObject.Loaded(Unit, requestCode))
            return
        }
        ld.postValue(ViewObject.Loading(requestCode))
        VitexApi.getTokenExchangeRate(listOf("VITE", "BTC-000", "ETH-000", "USDT-000")).subscribe({
            it.forEach {
                tokenRateCacheMap[it.tokenSymbol] = it
            }
            ld.postValue(ViewObject.Loaded(Unit, requestCode))
        }, {
            ld.postValue(ViewObject.Error(it, requestCode))
        })
    }

    @SuppressLint("CheckResult")
    fun getAllOperatorInfo(
        ld: MutableLiveData<ViewObject<Map<String, OperatorInfo>>>,
        isForce: Boolean,
        requestCode: Int? = null
    ) {
        if (!isForce && operatorInfoCache.isNotEmpty()) {
            ld.postValue(ViewObject.Loaded(operatorInfoCache, requestCode))
            return
        }
        ld.postValue(ViewObject.Loading(requestCode))
        VitexApi.getAllOperatorInfo().subscribe({ operators ->
            operators.forEach { operatorInfo ->
                operatorInfo.tradePairs.values.forEach { tradePairs ->
                    tradePairs.forEach {
                        operatorInfoCache[it] = operatorInfo
                    }
                }
            }
            ld.postValue(ViewObject.Loaded(operatorInfoCache, requestCode))
        }, {
            ld.postValue(ViewObject.Error(it, requestCode))
        })
    }

    @SuppressLint("CheckResult")
    fun getMiningSetting(
        ld: MutableLiveData<ViewObject<MiningSetting>>,
        isForce: Boolean,
        requestCode: Int? = null
    ) {
        if (!isForce && miningSettingCacheMap != null) {
            ld.postValue(ViewObject.Loaded(miningSettingCacheMap, requestCode))
            return
        }
        ld.postValue(ViewObject.Loading(requestCode))
        VitexApi.getMiningSetting().subscribe({
            miningSettingCacheMap = it
            ld.postValue(ViewObject.Loaded(it, requestCode))
        }, {
            ld.postValue(ViewObject.Error(it, requestCode))
        })
    }

    fun isOrderMiningPair(symbol: String): Boolean {
        return miningSettingCacheMap?.orderSymbols?.indexOf(symbol) ?: -1 != -1
    }

    fun isTradeMiningPair(symbol: String): Boolean {
        return miningSettingCacheMap?.tradeSymbols?.indexOf(symbol) ?: -1 != -1
    }

    fun isOrderAndTradeMiningPair(symbol: String): Boolean {
        return miningSettingCacheMap?.orderSymbols?.indexOf(symbol) ?: -1 != -1
                && miningSettingCacheMap?.tradeSymbols?.indexOf(symbol) ?: -1 != -1
    }

    fun getMiningRate(symbolPair: String): String? {
        val result = miningSettingCacheMap?.orderMiningMultiples?.get(symbolPair)
        return if (result == "null") null else result
    }

    fun getMiningOrderMiningSetting(symbolPair: String): OrderMiningSetting? {
        return miningSettingCacheMap?.orderMiningSettings?.get(symbolPair)
    }

    fun getZeroFeeList(symbolPair: String): List<String>? {
        return miningSettingCacheMap?.zeroFeePairs
    }

    fun isZeroFee(symbolPair: String): Boolean {
        return miningSettingCacheMap?.zeroFeePairs?.contains(symbolPair) ?: false
    }

}