package net.vite.wallet.exchange.history

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.vite.wallet.ViewObject
import net.vite.wallet.network.http.vitex.SuspendableVitexApi

enum class DateRange(val range: Long) {
    ONE_DAY(24L * 60 * 60 * 1000),
    ONE_WEEK(7 * 24L * 60 * 60 * 1000),
    ONE_MONTH(30 * 24L * 60 * 60 * 1000),
    THREE_MONTH(3 * 30 * 24L * 60 * 60 * 1000),
    ALL(-1)

}


data class OrdersFilter(
    val symbol: String? = null,
    val quoteTokenSymbol: String? = null,
    val tradeTokenSymbol: String? = null,
    val dateRange: DateRange? = null,
    //side	INTEGER	NO	Order side. 0 - buy, 1 - sell
    val side: Int? = null,
    //status INTEGER	NO	Order status,
    // valid in [0-10].
    // 3,5 - returns orders that are unfilled or partially filled;
    // 7,8 - returns orders that are cancelled or partially cancelled
    //https://docs.vite.org/go-vite/dex/api/rest-api.html#order-status
    val status: Int? = null,
    val total: Int = 1,
) {
    companion object {
        val EMPTY = OrdersFilter()
    }

    val startTime: Long?
        get() = dateRange?.takeIf { it != DateRange.ALL }?.let {
            (System.currentTimeMillis() - it.range) / 1000
        }
}


class HistoryOrdersListViewModel : ViewModel() {

    val historyOrders: MutableLiveData<List<PagedOrderListItem>> =
        MutableLiveData(emptyList())

    val historyOrdersPageRequest: MutableLiveData<ViewObject<PagedOrderListItem>> =
        MutableLiveData(ViewObject.Init())

    fun refresh(address: String, filter: OrdersFilter) {
        historyOrdersPageRequest.value = ViewObject.Init()
        historyOrders.value = emptyList()
        getHistoryOrders(address, filter)
    }


    fun getHistoryOrders(address: String, filter: OrdersFilter) {
        val lastRequest = historyOrdersPageRequest.value
        if (lastRequest?.isLoading() == true) {
            return
        }
        if (lastRequest?.resp != null && lastRequest.resp?.orders?.size ?: 0 < PAGE_SIZE) {
            return
        }
        val offset = (historyOrders.value?.lastOrNull()?.offset ?: -1) + 1
        viewModelScope.launch {
            historyOrdersPageRequest.postValue(ViewObject.Loading())
            try {
                val orders = SuspendableVitexApi.getAllOrders(
                    address,
                    filter.symbol,
                    filter.quoteTokenSymbol,
                    filter.tradeTokenSymbol,
                    filter.startTime,
                    null,
                    filter.side,
                    filter.status,
                    offset,
                    PAGE_SIZE,
                    filter.total
                )
                val item = PagedOrderListItem(
                    offset,
                    orders.order ?: emptyList()
                )
                historyOrders.postValue(
                    (historyOrders.value?.toMutableList() ?: ArrayList()).apply {
                        add(item)
                    })
                historyOrdersPageRequest.postValue(ViewObject.Loaded(item))
            } catch (e: Exception) {
                historyOrdersPageRequest.postValue(ViewObject.Error(e))
            }
        }
    }

}