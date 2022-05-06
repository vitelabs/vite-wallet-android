package net.vite.wallet.balance.txlist

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import net.vite.wallet.loge
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.rpc.AccountBlock
import net.vite.wallet.network.rpc.AsyncNet
import net.vite.wallet.network.rpc.SyncNet

class TxListPagedDataSource(val addr: String, val tti: String = "") : PageKeyedDataSource<String, AccountBlock>() {

    private var retry: (() -> Any)? = null

    val networkState = MutableLiveData<NetworkState>()

    val initialLoad = MutableLiveData<NetworkState>()

    fun retry() {
        retry?.invoke()
    }

    override fun loadInitial(params: LoadInitialParams<String>, callback: LoadInitialCallback<String, AccountBlock>) {

        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)
        val resp = SyncNet.getBlocksByHashInToken(addr, "", tti, params.requestedLoadSize)
        if (resp.success()) {
            val list = resp.resp ?: emptyList()
            if (list.size < params.requestedLoadSize) {
                callback.onResult(list, null, null)
            } else {
                callback.onResult(list, null, list.lastOrNull()?.prevHash)
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

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String, AccountBlock>) {
        networkState.postValue(NetworkState.LOADING)
        AsyncNet.getBlocksByHashInToken(addr, params.key, tti, params.requestedLoadSize)
            .subscribe({ resp ->
                if (resp.success()) {
                    val list = resp.resp ?: emptyList()
                    if (list.size < params.requestedLoadSize) {
                        callback.onResult(list, null)
                    } else {
                        callback.onResult(list, list.lastOrNull()?.prevHash)
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

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<String, AccountBlock>) {
    }

}