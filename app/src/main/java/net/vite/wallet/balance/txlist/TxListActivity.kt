package net.vite.wallet.balance.txlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_tx_list.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.network.NetworkState

class TxListActivity : UnchangableAccountAwareActivity() {

    companion object {
        fun show(context: Context, addr: String) {
            context.startActivity(Intent(context, TxListActivity::class.java).apply {
                putExtra("address", addr)
            })
        }
    }

    private val vm by viewModels<TxListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tx_list)
        val adapter = TxListAdapter()
        txMainList.adapter = adapter

        vm.refreshState.observe(this, {
            swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })

        vm.pagedListLd.observe(this, {
            if (it.isNotEmpty()) {
                emptyGroup.visibility = View.GONE
            }
            adapter.submitList(it)
        })

        vm.emptyListLd.observe(this, {
            emptyGroup.visibility = View.VISIBLE
        })

        swipeRefresh.setOnRefreshListener {
            vm.refresh()
        }

        vm.initLoad(intent.getStringExtra("address")!!)

    }
}
