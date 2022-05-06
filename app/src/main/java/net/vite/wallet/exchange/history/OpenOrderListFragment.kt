package net.vite.wallet.exchange.history

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
import net.vite.wallet.R
import net.vite.wallet.account.AccountAwareFragment
import net.vite.wallet.utils.viewbinding.findMyView


class OpenOrderListFragment : AccountAwareFragment() {
    companion object {
        fun newInstance() = OpenOrderListFragment()
    }

    val recyclerView by findMyView<RecyclerView>(R.id.recyclerView)
    val emptyView by findMyView<View>(R.id.emptyGroup)
    private lateinit var adapter: OrdersAdapter

    val swipeRefresh by findMyView<SwipeRefreshLayout>(R.id.swipeRefresh)

    val vm by activityViewModels<OpenOrdersListViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_trade_history_list, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = OrdersAdapter()
        recyclerView.adapter = adapter
        vm.openOrders.observe(viewLifecycleOwner) {
            emptyView.isVisible = it.isEmpty() || it.last().orders.isEmpty()
            recyclerView.isVisible = !emptyView.isVisible
        }

        vm.openOrdersPageRequest.observe(viewLifecycleOwner) {
            swipeRefresh.isRefreshing = it.isLoading() && adapter.orders.isEmpty()
            if (it.isInit()) {
                adapter.orders.clear()
                adapter.notifyDataSetChanged()
                return@observe
            }
            val neworders = it.resp?.orders
            if (neworders?.isNotEmpty() == true) {
                adapter.orders.addAll(neworders)
                adapter.notifyItemRangeInserted(
                    adapter.orders.size - neworders.size,
                    neworders.size
                )
            }
        }

        recyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - 5) {
                    vm.getOpenOrders(currentAccount.nowViteAddress()!!)
                }
            }
        })

        swipeRefresh.setOnRefreshListener {
            vm.refreshOpenOrders(currentAccount.nowViteAddress()!!)
        }

        vm.refreshOpenOrders(currentAccount.nowViteAddress()!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.clearOnScrollListeners()
    }
}