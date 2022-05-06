package net.vite.wallet.balance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_token_balance_detail_eth.*
import kotlinx.android.synthetic.main.widget_banlance_tx_list.view.*
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.ethtxlist.EthTxListAdapter
import net.vite.wallet.balance.ethtxlist.EthTxListViewModel
import net.vite.wallet.balance.txsend.EthTxActivity
import net.vite.wallet.logi
import net.vite.wallet.me.EthAddressManagementActivity
import net.vite.wallet.me.QrShareActivity
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.rpc.EthAccountInfo
import net.vite.wallet.utils.showToast


class EthTokenBalanceDetailActivity : UnchangableAccountAwareActivity() {

    private lateinit var balanceViewModel: BalanceViewModel
    private lateinit var txListViewModel: EthTxListViewModel
    private lateinit var normalTokenInfo: NormalTokenInfo
    private var nowAddr: String = ""

    companion object {
        fun show(context: Context, tokenCode: String) {
            context.startActivity(
                Intent(context, EthTokenBalanceDetailActivity::class.java).putExtra(
                    "tokenCode", tokenCode
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AccountCenter.currentEthAddress() == null) {
            finish()
        } else {
            nowAddr = AccountCenter.currentEthAddress()!!
        }

        setContentView(R.layout.activity_token_balance_detail_eth)
        balanceViewModel = ViewModelProviders.of(this)[BalanceViewModel::class.java]
        txListViewModel = ViewModelProviders.of(this)[EthTxListViewModel::class.java]
        val tokenCode = intent.getStringExtra("tokenCode")!!
        normalTokenInfo = TokenInfoCenter.getTokenInfoInCache(tokenCode) ?: run {
            showToast("can not find tokenInfo with$tokenCode")
            finish()
            return
        }

        refreshBalanceDetailWidget()

        balanceViewModel.rtEthAccInfo.observe(this, Observer<EthAccountInfo> {
            it?.let {
                refreshBalanceDetailWidget()
            }
        })

        setupTxList()

        balanceDetailWidget.setCoinFamily(TokenFamily.ETH)
        balanceDetailWidget.setReceiveBtnOnClickListener {
            QrShareActivity.show(
                this@EthTokenBalanceDetailActivity, nowAddr, normalTokenInfo
            )
        }

        balanceDetailWidget.setTransferBtnOnClickListener {
            EthTxActivity.show(this@EthTokenBalanceDetailActivity, tokenCode)
        }

        txListWidget.setRefreshEnable(true)
//        txListWidget.showEmpty(true)
//        txListWidget.setEmpty(getString(R.string.review_tx_list)) {
//            startActivity(createBrowserIntent("${NetConfigHolder.netConfig.remoteEtherscanPrefix}/address/$nowAddr"))
//        }

        tokenWidget.setup(normalTokenInfo.icon)


        tokenSymbolTxt.text = normalTokenInfo.symbol ?: ""
        tokenNameTxt.text = normalTokenInfo.name ?: ""


        tokenWidget.setOnClickListener {
            TokenInfoDetailActivity.show(this, tokenCode)
        }


    }

    private fun setupTxList() {

        txListViewModel.networkState.observe(this, Observer {
            if (it.isError()) {
                txListWidget.showEmpty(true)
                logi(it.throwable?.message ?: "eth setupTxList error")
            }
        })

        txListViewModel.refreshState.observe(this, Observer<NetworkState> {
            txListWidget.swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })

        txListViewModel.pagedListLd.observe(this, Observer {
            if (it.isNotEmpty()) {
                txListWidget.showEmpty(false)
                txListWidget.setRefreshEnable(true)
                txListWidget.submitList(it)
            } else {
                txListWidget.showEmpty(true)
                txListWidget.setRefreshEnable(true)
                txListWidget.submitList(it)
            }
        })

        txListViewModel.emptyListLd.observe(this, Observer {
            txListWidget.showEmpty(true)
            txListWidget.setEmpty(getString(R.string.empty_tx_list)) {}
        })

        txListWidget.setOnRefreshListener {
            txListViewModel.refresh()
        }

        txListViewModel.initLoad(nowAddr, normalTokenInfo.tokenAddress ?: "")
    }

    override fun onStart() {
        super.onStart()

        val addr = currentAccount.nowEthAddress()
        if (nowAddr != addr) {
            nowAddr = currentAccount.nowEthAddress() ?: ""
            refreshBalanceDetailWidget()
            txListViewModel.initLoad(nowAddr, normalTokenInfo.tokenAddress ?: "")
            txListWidget.setNewAdapter(EthTxListAdapter())
        }
    }

    private fun refreshBalanceDetailWidget() {
        balanceDetailWidget.apply {
            setAddressName(currentAccount.getAddressName(nowAddr))
            setAddress(nowAddr)
            setBalanceAmount(normalTokenInfo.balanceText(8))
            setBalanceValue(normalTokenInfo.balanceValue().toCurrencyText(2))

            setAddressManagerOnClickListener {
                startActivity(
                    Intent(
                        this@EthTokenBalanceDetailActivity,
                        EthAddressManagementActivity::class.java
                    )
                )
            }
        }

    }

}