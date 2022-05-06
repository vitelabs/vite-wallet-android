package net.vite.wallet.balance.crosschain.deposit.list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_crosschain_deposit_tx_list.*
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.crosschain.addMappedToken
import net.vite.wallet.balance.crosschain.setMappedTokenSelected
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.vitex.NormalTokenInfo


class DepositRecordsActivity : UnchangableAccountAwareActivity() {
    companion object {
        fun show(context: Context, tokenId: String, address: String, gwUrl: String) {
            context.startActivity(Intent(context, DepositRecordsActivity::class.java).apply {
                putExtra("address", address)
                putExtra("tokenId", tokenId)
                putExtra("gwUrl", gwUrl)
            })
        }

        fun show4Result(
            activity: Activity,
            tokenId: String,
            address: String,
            gwUrl: String,
            requestCode: Int
        ) {
            activity.startActivityForResult(
                Intent(
                    activity,
                    DepositRecordsActivity::class.java
                ).apply {
                    putExtra("address", address)
                    putExtra("tokenId", tokenId)
                    putExtra("gwUrl", gwUrl)
                }, requestCode
            )
        }
    }

    private val vm by viewModels<DepositRecordsVm>()

    private val address by lazy {
        intent.getStringExtra("address")!!
    }

    private val tokenId by lazy { intent.getStringExtra("tokenId")!! }

    private val viteNormalTokenInfo: NormalTokenInfo by lazy {
        TokenInfoCenter.getTokenInfoIncacheByTokenAddr(tokenId)!!
    }

    private val adapter = DepositRecordsAdapter()
    private var gwTokenInfo: NormalTokenInfo = NormalTokenInfo.EMPTY
        set(value) {
            field = value
            adapter.gwTokenInfo = value
            chooseMainNetContentContainer.setMappedTokenSelected(value)
            vm.initLoad(address, tokenId, value.url!!)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crosschain_deposit_tx_list)

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

        viteNormalTokenInfo.allMappedToken().forEach { tokenInfo ->
            chooseMainNetContentContainer.addMappedToken(tokenInfo) { selectedTokenInfo ->
                gwTokenInfo = selectedTokenInfo
            }
        }

        val gwUrl = intent.getStringExtra("gwUrl")
        gwTokenInfo = viteNormalTokenInfo.allMappedToken().find { it.url == gwUrl }
            ?: viteNormalTokenInfo.allMappedToken().first()

        txMainList.adapter = adapter

    }
}