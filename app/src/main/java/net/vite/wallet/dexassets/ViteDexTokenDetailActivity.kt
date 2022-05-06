package net.vite.wallet.dexassets

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.balance.DexAccountFundInfoPoll
import net.vite.wallet.balance.InternalTransferActivity
import net.vite.wallet.balance.crosschain.CrosschainNoticeActivity
import net.vite.wallet.balance.crosschain.GwCrosschainVM
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.balance.quota.QuotaPledgeInfoPoll
import net.vite.wallet.balance.txsend.ViteTxActivity
import net.vite.wallet.dexassets.widget.DexDetailGroupButton
import net.vite.wallet.dexassets.widget.StartTextEndTextRow
import net.vite.wallet.dialog.ChooseTradePairDialog
import net.vite.wallet.exchange.MarketVM
import net.vite.wallet.exchange.TickerStatisticsCenter
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.gw.MetaInfoResp
import net.vite.wallet.network.http.vitex.TickerStatistics
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.rpc.ViteTokenInfo
import net.vite.wallet.network.rpc.VxTokenInfo
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.utils.dp2px
import net.vite.wallet.utils.showToast
import net.vite.wallet.utils.viewbinding.findMyView
import net.vite.wallet.widget.TokenIconWidget
import java.math.BigInteger

class ViteDexTokenDetailActivity : UnchangableAccountAwareActivity() {


    val normalTokenInfo by lazy {
        val tokenCode = intent.getStringExtra("tokenCode")!!
        TokenInfoCenter.getTokenInfoInCache(tokenCode) ?: throw Exception("empty tokeninfo")
    }
    val keyValueContainer by findMyView<LinearLayout>(R.id.keyValueContainer)
    val radioButtonsGroup by findMyView<View>(R.id.radioButtonsGroup)
    val listContainer by findMyView<View>(R.id.listContainer)
    val tabAppbar by findMyView<AppBarLayout>(R.id.tabAppbar)
    val isViteVite by lazy {
        normalTokenInfo.tokenAddress == ViteTokenInfo.tokenId
    }
    val isViteVx by lazy {
        normalTokenInfo.tokenAddress == VxTokenInfo.tokenId
    }

    val isDex by lazy {
        intent.getBooleanExtra("isViteX", false)
    }

    val pageListVM by viewModels<DexTxListViewModel>()
    val balanceVM by viewModels<BalanceViewModel>()
    val crosschainVM by viewModels<GwCrosschainVM>()
    val marketVm by viewModels<MarketVM>()
    val dexAssetsDetailsViewModel by viewModels<DexAssetsDetailsViewModel>()

    val cycleProgressBar by findMyView<View>(R.id.cycleProgressBar)
    val list by findMyView<RecyclerView>(R.id.list)
    val emptyGroup by findMyView<View>(R.id.emptyGroup)
    val swipeRefresh by findMyView<SwipeRefreshLayout>(R.id.swipeRefresh)

    override fun init() {
        setContentView(R.layout.activity_dex_token_detail)
        val pageAdapter = DexItemTxAdapter()
        list.adapter = pageAdapter
        listContainer.post {
            pageAdapter.bottomHeight = radioButtonsGroup.height + 1.0F.dp2px().toInt()
        }

        tabAppbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            swipeRefresh.isEnabled = verticalOffset >= 0
        })
        pageListVM.networkState.observe(this) {
            swipeRefresh.isRefreshing = it == NetworkState.LOADING
            if (it.isError()) {
                showToast(it.tryGetErrorMsg(this))
            }
        }
        pageListVM.emptyListLd.observe(this) {
            emptyGroup.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        pageListVM.pagedListLd.observe(this) {
            if (it.isNotEmpty()) {
                emptyGroup.visibility = View.GONE
            }
            pageAdapter.submitList(it)
        }

        balanceVM.dexAccountFundInfo.observe(this) {
            updateOverview()
        }

        swipeRefresh.setOnRefreshListener {
            pageListVM.refresh()
            refreshExceptList()
        }

        crosschainVM.metaInfoLd.observe(this) {
            cycleProgressBar.visibility = it.progressVisible
            if (it.isLoading()) {
                return@observe
            }
            if (it.isError()) {
                it.networkState.showBaseErrorDialog(this)
                return@observe
            }

            val tokenCode = it.metaData
            if (tokenCode == null || tokenCode !is String) {
                return@observe
            }

            if (it.requestCode == DEPOSIT) {
                if (it.resp?.depositState == MetaInfoResp.OPEN) {
                    CrosschainNoticeActivity.show(
                        this,
                        tokenCode,
                        CrosschainNoticeActivity.REQUEST_DEPOSIT
                    )
                } else {
                    it.resp?.showCheckDialog(this) {
                        it.dismiss()
                    }
                }
            }

            if (it.requestCode == WITHDRAW) {
                if (it.resp?.withdrawState == MetaInfoResp.OPEN) {
                    CrosschainNoticeActivity.show(
                        this,
                        tokenCode,
                        CrosschainNoticeActivity.REQUEST_WITHDRAW
                    )
                } else {
                    it.resp?.showCheckDialog(this) {
                        it.dismiss()
                    }
                }
            }
        }

        balanceVM.rtPledgeTotalAmount.observe(this) {
            updateOverview()
        }

        pageListVM.initLoad(
            currentAccount.nowViteAddress()!!,
            normalTokenInfo.tokenAddress!!,
            isDex
        )

        dexAssetsDetailsViewModel.rtPrice.observe(this) {
            updateOverview()
        }

        dexAssetsDetailsViewModel.vipStakeForSBPLD.observe(this) {
            updateOverview()
        }

        dexAssetsDetailsViewModel.vipStakeInfoListLD.observe(this) {
            updateOverview()
        }

        dexAssetsDetailsViewModel.miningStakingAmountLD.observe(this) {
            updateOverview()
        }

        dexAssetsDetailsViewModel.vipStakeForFullNodeLD.observe(this) {
            updateOverview()
        }


        updateOverview()
        setBottomRadioButtons()
    }

    val dexDeposit by findMyView<DexDetailGroupButton>(R.id.dexDeposit)
    val dexWithdraw by findMyView<DexDetailGroupButton>(R.id.dexWithdraw)
    val dexTransfer by findMyView<DexDetailGroupButton>(R.id.dexTransfer)
    val dexInnerTransfer by findMyView<DexDetailGroupButton>(R.id.dexInnerTransfer)
    val dexExchange by findMyView<DexDetailGroupButton>(R.id.dexExchange)
    fun setBottomRadioButtons() {
        dexDeposit.setImage(R.mipmap.dex_token_detail_deposit)
        dexWithdraw.setImage(R.mipmap.dex_token_detail_withdraw)
        dexTransfer.setImage(R.mipmap.dex_token_detail_transfer)
        dexInnerTransfer.setImage(R.mipmap.dex_token_detail_inner_transfer)
        dexDeposit.setText(R.string.dex_token_detail_deposit)
        dexWithdraw.setText(R.string.dex_token_detail_withdraw)
        dexTransfer.setText(R.string.dex_token_detail_transfer)
        dexInnerTransfer.setText(R.string.dex_token_detail_inner_transfer)
        dexExchange.setText(R.string.dex_token_detail_exchange)
        dexExchange.setImage(R.mipmap.dex_token_detail_exchange)
        dexTransfer.visibility = View.VISIBLE


        if (isDex) {
            dexExchange.visibility = View.VISIBLE
            dexInnerTransfer.visibility = View.GONE
            dexDeposit.visibility = View.GONE
            dexWithdraw.visibility = View.GONE
        } else {
            dexExchange.visibility = View.GONE
            if (normalTokenInfo.isGatewayToken()) {
                dexDeposit.visibility = View.VISIBLE
                dexWithdraw.visibility = View.VISIBLE
                dexInnerTransfer.visibility = View.VISIBLE
            } else {
                dexDeposit.visibility = View.GONE
                dexWithdraw.visibility = View.GONE
                dexInnerTransfer.visibility = View.VISIBLE
            }

        }

        dexDeposit.setOnClickListener {
            CrosschainNoticeActivity.show(
                this,
                normalTokenInfo.tokenCode!!,
                CrosschainNoticeActivity.REQUEST_DEPOSIT
            )
        }

        dexWithdraw.setOnClickListener {
            CrosschainNoticeActivity.show(
                this,
                normalTokenInfo.tokenCode!!,
                CrosschainNoticeActivity.REQUEST_WITHDRAW
            )
        }

        dexInnerTransfer.setOnClickListener {
            ViteTxActivity.show(
                this,
                normalTokenInfo.tokenCode!!
            )
        }
        dexExchange.setOnClickListener {
            val tickers = TickerStatisticsCenter.httpCategoryCacheMap.values.flatten().filter {
                it.quoteTokenSymbol == normalTokenInfo.uniqueName() || it.tradeTokenSymbol == normalTokenInfo.uniqueName()
            }.sortedBy { it.symbol }

            ChooseTradePairDialog(this, tickers) { t: TickerStatistics, _ ->
                setResult(RESULT_OK, Intent().apply { putExtra("symbol", t.symbol) })
                finish()
            }.show()

        }
        dexTransfer.setOnClickListener {
            InternalTransferActivity.show(this, normalTokenInfo.tokenAddress!!)
        }
    }

    val tokenWidget by findMyView<TokenIconWidget>(R.id.tokenWidget)
    val tokenName by findMyView<TextView>(R.id.tokenName)
    val allAssetAmount by findMyView<TextView>(R.id.allAssetAmount)
    val allAssetAmountValue by findMyView<TextView>(R.id.allAssetAmountValue)

    fun addKeyValueTextRow(@StringRes key: Int, value: CharSequence) {
        keyValueContainer.addView(
            StartTextEndTextRow(this).apply {
                setKey(key)
                setValue(value)
            }
        )
    }

    fun updateOverview() {
        keyValueContainer.removeAllViews()
        with(normalTokenInfo) {
            tokenWidget.setup(icon, false)
            tokenName.text = uniqueName()

        }

        allAssetAmount.text = if (isDex) {
            normalTokenInfo.dexAllBalance()
        } else {
            normalTokenInfo.walletTotalBalance()
        }.toLocalReadableText(8)

        allAssetAmountValue.text = if (isDex) {
            normalTokenInfo.dexAllBalanceValue()
        } else {
            normalTokenInfo.walletTotalBalanceValue()
        }.toCurrencyText(needApproximate = true)
        if (isDex) {
            addKeyValueTextRow(
                R.string.dex_avaliable_balance,
                normalTokenInfo.dexAvailableBalance().toLocalReadableText(8)
            )
            addKeyValueTextRow(
                R.string.dex_order_locked_balance,
                normalTokenInfo.dexLockedBalance().toLocalReadableText(8)
            )
            if (isViteVite) {
                addKeyValueTextRow(
                    R.string.dex_vip_lock,
                    ViteTokenInfo.amountText(
                        ViteAccountInfoPoll.myLatestViteAccountInfo()?.stakeForDexVip
                            ?: BigInteger.ZERO,
                        8
                    )
                )
                addKeyValueTextRow(
                    R.string.dex_mine_lock,
                    ViteTokenInfo.amountText(
                        ViteAccountInfoPoll.myLatestViteAccountInfo()?.stakeForDexMining
                            ?: BigInteger.ZERO,
                        8
                    )
                )
                addKeyValueTextRow(
                    R.string.dex_unlocking,
                    ViteTokenInfo.amountText(
                        DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                            ?.get(ViteTokenInfo.tokenId)?.cancellingStake?.toBigIntegerOrNull()
                            ?: BigInteger.ZERO,
                        8
                    )
                )
            }
            if (isViteVx) {
                addKeyValueTextRow(
                    R.string.dex_dividen_lock,
                    VxTokenInfo.amountText(
                        DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                            ?.get(VxTokenInfo.tokenId)?.vxLocked?.toBigIntegerOrNull()
                            ?: BigInteger.ZERO,
                        8
                    )
                )
                addKeyValueTextRow(
                    R.string.dex_unlocking,
                    VxTokenInfo.amountText(
                        DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                            ?.get(VxTokenInfo.tokenId)?.vxUnlocking?.toBigIntegerOrNull()
                            ?: BigInteger.ZERO,
                        8
                    )
                )
            }
        } else if (isViteVite) {
            addKeyValueTextRow(R.string.dex_avaliable_balance, normalTokenInfo.balanceText(8))
            val pledgeInfo =
                QuotaPledgeInfoPoll.getLatestData(currentAccount.nowViteAddress()!!)
            addKeyValueTextRow(
                R.string.dex_asset_detail_quota_key, ViteTokenInfo.amountText(
                    pledgeInfo?.totalPledgeAmount?.toBigIntegerOrNull() ?: BigInteger.ZERO, 8
                )
            )
            addKeyValueTextRow(
                R.string.dex_asset_detail_quota_sbp_key,
                ViteTokenInfo.amountText(
                    ViteAccountInfoPoll.myLatestViteAccountInfo()?.stakeToSBPAll ?: BigInteger.ZERO,
                    8
                )
            )
            addKeyValueTextRow(
                R.string.dex_asset_detail_quota_full_node_key,
                ViteTokenInfo.amountText(
                    ViteAccountInfoPoll.myLatestViteAccountInfo()?.stateToFullNodeAll
                        ?: BigInteger.ZERO,
                    8
                )
            )
        }
    }

    fun refreshExceptList() {
        marketVm.getAll24HPriceChangeByCategory(false)
        marketVm.getAllTokenExchangeRate(false)
        currentAccount.nowViteAddress()?.let { addr ->
            dexAssetsDetailsViewModel.getStakeForFullNode(addr)
            dexAssetsDetailsViewModel.getStakeForSBP(addr)
            dexAssetsDetailsViewModel.dexGetVIPStakeInfoList(addr)
            dexAssetsDetailsViewModel.dexGetCurrentMiningStakingAmountByAddress(addr)
        }
    }

    override fun onStart() {
        super.onStart()
        refreshExceptList()
    }

    companion object {
        private const val DEPOSIT = 13521
        private const val WITHDRAW = 13531

        fun actionShow(fragment: Fragment, tokenCode: String, isVitex: Boolean, requestCode: Int) {
            fragment.startActivityForResult(
                Intent(
                    fragment.requireActivity(),
                    ViteDexTokenDetailActivity::class.java
                ).apply {
                    putExtra("tokenCode", tokenCode)
                    putExtra("isViteX", isVitex)
                }, requestCode
            )
        }
    }


    fun TextView.bindText(str: String) {
        this.visibility = View.VISIBLE
        this.text = str
    }

    fun TextView.bindText(@StringRes strRes: Int) {
        this.visibility = View.VISIBLE
        this.setText(strRes)
    }

}
