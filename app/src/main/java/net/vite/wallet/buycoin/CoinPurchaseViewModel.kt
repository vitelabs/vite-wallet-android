package net.vite.wallet.buycoin

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.vite.wallet.ViewObject
import net.vite.wallet.network.http.coinpurchase.CoinPurchaseApi
import net.vite.wallet.network.http.coinpurchase.CoinPurchaseExchangeReq
import net.vite.wallet.network.http.coinpurchase.CoinPurchaseExchangeResp
import net.vite.wallet.network.http.coinpurchase.ConvertRateResp

@SuppressLint("CheckResult")
class CoinPurchaseViewModel : ViewModel() {
    val convertRateLd = MutableLiveData<ViewObject<ConvertRateResp>>()
    fun convertRate(address: String, requestCode: Int = 0) {
        convertRateLd.postValue(ViewObject.Loading(requestCode))
        CoinPurchaseApi.convertRate(address).subscribe({
            convertRateLd.postValue(ViewObject.Loaded(it, requestCode))
        }, {
            convertRateLd.postValue(ViewObject.Error(it, requestCode))
        })
    }

    val exchangeLd = MutableLiveData<ViewObject<CoinPurchaseExchangeResp>>()
    fun exchange(coinPurchaseExchangeReq: CoinPurchaseExchangeReq, requestCode: Int = 0) {
        exchangeLd.postValue(ViewObject.Loading(requestCode))
        CoinPurchaseApi.exchange(coinPurchaseExchangeReq).subscribe({
            exchangeLd.postValue(ViewObject.Loaded(it, requestCode))
        }, {
            exchangeLd.postValue(ViewObject.Error(it, requestCode))
        })
    }
}