package net.vite.wallet.exchange

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.ViewObject
import net.vite.wallet.network.http.vitex.MiningSetting
import net.vite.wallet.network.http.vitex.OperatorInfo
import net.vite.wallet.network.http.vitex.TickerStatistics
import java.util.concurrent.TimeUnit

class MarketVM : ViewModel() {
    val allOperatorInfoLd = MutableLiveData<ViewObject<Map<String, OperatorInfo>>>()

    fun getAllOperatorInfo(isForce: Boolean) {
        TickerStatisticsCenter.getAllOperatorInfo(allOperatorInfoLd, isForce)
    }

    val getMiningSettingLd = MutableLiveData<ViewObject<MiningSetting>>()

    fun getMiningSetting(isForce: Boolean) {
        TickerStatisticsCenter.getMiningSetting(getMiningSettingLd, isForce)
    }

    fun getAll24HPriceChangeByCategory(
        isForce: Boolean
    ) {
        MarketFragment.AllTypeArray.forEach { type ->
            TickerStatisticsCenter.get24HPriceChangeByCategory(
                httpMarketLd,
                type,
                isForce,
                MarketFragment.getRequestCode(type)
            )
        }
    }


    val httpMarketLd = MutableLiveData<ViewObject<List<TickerStatistics>>>()
    fun get24HPriceChangeByCategory(
        category: String,
        isForce: Boolean,
        requestCode: Int? = null
    ) {
        TickerStatisticsCenter.get24HPriceChangeByCategory(
            httpMarketLd,
            category,
            isForce,
            requestCode
        )
    }

    val symbolsMarketLd = MutableLiveData<ViewObject<List<TickerStatistics>>>()
    fun get24HPriceChangeBySymbols(
        symbols: List<String>,
        isForce: Boolean,
        requestCode: Int? = null
    ) {
        TickerStatisticsCenter.get24HPriceChangeBySymbols(
            symbolsMarketLd,
            symbols,
            isForce,
            requestCode
        )
    }

    val tokenRateLd = MutableLiveData<ViewObject<Unit>>()
    fun getAllTokenExchangeRate(
        isForce: Boolean,
        requestCode: Int? = null
    ) {
        TickerStatisticsCenter.getAllTokenExchangeRate(
            tokenRateLd,
            isForce,
            requestCode
        )

        tokenRateDisposable ?: kotlin.run {
            tokenRateDisposable = Completable.fromCallable {
                TickerStatisticsCenter.getAllTokenExchangeRate(
                    tokenRateLd,
                    true
                )
                1
            }.repeatWhen {
                it.delay(30, TimeUnit.SECONDS)
            }.subscribeOn(Schedulers.io()).subscribe({}, {})
        }
    }


    private var tokenRateDisposable: Disposable? = null
    private var tokenIsMiningDisposable: Disposable? = null

    override fun onCleared() {
        tokenRateDisposable?.dispose()
        tokenIsMiningDisposable?.dispose()
    }
}
