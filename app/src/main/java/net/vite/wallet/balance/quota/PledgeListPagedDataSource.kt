package net.vite.wallet.balance.quota

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.rpc.AsyncNet
import net.vite.wallet.network.rpc.PledgeInfo
import net.vite.wallet.network.rpc.SyncNet

class PledgeListPagedDataSource(val addr: String) : PageKeyedDataSource<Long, PledgeInfo>() {

    private var retry: (() -> Any)? = null

    val networkState = MutableLiveData<NetworkState>()

    val initialLoad = MutableLiveData<NetworkState>()

    fun retry() {
        retry?.invoke()
    }

    override fun loadInitial(params: LoadInitialParams<Long>, callback: LoadInitialCallback<Long, PledgeInfo>) {

        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)
        val resp = SyncNet.getPledgeList(addr, 0, params.requestedLoadSize.toLong())
        if (resp.success()) {
            val list = resp.resp?.pledgeInfoList ?: emptyList()
            callback.onResult(list, null, 1)
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

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<Long, PledgeInfo>) {
        networkState.postValue(NetworkState.LOADING)
        AsyncNet.getPledgeList(addr, params.key, params.requestedLoadSize.toLong()).subscribe(
            { resp ->
            if (resp.success()) {
                val list = resp.resp?.pledgeInfoList ?: emptyList()
                if (list.isEmpty()) {
                    callback.onResult(list, null)
                } else {
                    callback.onResult(list, params.key + 1)
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
            retry = {
                loadAfter(params, callback)
            }
            networkState.postValue(
                NetworkState.error(it)
            )
        })
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<Long, PledgeInfo>) {
    }

}