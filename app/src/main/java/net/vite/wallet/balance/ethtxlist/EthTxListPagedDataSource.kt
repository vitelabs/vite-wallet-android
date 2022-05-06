package net.vite.wallet.balance.ethtxlist

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import net.vite.wallet.loge
import net.vite.wallet.logi
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.eth.EthTransaction
import net.vite.wallet.network.eth.EthTransactionApi

class EthTxListPagedDataSource(val addr: String, val contractAddress: String = "") :
    PageKeyedDataSource<String, EthTransaction>() {

    private var retry: (() -> Any)? = null

    val networkState = MutableLiveData<NetworkState>()

    val initialLoad = MutableLiveData<NetworkState>()

    fun retry() {
        retry?.invoke()
    }

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<String, EthTransaction>
    ) {

        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)


        if (contractAddress != "") {
            EthTransactionApi.getErc20TransactionsSync(
                addr,
                contractAddress,
                1,
                params.requestedLoadSize
            )
                .subscribe({

                    if (it.status == "1") {
                        val list = it.result
                        if (list.size < params.requestedLoadSize) {
                            callback.onResult(list, null, null)
                        } else {
                            callback.onResult(
                                list,
                                null,
                                list.lastOrNull()?.blockHash + list.lastOrNull()?.from + list.lastOrNull()?.to
                            )
                        }

                        networkState.postValue(NetworkState.LOADED)
                        initialLoad.postValue(NetworkState.LOADED)
                    } else {
                        logi("eth transactions got error ${it.message}")
                        networkState.postValue(NetworkState.LOADED)
                        initialLoad.postValue(NetworkState.LOADED)
                    }
                }, {
                    retry = {
                        loadInitial(params, callback)
                    }
                    val error = NetworkState.error(it)
                    networkState.postValue(error)
                    initialLoad.postValue(error)
                })
        } else {
            EthTransactionApi.getTransactionsSync(
                addr,
                1,
                params.requestedLoadSize
            )
                .subscribe({

                    if (it.status == "1") {
                        val list = it.result
                        if (list.size < params.requestedLoadSize) {
                            callback.onResult(list, null, null)
                        } else {
                            callback.onResult(
                                list,
                                null,
                                list.lastOrNull()?.blockHash + list.lastOrNull()?.from + list.lastOrNull()?.to
                            )
                        }

                        networkState.postValue(NetworkState.LOADED)
                        initialLoad.postValue(NetworkState.LOADED)
                    } else {
                        logi("eth transactions got error ${it.message}")
                        networkState.postValue(NetworkState.LOADED)
                        initialLoad.postValue(NetworkState.LOADED)
                    }
                }, {
                    retry = {
                        loadInitial(params, callback)
                    }
                    val error = NetworkState.error(it)
                    networkState.postValue(error)
                    initialLoad.postValue(error)
                })
        }

    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<String, EthTransaction>
    ) {
        networkState.postValue(NetworkState.LOADING)

        if (contractAddress != "") {
            EthTransactionApi.getErc20Transactions(
                addr,
                contractAddress,
                1,
                params.requestedLoadSize
            )
                .subscribe({

                    if (it.status == "1") {
                        val list = it.result
                        if (list.size < params.requestedLoadSize) {
                            callback.onResult(list, null)
                        } else {
                            callback.onResult(
                                list,
                                list.lastOrNull()?.blockHash + list.lastOrNull()?.from + list.lastOrNull()?.to
                            )
                        }

                        networkState.postValue(NetworkState.LOADED)
                        initialLoad.postValue(NetworkState.LOADED)
                    } else {
                        logi("eth transactions got error ${it.message}")
                        networkState.postValue(NetworkState.LOADED)
                        initialLoad.postValue(NetworkState.LOADED)
                    }
                }, {
                    retry = {
                        loadAfter(params, callback)
                    }
                    val error = NetworkState.error(it)
                    networkState.postValue(error)
                    initialLoad.postValue(error)
                })
        } else {
            EthTransactionApi.getTransactions(
                addr,
                1,
                params.requestedLoadSize
            )
                .subscribe({
                    if (it.status == "1") {

                        val list = it.result
                        if (list.size < params.requestedLoadSize) {
                            callback.onResult(list, null)
                        } else {
                            callback.onResult(
                                list,
                                list.lastOrNull()?.blockHash + list.lastOrNull()?.from + list.lastOrNull()?.to
                            )
                        }

                        networkState.postValue(NetworkState.LOADED)
                        initialLoad.postValue(NetworkState.LOADED)
                    } else {
                        logi("eth transactions got error ${it.message}")
                        networkState.postValue(NetworkState.LOADED)
                        initialLoad.postValue(NetworkState.LOADED)
                    }
                }, {
                    retry = {
                        loadAfter(params, callback)
                    }
                    networkState.postValue(
                        NetworkState.error(it)
                    )
                    loge(it)
                })
        }
    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<String, EthTransaction>
    ) {
    }

}