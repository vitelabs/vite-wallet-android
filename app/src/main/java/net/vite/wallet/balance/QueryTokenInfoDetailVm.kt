package net.vite.wallet.balance

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.vitex.CCurrencyInfoDetail
import net.vite.wallet.network.http.vitex.VitexApi


class QueryTokenInfoDetailVm : ViewModel() {
    val queryResultLd = MutableLiveData<CCurrencyInfoDetail>()
    val networkState = MutableLiveData<NetworkState>()
    @SuppressLint("CheckResult")
    fun queryTokenInfoDetail(queryCode: String) {
        networkState.postValue(NetworkState.LOADING)
        VitexApi.getApi()
            .batchQueryTokenInfoDetail(listOf(queryCode))
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (it.data?.size == 1) {
                    queryResultLd.postValue(it.data[0])
                }
                networkState.postValue(NetworkState.LOADED)
            }, {
                networkState.postValue(NetworkState.error(it))
            })

    }
}