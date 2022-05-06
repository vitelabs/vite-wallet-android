package net.vite.wallet.balance.crosschain.deposit.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.gw.DepositRecord
import org.vitelabs.mobile.Mobile


class DepositRecordsVm : ViewModel() {
    private class TxLoadContext(
        val pagedList: LiveData<PagedList<DepositRecord>>,
        val networkState: LiveData<NetworkState>,
        val refreshState: LiveData<NetworkState>,
        val refresh: () -> Unit,
        val retry: () -> Unit,
        val emptyList: LiveData<Boolean>

    )

    private val addrLd = MutableLiveData<Pair<String, Pair<String, String>>>()
    private val txLoadContextLd = Transformations.map(addrLd) {
        load(it.first, it.second.first, it.second.second)
    }


    val pagedListLd = Transformations.switchMap(txLoadContextLd) {
        it.pagedList
    }

    val emptyListLd = Transformations.switchMap(txLoadContextLd) {
        it.emptyList
    }

    val networkState = Transformations.switchMap(txLoadContextLd) {
        it.networkState
    }

    val refreshState = Transformations.switchMap(txLoadContextLd) {
        it.refreshState
    }

    fun initLoad(addr: String, tti: String, gwUrl: String) {
        if (Mobile.isValidAddress(addr)) {
            addrLd.value = addr to (tti to gwUrl)
        }
    }

    fun refresh() {
        txLoadContextLd.value?.refresh?.invoke()
    }

    fun retry() {
        txLoadContextLd.value?.retry?.invoke()
    }

    private fun load(addr: String, tti: String, gwUrl: String): TxLoadContext {
        val sourceFactory = TxDataSourceFactory(addr, tti, gwUrl)

        val emptyList = MutableLiveData<Boolean>()
        val pagedListConfig = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setInitialLoadSizeHint(15 * 2)
            .setPageSize(15)
            .build()

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        val pagedList = LivePagedListBuilder(sourceFactory, pagedListConfig)
            .setBoundaryCallback(object : PagedList.BoundaryCallback<DepositRecord>() {
                override fun onZeroItemsLoaded() {
                    emptyList.postValue(true)
                }

                override fun onItemAtEndLoaded(itemAtEnd: DepositRecord) {
                }

                override fun onItemAtFrontLoaded(itemAtFront: DepositRecord) {
                }
            })
            .build()

        return TxLoadContext(
            pagedList = pagedList,
            networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                it.networkState
            },
            retry = {
                sourceFactory.sourceLiveData.value?.retry()
            },
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState,
            emptyList = emptyList
        )
    }
}