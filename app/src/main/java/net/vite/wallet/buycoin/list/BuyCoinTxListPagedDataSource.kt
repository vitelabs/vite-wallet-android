package net.vite.wallet.buycoin.list

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.coinpurchase.CoinPurchaseApi
import net.vite.wallet.network.http.coinpurchase.PurchaseRecordItem
import java.util.concurrent.CountDownLatch

@SuppressLint("CheckResult")
class BuyCoinTxListPagedDataSource(val addr: String) :
    PageKeyedDataSource<Int, PurchaseRecordItem>() {

    private var retry: (() -> Any)? = null

    val networkState = MutableLiveData<NetworkState>()

    val initialLoad = MutableLiveData<NetworkState>()

    fun retry() {
        retry?.invoke()
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, PurchaseRecordItem>
    ) {
        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)

        val countDownLatch = CountDownLatch(1)
        CoinPurchaseApi.history(addr, 0, params.requestedLoadSize).subscribe({ rawResp ->
            val list = rawResp ?: emptyList()
            if (list.size < params.requestedLoadSize) {
                callback.onResult(list, null, null)
            } else {
                callback.onResult(list, null, 2)
            }
            networkState.postValue(NetworkState.LOADED)
            initialLoad.postValue(NetworkState.LOADED)
            countDownLatch.countDown()
        }, {
            networkState.postValue(NetworkState.error(it))
            initialLoad.postValue(NetworkState.error(it))
            countDownLatch.countDown()
        })

        countDownLatch.await()
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, PurchaseRecordItem>) {
        networkState.postValue(NetworkState.LOADING)

        CoinPurchaseApi.history(addr, params.key, params.requestedLoadSize)
            .subscribeOn(Schedulers.io())
            .subscribe({ rawResp ->
                val list = rawResp ?: emptyList()
                if (list.size < params.requestedLoadSize) {
                    callback.onResult(list, null)
                } else {
                    callback.onResult(list, params.key + 1)
                }
                networkState.postValue(NetworkState.LOADED)
            }, {
                retry = {
                    loadAfter(params, callback)
                }
                networkState.postValue(
                    NetworkState.error(it)
                )
            })


    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, PurchaseRecordItem>) {
    }

}