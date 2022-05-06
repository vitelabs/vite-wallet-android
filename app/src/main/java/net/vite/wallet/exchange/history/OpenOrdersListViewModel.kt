package net.vite.wallet.exchange.history

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.vite.wallet.ViewObject
import net.vite.wallet.network.http.vitex.SuspendableVitexApi


internal const val PAGE_SIZE = 50


class OpenOrdersListViewModel : ViewModel() {
    val openOrders: MutableLiveData<List<PagedOrderListItem>> =
        MutableLiveData(emptyList())

    val openOrdersPageRequest: MutableLiveData<ViewObject<PagedOrderListItem>> =
        MutableLiveData(ViewObject.Init())

    fun refreshOpenOrders(address: String) {
        openOrdersPageRequest.value = ViewObject.Init()
        openOrders.value = emptyList()
        getOpenOrders(address)
    }

    fun getOpenOrders(address: String) {
        val lastRequest = openOrdersPageRequest.value
        if (lastRequest?.isLoading() == true) {
            return
        }
        if (lastRequest?.resp != null && lastRequest.resp?.orders?.size ?: 0 < PAGE_SIZE) {
            // means last page has loaded
            return
        }

        val offset = (openOrders.value?.lastOrNull()?.offset ?: -1) + 1
        viewModelScope.launch {
            openOrdersPageRequest.postValue(ViewObject.Loading())
            try {
                val orders = SuspendableVitexApi.getOpenOrders(
                    address,
                    null,
                    null,
                    null,
                    offset,
                    PAGE_SIZE
                )
                val item = PagedOrderListItem(
                    offset,
                    orders.order ?: emptyList()
                )
                openOrders.postValue((openOrders.value?.toMutableList() ?: ArrayList()).apply {
                    add(item)
                })
                openOrdersPageRequest.postValue(ViewObject.Loaded(item))
            } catch (e: Exception) {
                openOrdersPageRequest.postValue(ViewObject.Error(e))
            }
        }
    }

}