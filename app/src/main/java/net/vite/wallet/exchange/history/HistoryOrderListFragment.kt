package net.vite.wallet.exchange.history

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import net.vite.wallet.R
import net.vite.wallet.account.AccountAwareFragment
import net.vite.wallet.exchange.search.MarketPairSearchActivity
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.utils.viewbinding.findMyView


class HistoryOrderListFragment : AccountAwareFragment() {

    val recyclerView by findMyView<RecyclerView>(R.id.recyclerView)
    val emptyView by findMyView<View>(R.id.emptyGroup)
    val swipeRefresh by findMyView<SwipeRefreshLayout>(R.id.swipeRefresh)
    var filterBottomSheetDialog: FilterBottomSheetDialog? = null
    val address
        get() = currentAccount.nowViteAddress()!!

    private var currentFilter: OrdersFilter = OrdersFilter.EMPTY
    private lateinit var adapter: OrdersAdapter

    val vm by activityViewModels<HistoryOrdersListViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_trade_history_list, container, false)

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = OrdersAdapter()
        recyclerView.adapter = adapter

        vm.historyOrders.observe(viewLifecycleOwner) {
            emptyView.isVisible = it.isEmpty() || it.last().orders.isEmpty()
            recyclerView.isVisible = !emptyView.isVisible
        }

        vm.historyOrdersPageRequest.observe(viewLifecycleOwner) {
            swipeRefresh.isRefreshing = it.isLoading() && adapter.orders.isEmpty()
            if (it.isInit()) {
                adapter.orders.clear()
                adapter.notifyDataSetChanged()
                return@observe
            }

            val newOrders = it.resp?.orders
            if (newOrders?.isNotEmpty() == true) {
                adapter.orders.addAll(newOrders)
                adapter.notifyItemRangeInserted(
                    adapter.orders.size - newOrders.size,
                    newOrders.size
                )
            }
        }

        recyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - 5) {
                    vm.getHistoryOrders(address, currentFilter)
                }
            }
        })

        swipeRefresh.setOnRefreshListener {
            currentFilter = OrdersFilter.EMPTY
            vm.refresh(address, currentFilter)
        }

        vm.refresh(address, currentFilter)
    }


    fun onFilterIconClick() {
        filterBottomSheetDialog = FilterBottomSheetDialog(
            requireContext(),
            currentFilter,
            this
        )
        filterBottomSheetDialog?.show()
        filterBottomSheetDialog?.setOnDismissListener {
            filterBottomSheetDialog = null
        }
    }

    fun chooseSymbol() {
        startActivityForResult(
            Intent(requireActivity(), MarketPairSearchActivity::class.java),
            1233
        )
    }

    fun setFilter(filter: OrdersFilter) {
        currentFilter = filter
        vm.refresh(address, filter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.clearOnScrollListeners()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1233 && resultCode == Activity.RESULT_OK) {
            val symbol = kotlin.runCatching {
                Gson().fromJson(
                    data?.getStringExtra("result") ?: "", TickerStatistics::class.java
                )
            }.getOrNull()?.symbol ?: return
            filterBottomSheetDialog?.let {
                it.filter = it.filter.copy(symbol = symbol.takeUnless { it == "All" })
                it.filterChanged()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}