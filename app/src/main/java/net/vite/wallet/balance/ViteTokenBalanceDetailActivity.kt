package net.vite.wallet.balance

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_token_balance_detail_vite.*
import kotlinx.android.synthetic.main.widget_banlance_tx_list.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.ViteApplication
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.crosschain.CrosschainNoticeActivity
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.balance.quota.PledgeQuotaActivity
import net.vite.wallet.balance.quota.QuotaAndTxNumPoll
import net.vite.wallet.balance.quota.QuotaPledgeInfoPoll
import net.vite.wallet.balance.txlist.TxListViewModel
import net.vite.wallet.balance.txsend.ViteTxActivity
import net.vite.wallet.balance.vote.VoteActivity
import net.vite.wallet.me.QrShareActivity
import net.vite.wallet.me.ViteAddressManagementActivity
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.rpc.ViteAccountInfo
import net.vite.wallet.network.rpc.ViteTokenInfo
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.showToast
import java.math.BigInteger

open class ViteTokenBalanceDetailActivity : UnchangableAccountAwareActivity() {

    private lateinit var normalTokenInfo: NormalTokenInfo

    private val balanceViewModel by viewModels<BalanceViewModel>()
    private val txListViewModel by viewModels<TxListViewModel>()

    private var isViteViteToken = false
    private var nowAddr: String = ""

    companion object {
        fun show(context: Context, tokenCode: String) {
            context.startActivity(
                Intent(
                    context,
                    ViteTokenBalanceDetailActivity::class.java
                ).apply {
                    putExtra("tokenCode", tokenCode)
                })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_token_balance_detail_vite)
        if (savedInstanceState != null) {
            (applicationContext as ViteApplication).restartApp()
            return
        }
        currentAccount = AccountCenter.getCurrentAccount()!!

        val tokenCode = intent.getStringExtra("tokenCode")!!
        normalTokenInfo = TokenInfoCenter.getTokenInfoInCache(tokenCode) ?: kotlin.run {
            showToast("can not find tokenInfo with$tokenCode")
            finish()
            return
        }

        isViteViteToken = normalTokenInfo.tokenCode == TokenInfoCenter.ViteViteTokenCodes.tokenCode

        balanceViewModel.rtViteAccInfo.observe(this, Observer<ViteAccountInfo> {
            it?.let {
                refreshBalanceDetailWidget()
            }
        })

        setupTxList()

        balanceDetailWidget.apply {
            setCoinFamily(TokenFamily.VITE)
            setTransferBtnOnClickListener {

                ViteTxActivity.show(
                    this@ViteTokenBalanceDetailActivity,
                    tokenCode
                )
            }

            setReceiveBtnOnClickListener {

                normalTokenInfo.tokenAddress?.let { _ ->
                    QrShareActivity.show(
                        this@ViteTokenBalanceDetailActivity, nowAddr, normalTokenInfo
                    )
                }
            }
        }
        normalTokenInfo.gatewayInfo?.let {
            gatewayName.setText(it.name ?: "")
            gatewayName.setTextColor(Color.parseColor("#FF007AFF"))
            gatewayName.visibility = View.VISIBLE
        } ?: kotlin.run {
            gatewayName.visibility = View.GONE
        }

        tokenWidget.setup(normalTokenInfo.icon)
        tokenWidget.setOnClickListener {
            TokenInfoDetailActivity.show(this, tokenCode)
        }

        tokenShowNameTxt.text = normalTokenInfo.uniqueName()
        tokenNameTxt.text = normalTokenInfo.name ?: ""


        balanceDetailQuotaWidget.visibility = View.VISIBLE
        balanceDetailQuotaWidget.getQuotaBtn.setOnClickListener {

            PledgeQuotaActivity.show(this@ViteTokenBalanceDetailActivity)
        }
        balanceViewModel.rtQuotaAndTxNum.observe(this, Observer {
            QuotaAndTxNumPoll.getLatestData(currentAccount.nowViteAddress()!!)?.let {
                balanceDetailQuotaWidget.setUtpeAndCurrent(it.utpe ?: "0", it.currentUt ?: "0")
            }
        })


        if (isViteViteToken) {
            balanceViewModel.rtPledgeTotalAmount.observe(this, Observer {
                refreshPledgeInfo()
            })
            balanceDetailExtraFuncWidget.visibility = View.VISIBLE
            balanceDetailExtraFuncWidget.setupSingleBtn(R.mipmap.vote_blue, R.string.vote) {

                startActivity(Intent(this@ViteTokenBalanceDetailActivity, VoteActivity::class.java))
            }
        } else if (normalTokenInfo.isGatewayToken()) {
            balanceDetailExtraFuncWidget.visibility = View.VISIBLE
            balanceDetailExtraFuncWidget.setupLeft(
                R.mipmap.crosschain_charge,
                R.string.crosschain_charge
            ) {
                CrosschainNoticeActivity.show(
                    this,
                    tokenCode,
                    CrosschainNoticeActivity.REQUEST_DEPOSIT
                )
            }

            balanceDetailExtraFuncWidget.setupRight(
                R.mipmap.crosschain_withdraw,
                R.string.crosschain_withdraw
            ) {
                CrosschainNoticeActivity.show(
                    this,
                    tokenCode,
                    CrosschainNoticeActivity.REQUEST_WITHDRAW
                )
            }


            gatewayName.setOnClickListener {
                GatewayInfoDetailActivity.show(
                    this, normalTokenInfo.tokenCode ?: ""
                )
            }

        } else {
            balanceDetailExtraFuncWidget.visibility = View.GONE
        }

        tokenWidget.setOnClickListener {
            TokenInfoDetailActivity.show(this, tokenCode)
        }

    }

    private fun setupTxList() {
        txListViewModel.networkState.observe(this, Observer {
            if (it.isError()) {
                txListWidget.showEmpty(true)
                txListWidget.setEmpty(getString(R.string.net_error_view_info)) {
                    startActivity(createBrowserIntent("${NetConfigHolder.netConfig.explorerUrlPrefix}/address/$nowAddr"))
                }
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
        val addr = currentAccount.nowViteAddress() ?: kotlin.run {
            finish()
            return
        }
        if (nowAddr != addr) {
            nowAddr = addr
            refreshBalanceDetailWidget()
            AccountCenter.dexInviteManager?.startBind()

            txListViewModel.initLoad(nowAddr, normalTokenInfo.tokenAddress ?: "")
        }
        if (isViteViteToken) {
            refreshPledgeInfo()
        }

        QuotaAndTxNumPoll.getLatestData(currentAccount.nowViteAddress()!!)?.let {
            balanceDetailQuotaWidget.setUtpeAndCurrent(it.utpe ?: "0", it.currentUt ?: "0")
        }
    }


    private fun refreshBalanceDetailWidget() {
        with(balanceDetailWidget) {
            setAddressManagerOnClickListener {

                startActivity(
                    Intent(
                        this@ViteTokenBalanceDetailActivity,
                        ViteAddressManagementActivity::class.java
                    )
                )
            }

            setAddressName(currentAccount.getAddressName(nowAddr) ?: "")
            setAddress(nowAddr)
            setBalanceAmount(normalTokenInfo.balanceText(8))
            setBalanceValue(normalTokenInfo.balanceValue().toCurrencyText(2))

            val unreceivedBalanceInfo =
                ViteAccountInfoPoll.latestViteAccountInfo[nowAddr]?.onroadBalance?.tokenBalanceInfoMap?.get(
                    normalTokenInfo.tokenAddress
                )
            setUnreceivedAmount(
                amount = unreceivedBalanceInfo?.let {
                    it.viteChainTokenInfo?.amountText(
                        it.totalAmount?.toBigIntegerOrNull() ?: BigInteger.ZERO, 4
                    )
                } ?: "0",
                txCount = unreceivedBalanceInfo?.number?.toIntOrNull() ?: 0
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (GlobalKVCache.read("needRefreshViteList") == "1") {
            GlobalScope.launch {
                delay(2000L)
                runOnUiThread {
                    txListViewModel.refresh()
                    GlobalKVCache.store("needRefreshViteList" to "0")
                }
            }
        }
    }
}
