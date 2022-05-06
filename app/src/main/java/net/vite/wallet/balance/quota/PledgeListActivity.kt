package net.vite.wallet.balance.quota

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_pledge_list.*
import net.vite.wallet.R
import net.vite.wallet.TxEndStatus
import net.vite.wallet.TxSendSuccess
import net.vite.wallet.abi.datatypes.generated.Bytes32
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.constants.BlockDetailTypeCancelPledge
import net.vite.wallet.constants.BlockTypeToContactAddress
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.rpc.BuildInContractEncoder
import net.vite.wallet.network.rpc.NormalTxParams
import net.vite.wallet.network.rpc.PledgeInfo
import net.vite.wallet.network.rpc.ViteTokenInfo
import net.vite.wallet.utils.hexToBytes
import java.math.BigInteger


class PledgeListActivity : BaseTxSendActivity() {

    private val listOfCancelPledging = ArrayList<String>()


    companion object {
        fun show(context: Context, addr: String) {
            context.startActivity(Intent(context, PledgeListActivity::class.java).apply {
                putExtra("address", addr)
            })
        }
    }

    private val vm by viewModels<PledgeListViewModel>()
    private lateinit var adapter: PledgeListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pledge_list)

        adapter = PledgeListAdapter(::onPledgeInfoRetrieve, listOfCancelPledging)
        mainList.adapter = adapter

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

    override fun onTxEnd(status: TxEndStatus) {
        if (status is TxSendSuccess) {
            status.accountBlock.data?.let {
                listOfCancelPledging.add(it)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun onPledgeInfoRetrieve(info: PledgeInfo) {
        val data = if (info.id == null) {
            BuildInContractEncoder.encodeCancelPledge(info.addr!!, info.amount!!)
        } else {
            BuildInContractEncoder.encodeCancelPledgeNew(Bytes32(info.id.hexToBytes()))
        }

        lastSendParams = NormalTxParams(
            accountAddr = currentAccount.nowViteAddress()!!,
            toAddr = BlockTypeToContactAddress[BlockDetailTypeCancelPledge]!!,
            tokenId = ViteTokenInfo.tokenId!!,
            amountInSu = BigInteger.ZERO,
            data = data
        )

        val amountTxt = ViteTokenInfo.amountText(info.amount!!, 8)

        val dialogParams = BigDialog.Params(
            bigTitle = getString(R.string.retrieve_quota),
            secondTitle = getString(R.string.quota_benifit_address),
            secondValue = info.addr,
            amount = amountTxt,
            tokenSymbol = ViteTokenInfo.symbol ?: "",
            firstButtonTxt = getString(R.string.confirm),
            quotaCost = "5"
        )
        verifyIdentity(dialogParams, {
            lastSendParams?.let { sendTx(it) }
        }, {})

    }
}
