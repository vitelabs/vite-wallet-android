package net.vite.wallet.balance.conversion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

import kotlinx.android.synthetic.main.activity_token_balance_detail_eth_vite.*
import kotlinx.android.synthetic.main.widget_banlance_tx_list.view.*
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.balance.TokenInfoDetailActivity
import net.vite.wallet.balance.ethtxlist.EthTxListAdapter
import net.vite.wallet.balance.ethtxlist.EthTxListViewModel
import net.vite.wallet.balance.quota.QuotaPledgeInfoPoll
import net.vite.wallet.balance.txsend.EthTxActivity
import net.vite.wallet.logi
import net.vite.wallet.me.EthAddressManagementActivity
import net.vite.wallet.me.QrShareActivity
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.rpc.EthAccountInfo
import net.vite.wallet.network.rpc.ViteTokenInfo
import net.vite.wallet.utils.showToast
import java.math.BigInteger


class EthViteTokenBalanceDetailActivity : UnchangableAccountAwareActivity() {

    private lateinit var balanceViewModel: BalanceViewModel
    private lateinit var txListViewModel: EthTxListViewModel
    private lateinit var normalTokenInfo: NormalTokenInfo
    private var nowAddr: String = ""

    companion object {
        fun show(context: Context) {
            context.startActivity(
                Intent(context, EthViteTokenBalanceDetailActivity::class.java)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_token_balance_detail_eth_vite)
        balanceViewModel = ViewModelProviders.of(this)[BalanceViewModel::class.java]
        txListViewModel = ViewModelProviders.of(this)[EthTxListViewModel::class.java]
        val tokenCode = TokenInfoCenter.EthViteTokenCodes.tokenCode
        normalTokenInfo = TokenInfoCenter.getTokenInfoInCache(tokenCode) ?: run {
            showToast("can not find tokenInfo with$tokenCode")
            finish()
            return
        }

        balanceViewModel.rtEthAccInfo.observe(this, Observer<EthAccountInfo> {
            it?.let {
                refreshBalanceDetailWidget()
            }
        })

        setupTxList()

        balanceDetailWidget.setCoinFamily(TokenFamily.ETH)
        balanceDetailWidget.setReceiveBtnOnClickListener {
            QrShareActivity.show(
                this@EthViteTokenBalanceDetailActivity, nowAddr, normalTokenInfo
            )
        }

        balanceDetailWidget.setTransferBtnOnClickListener {
            EthTxActivity.show(this@EthViteTokenBalanceDetailActivity, tokenCode)
        }

        exchangeViteWidget.isVisible = false
        exchangeViteWidget.setupSingleBtn(
            R.mipmap.convert_vite, R.string.convert_vite
        ) {

            EthViteConversionTxActivity.show(this)
        }

        txListWidget.setRefreshEnable(true)
//        txListWidget.showEmpty(true)
//        txListWidget.setEmpty(getString(R.string.review_tx_list)) {
//            startActivity(createBrowserIntent("${NetConfigHolder.netConfig.remoteEtherscanPrefix}/address/$nowAddr"))
//        }

        tokenSymbolTxt.text = normalTokenInfo.symbol ?: ""
        tokenNameTxt.text = normalTokenInfo.name ?: ""
        tokenWidget.setup(normalTokenInfo.icon)

        tokenWidget.setOnClickListener {
            TokenInfoDetailActivity.show(this, normalTokenInfo.tokenCode ?: "")
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
            }
            txListWidget.submitList(it)
        })

        txListViewModel.emptyListLd.observe(this, Observer {
            txListWidget.showEmpty(true)
            txListWidget.setEmpty(getString(R.string.empty_tx_list)) {}
        })

        txListWidget.setOnRefreshListener {
            txListViewModel.refresh()
        }
    }

    private fun refreshPledgeInfo() {
        QuotaPledgeInfoPoll.getLatestData(currentAccount.nowViteAddress()!!)?.let {
            balanceDetailWidget.setAlreadyPledgeAmount(
                ViteTokenInfo.amountText(
                    it.totalPledgeAmount?.toBigIntegerOrNull() ?: BigInteger.ZERO, 8
                )
            )
        }
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
            setAddressName(currentAccount.getAddressName(nowAddr) ?: "")
            setAddress(nowAddr)
            setBalanceAmount(normalTokenInfo.balanceText(8))
            setBalanceValue(normalTokenInfo.balanceValue().toCurrencyText(2))

            setAddressManagerOnClickListener {
                startActivity(
                    Intent(
                        this@EthViteTokenBalanceDetailActivity,
                        EthAddressManagementActivity::class.java
                    )
                )
            }
        }
    }

}