package net.vite.wallet.dexassets

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import net.vite.wallet.loge
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.vitex.VitexApi
import net.vite.wallet.network.rpc.AsyncNet
import net.vite.wallet.network.rpc.SyncNet
import java.math.BigDecimal

class DexTxListPagedDataSource(
    val addr: String,
    val tti: String,
    val isViteX: Boolean
) : PageKeyedDataSource<String, DexTokenTxVo>() {
    private var retry: (() -> Any)? = null

    val networkState = MutableLiveData<NetworkState>()
    val initialLoad = MutableLiveData<NetworkState>()

    fun retry() {
        retry?.invoke()
    }

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<String, DexTokenTxVo>
    ) {
        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)
        if (isViteX) {
            VitexApi.getDepositWithdrawRecords(
                address = addr,
                tokenId = tti,
                offset = 0,
                limit = params.requestedLoadSize
            ).blockingSubscribe({
                val list = it.record.map { DexTokenTxVo.fromVitex(it) }
                if (list.size < params.requestedLoadSize) {
                    callback.onResult(list, null, null)
                } else {
                    callback.onResult(list, null, "1")
                }
                networkState.postValue(NetworkState.LOADED)
                initialLoad.postValue(NetworkState.LOADED)
            }, {
                retry = {
                    loadInitial(params, callback)
                }
                val error = NetworkState.error(it)
                networkState.postValue(error)
                initialLoad.postValue(error)
            })
        } else {
            val resp = SyncNet.getBlocksByHashInToken(addr, "", tti, 200)
            if (resp.success()) {
                val list = resp.resp?.filter {
                    (it.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO) != BigDecimal.ZERO
                }?.map {
                    DexTokenTxVo.fromAccountBlock(it)
                } ?: emptyList()
                if (list.size < params.requestedLoadSize) {
                    callback.onResult(list, null, null)
                } else {
                    callback.onResult(list, null, null)
                }

                networkState.postValue(NetworkState.LOADED)
                initialLoad.postValue(NetworkState.LOADED)
            } else {
                retry = {
                    loadInitial(params, callback)
                }
                val error = NetworkState.error(resp.throwable)
                networkState.postValue(error)
                initialLoad.postValue(error)
            }
        }


    }

    @SuppressLint("CheckResult")
    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<String, DexTokenTxVo>
    ) {
        networkState.postValue(NetworkState.LOADING)
        if (isViteX) {
            VitexApi.getDepositWithdrawRecords(
                address = addr,
                tokenId = tti,
                offset = params.key.toInt(),
                limit = params.requestedLoadSize
            ).subscribe({
                val list = it.record.map { DexTokenTxVo.fromVitex(it) }
                if (list.size < params.requestedLoadSize) {
                    callback.onResult(list, null)
                } else {
                    callback.onResult(list, (params.key.toInt() + 1).toString())
                }
                networkState.postValue(NetworkState.LOADED)
            }, {
                retry = {
                    loadAfter(params, callback)
                }
                val error = NetworkState.error(it)
                networkState.postValue(error)
            })
        } else {
            AsyncNet.getBlocksByHashInToken(addr, params.key, tti, params.requestedLoadSize)
                .subscribe({ resp ->
                    if (resp.success()) {
                        val list = resp.resp?.filter {
                            (it.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO) != BigDecimal.ZERO
                        }?.map {
                            DexTokenTxVo.fromAccountBlock(it)
                        } ?: emptyList()
                        if (list.size < params.requestedLoadSize) {
                            callback.onResult(list, null)
                        } else {
                            callback.onResult(list, null)
                        }
                        networkState.postValue(NetworkState.LOADED)
                    } else {
                        retry = {
                            loadAfter(params, callback)
                        }
                        networkState.postValue(
                            NetworkState.error(resp.throwable)
                        )
                    }
                }, {
                    loge(it)
                })
        }

    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<String, DexTokenTxVo>
    ) {
    }


}