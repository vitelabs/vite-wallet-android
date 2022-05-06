package net.vite.wallet.dexassets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import net.vite.wallet.network.NetworkState
import org.vitelabs.mobile.Mobile

class DexTxListViewModel : ViewModel() {
    private class InitLoadParams(
        val addr: String,
        val tti: String,
        val isViteX: Boolean
    )

    private class TxLoadContext(
        val pagedList: LiveData<PagedList<DexTokenTxVo>>,
        val networkState: LiveData<NetworkState>,
        val refreshState: LiveData<NetworkState>,
        val refresh: () -> Unit,
        val retry: () -> Unit,
        val emptyList: LiveData<Boolean>

    )

    private val initLoadParams = MutableLiveData<InitLoadParams>()
    private val txLoadContextLd = Transformations.map(initLoadParams) {
        load(it.addr, it.tti, it.isViteX)
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

    fun initLoad(addr: String, tti: String, isViteX: Boolean) {
        if (Mobile.isValidAddress(addr)) {
            initLoadParams.value = InitLoadParams(addr, tti, isViteX)
        }
    }

    fun refresh() {
        txLoadContextLd.value?.refresh?.invoke()
    }

    fun retry() {
        txLoadContextLd.value?.retry?.invoke()
    }

    private fun load(addr: String, tti: String, isViteX: Boolean): TxLoadContext {
        val sourceFactory = DexTxDataSourceFactory(addr, tti, isViteX)

        val emptyList = MutableLiveData<Boolean>()
        val pagedListConfig = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setInitialLoadSizeHint(50 * 2)
            .setPageSize(50)
            .build()

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        val pagedList = LivePagedListBuilder(sourceFactory, pagedListConfig)
            .setBoundaryCallback(object : PagedList.BoundaryCallback<DexTokenTxVo>() {
                override fun onZeroItemsLoaded() {
                    emptyList.postValue(true)
                }

                override fun onItemAtEndLoaded(itemAtEnd: DexTokenTxVo) {
                }

                override fun onItemAtFrontLoaded(itemAtFront: DexTokenTxVo) {
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