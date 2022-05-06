package net.vite.wallet.buycoin.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_buy_coin_record.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.network.NetworkState

class BuyCoinRecordActivity : UnchangableAccountAwareActivity() {
    companion object {
        fun show(addr: String, context: Context) {
            context.startActivity(Intent(context, BuyCoinRecordActivity::class.java).apply {
                putExtra("address", addr)
            })
        }
    }

    private val vm by lazy {
        ViewModelProviders.of(this)[BuyCoinRecordsVm::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_coin_record)

        val address = intent.getStringExtra("address") ?: kotlin.run {
            finish()
            return
        }
        val adapter = BuyCoinRecordsAdapter()
        mainList.adapter = adapter


        vm.refreshState.observe(this, Observer<NetworkState> {
            swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })


        vm.emptyListLd.observe(this, Observer {
            emptyGroup.visibility = View.VISIBLE
        })

        vm.pagedListLd.observe(this, Observer {
            if (it.isNotEmpty()) {
                emptyGroup.visibility = View.GONE
            }
            adapter.submitList(it)
        })


        swipeRefresh.setOnRefreshListener {
            vm.refresh()
        }

        vm.initLoad(address)
    }
}