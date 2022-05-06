package net.vite.wallet.balance.quota

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.rpc.PledgeInfo


class PledgeListViewModel : ViewModel() {
    private class LoadContext(
        val pagedList: LiveData<PagedList<PledgeInfo>>,
        val networkState: LiveData<NetworkState>,
        val refreshState: LiveData<NetworkState>,
        val refresh: () -> Unit,
        val retry: () -> Unit,
        val emptyList: LiveData<Boolean>

    )

    private val addrLd = MutableLiveData<String>()
    private val txLoadContextLd = Transformations.map(addrLd) {
        load(it)
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

    fun initLoad(addr: String) {
        addrLd.value = addr
    }

    fun refresh() {
        txLoadContextLd?.value?.refresh?.invoke()
    }

    fun retry() {
        txLoadContextLd?.value?.retry?.invoke()
    }

    private fun load(addr: String): LoadContext {
        val sourceFactory = PledgeDataSourceFactory(addr)

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
            .setBoundaryCallback(object : PagedList.BoundaryCallback<PledgeInfo>() {
                override fun onZeroItemsLoaded() {
                    emptyList.postValue(true)
                }

                override fun onItemAtEndLoaded(itemAtEnd: PledgeInfo) {
                }

                override fun onItemAtFrontLoaded(itemAtFront: PledgeInfo) {
                }
            })
            .build()

        return LoadContext(
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