package net.vite.wallet.exchange

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.ViewObject
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.http.operation.ExchangeBannerResp
import net.vite.wallet.network.http.operation.OperationApi


class ExchangeViewModel : ViewModel() {
    private val gson = Gson()

    var loadBannerDisposable: Disposable? = null
    val bannerReqLd = MutableLiveData<ViewObject<List<ExchangeBannerResp>>>()
    val cacheMap = HashMap<String, List<ExchangeBannerResp>>()


    @SuppressLint("CheckResult")
    fun loadBanner(language: String) {

        if (cacheMap.containsKey(language)) {
            bannerReqLd.postValue(
                ViewObject.Loaded(cacheMap[language])
            )
            return
        }

        GlobalKVCache.readAsync("exchange_banner_list_$language").subscribe({
            if (it.isNotEmpty()) {
                kotlin.runCatching {
                    val type = object : TypeToken<List<ExchangeBannerResp>>() {}.type
                    bannerReqLd.postValue(
                        ViewObject.Loaded(
                            gson.fromJson<List<ExchangeBannerResp>>(
                                it,
                                type
                            )
                        )
                    )
                }
            }
        }, {})

        loadBannerDisposable?.dispose()
        loadBannerDisposable =
            OperationApi.pullExchangeBanner(language)
                .subscribeOn(Schedulers.io()).subscribe({
                    bannerReqLd.postValue(ViewObject.Loaded(it))
                    cacheMap[language] = it
                    GlobalKVCache.store("exchange_banner_list_$language" to gson.toJson(it))
                }, {
                    bannerReqLd.postValue(ViewObject.Error(it))
                })

    }

    override fun onCleared() {
        super.onCleared()
        loadBannerDisposable?.dispose()
        loadBannerDisposable = null
    }

}
