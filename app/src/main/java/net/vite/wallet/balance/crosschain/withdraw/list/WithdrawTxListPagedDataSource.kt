package net.vite.wallet.balance.crosschain.withdraw.list

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.gw.GwCrosschainApi
import net.vite.wallet.network.http.gw.WithdrawRecord
import java.util.concurrent.CountDownLatch

@SuppressLint("CheckResult")
class WithdrawTxListPagedDataSource(val addr: String, val tti: String, val gwUrl: String) :
    PageKeyedDataSource<Int, WithdrawRecord>() {

    private var retry: (() -> Any)? = null

    val networkState = MutableLiveData<NetworkState>()

    val initialLoad = MutableLiveData<NetworkState>()

    fun retry() {
        retry?.invoke()
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, WithdrawRecord>
    ) {
        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)

        val countDownLatch = CountDownLatch(1)
        GwCrosschainApi.withdrawRecords(tti, addr, 1, params.requestedLoadSize, gwUrl)
            .subscribe({ rawResp ->
                val list = rawResp.data?.withdrawRecords?.map {
                    WithdrawRecord(
                        it.inTxHash,
                        it.inTxConfirmedCount,
                        it.inTxConfirmationCount,
                        it.outTxHash,
                        it.amount,
                        it.fee,
                        it.state,
                        it.dateTime,
                        tti,
                        rawResp.data
                    )
                } ?: emptyList()
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

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, WithdrawRecord>) {
        networkState.postValue(NetworkState.LOADING)

        GwCrosschainApi.withdrawRecords(tti, addr, params.key, params.requestedLoadSize, gwUrl)
            .subscribeOn(Schedulers.io())
            .subscribe({ rawResp ->
                val list = rawResp.data?.withdrawRecords?.map {
                    WithdrawRecord(
                        it.inTxHash,
                        it.inTxConfirmedCount,
                        it.inTxConfirmationCount,
                        it.outTxHash,
                        it.amount,
                        it.fee,
                        it.state,
                        it.dateTime,
                        tti,
                        rawResp.data
                    )
                } ?: emptyList()
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

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, WithdrawRecord>) {
    }

}