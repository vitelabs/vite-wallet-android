package net.vite.wallet.nut

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.nut.NutConfig
import net.vite.wallet.network.http.nut.NutConfigApi
import net.vite.wallet.utils.addTo


class NutViewModel : ViewModel() {

    private fun cacheName(isChinese: Boolean) = "NutConfigCache_${if (isChinese) {
        "zh"
    } else {
        "en"
    }}"

    private var compositeDisposable = CompositeDisposable()

    val networkState = MutableLiveData<NetworkState>()
    val nutConfigLd = MutableLiveData<NutConfig>()
    fun pullConfig(isChinese: Boolean) {
        networkState.postValue(NetworkState.LOADING)

        GlobalKVCache.readAsync(cacheName(isChinese)).subscribe({
            it?.let {
                try {
                    val config = Gson().fromJson(it, NutConfig::class.java)
                    if (config != null) {
                        nutConfigLd.postValue(config)
                    } else {
                    }
                } catch (e: Exception) {
                }
            }
        }, {
            it.printStackTrace()
        }).addTo(compositeDisposable)

        NutConfigApi.pull(isChinese).subscribeOn(Schedulers.io())
            .subscribe({
                if (it != null) {
                    nutConfigLd.postValue(it)
                    GlobalKVCache.store(
                        cacheName(isChinese) to Gson().toJson(it)
                    )
                }
                networkState.postValue(NetworkState.LOADED)
            }, {
                networkState.postValue(NetworkState.error(it))
            }).addTo(compositeDisposable)

    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}

