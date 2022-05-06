package net.vite.wallet.balance.ethtxlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_tx_list.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.network.NetworkState

class EthTxListActivity : UnchangableAccountAwareActivity() {

    companion object {
        fun show(context: Context, addr: String) {
            context.startActivity(Intent(context, EthTxListActivity::class.java).apply {
                putExtra("address", addr)
            })
        }
    }

    private val vm by lazy {
        ViewModelProviders.of(this)[EthTxListViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tx_list)
        val adapter = EthTxListAdapter()
        txMainList.adapter = adapter

        vm.refreshState.observe(this, Observer<NetworkState> {
            swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })

        vm.pagedListLd.observe(this, Observer {
            if (it.isNotEmpty()) {
                emptyGroup.visibility = View.GONE
            }
            adapter.submitList(it)
        })

        vm.emptyListLd.observe(this, Observer {
            emptyGroup.visibility = View.VISIBLE
        })

        swipeRefresh.setOnRefreshListener {
            vm.refresh()
        }

        vm.initLoad(intent.getStringExtra("address")!!)

    }
}
