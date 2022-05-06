package net.vite.wallet.balance

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_balance.*
import net.vite.wallet.Once
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.ViteConfig
import net.vite.wallet.account.AccountAwareFragment
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.balance.tokenselect.TokenSearchVM
import net.vite.wallet.balance.tokenselect.TokenSelectActivity
import net.vite.wallet.balance.txsend.EthTxActivity
import net.vite.wallet.balance.txsend.ViteTxActivity
import net.vite.wallet.balance.walletconnect.ViteConnectActivity
import net.vite.wallet.balance.walletconnect.session.VCFsmHolder
import net.vite.wallet.balance.walletconnect.session.fsm.StateSessionEstablished
import net.vite.wallet.balance.walletconnect.session.fsm.StateUnconnected
import net.vite.wallet.balance.walletconnect.session.fsm.VCState
import net.vite.wallet.me.MeActivity
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.utils.showToast
import net.vite.wallet.vep.EthUrlTransferParams
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.vep.ViteUrlTransferParams


class BalanceFragment : AccountAwareFragment() {

    var currentViteAddr = ""
    var currentPinTk = ArrayList<String>()
    var lastTokenPriceRefreshTime = 0L
    var lastTokenDetailPullTime = 0L
    var isShowMoneyDetail = true

    val balanceViewModel by activityViewModels<BalanceViewModel>()
    val tokenSearchVM by activityViewModels<TokenSearchVM>()
    lateinit var pinnedTokenAdapter: PinnedTokenAdapter

    private val accountTokenManager = AccountCenter.getCurrentAccountTokenManager()

    private val viteUrlTransferParams: Once<ViteUrlTransferParams> = Once()
    private val ethUrlTransferParams: Once<EthUrlTransferParams> = Once()

    fun updateAssetTxt() {
        if (!isShowMoneyDetail) {
            viewMoney.setImageResource(R.mipmap.hide_detail)
            totalAssetsTxt.text = "****"
            totalAssetsBtcTxt.text = "****"
        } else {
            viewMoney.setImageResource(R.mipmap.view_detail)

            totalAssetsBtcTxt.text =
                accountTokenManager?.getPinnedExchangeTokenCodeBtcValues()?.toLocalReadableText(8)

            totalAssetsTxt.text =
                "≈${accountTokenManager?.getPinnedTokenCodeValues()?.toCurrencyText()}"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (accountTokenManager == null) {
            activity?.finish()
            return
        }

        meFragmentIcon.setOnClickListener {
            startActivity(Intent(requireContext(), MeActivity::class.java))
        }

        val updateListFunc = {
            pinnedTokenAdapter.notifyDataSetChanged()
            updateAssetTxt()
        }

        balanceViewModel.rtViteAccInfo.observe(viewLifecycleOwner, {
            updateListFunc()
        })
        balanceViewModel.rtEthAccInfo.observe(viewLifecycleOwner, {
            updateListFunc()
        })

        balanceViewModel.tokenInfosLd.observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                lastTokenDetailPullTime = System.currentTimeMillis()
            }
            updateListFunc()
        })

        balanceViewModel.tokenPriceLd.observe(viewLifecycleOwner, {
            lastTokenPriceRefreshTime = System.currentTimeMillis()
            updateListFunc()
        })

        balanceViewModel.dexAccountFundInfo.observe(viewLifecycleOwner, {
            updateListFunc()
        })

        balanceViewModel.rtPledgeTotalAmount.observe(viewLifecycleOwner, {
            updateListFunc()
        })

        tokenSearchVM.fuzzyQueryLd.observe(viewLifecycleOwner, Observer {
            progressCycle.visibility = it.progressVisible
            if (it.isLoading()) {
                return@Observer
            }
            if (it.resp.isNullOrEmpty()) {
                activity?.showToast(R.string.cannot_find_token)
            }
            val tokens = it.resp ?: return@Observer
            val token = tokens[0]
            TokenInfoCenter.addUserSearchToken(token)
            accountTokenManager.pinToken(token)
            if (token.family() == TokenFamily.VITE) {
                accountTokenManager.pinExchangeToken(token)
            }

            pinnedTokenAdapter.notifyDataSetChanged()
            when (token.family()) {
                TokenFamily.VITE -> viteUrlTransferParams.get()?.let {
                    ViteTxActivity.show(requireActivity(), it)
                }
                TokenFamily.ETH -> ethUrlTransferParams.get()?.let {
                    EthTxActivity.show(requireActivity(), it)
                }

            }
        })

        accountTokenManager.loadLastPinnedTokenCodesSync()

        balanceViewModel.loadAllCachedTokenInfo()
        balanceViewModel.loadAllCachedPrice()

        currentPinTk.clear()
        currentPinTk.addAll(accountTokenManager.getPinnedTokenCodes())

        isShowMoneyDetail =
            currentAccount.sharedPreferences()?.getBoolean("isShowMoneyDetail", true) ?: true


        val glide = Glide.with(requireActivity())
        pinnedTokenAdapter = PinnedTokenAdapter(accountTokenManager, glide)
        mainListWallet.adapter = pinnedTokenAdapter
        pinnedTokenAdapter.isShowMoneyDetail = isShowMoneyDetail

        updateAssetTxt()

        viewMoney.setOnClickListener {
            isShowMoneyDetail = !isShowMoneyDetail
            updateAssetTxt()
            pinnedTokenAdapter.isShowMoneyDetail = isShowMoneyDetail
            pinnedTokenAdapter.notifyDataSetChanged()

            currentAccount.sharedPreferences()?.edit()
                ?.putBoolean("isShowMoneyDetail", isShowMoneyDetail)?.apply()
        }


        addToken.setOnClickListener {
            startActivity(Intent(requireActivity(), TokenSelectActivity::class.java))
        }

        qrCodeScan.setOnClickListener {
            try {
                (activity as BaseActivity).scanQrcode()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            accountName.text = currentAccount.name
            if (System.currentTimeMillis() - lastTokenDetailPullTime > 2 * 60 * 1000) {
                balanceViewModel.batchQueryTokenDetail(currentPinTk)
            }
        }
    }

    fun onScanQrResult(urlParseResult: QrcodeParseResult) {
        val result = urlParseResult.result
        when (result) {
            is EthUrlTransferParams -> {
                if (result.contractAddress == null) {
                    EthTxActivity.show(requireActivity(), result)
                    return
                }
                val existTokenInfo =
                    TokenInfoCenter.getTokenInfoIncacheByTokenAddr(result.contractAddress!!)
                if (existTokenInfo == null) {
                    ethUrlTransferParams.set(result)
                    tokenSearchVM.fuzzyQuery(result.contractAddress!!)
                } else {
                    TokenInfoCenter.addUserSearchToken(existTokenInfo)
                    accountTokenManager?.pinToken(existTokenInfo)
                    if (existTokenInfo.family() == TokenFamily.VITE) {
                        accountTokenManager?.pinExchangeToken(existTokenInfo)
                    }
                    EthTxActivity.show(requireActivity(), result)
                }
            }

            is ViteUrlTransferParams -> {
                val existTokenInfo =
                    TokenInfoCenter.getTokenInfoIncacheByTokenAddr(result.tti)
                if (existTokenInfo == null) {
                    viteUrlTransferParams.set(result)
                    tokenSearchVM.fuzzyQuery(result.tti)
                } else {
                    TokenInfoCenter.addUserSearchToken(existTokenInfo)
                    accountTokenManager?.pinToken(existTokenInfo)
                    if (existTokenInfo.family() == TokenFamily.VITE) {
                        accountTokenManager?.pinExchangeToken(existTokenInfo)
                    }
                    ViteTxActivity.show(requireActivity(), result)
                }
            }
        }
    }


    fun wcSessionStatusAwareFun(state: VCState) {
        when (state) {
            is StateSessionEstablished -> {
                wcStatus.visibility = View.VISIBLE
                wcStatus.setText(R.string.wallet_connect_in)
                wcStatus.setOnClickListener {
                    ViteConnectActivity.show(requireActivity())
                }
            }
            is StateUnconnected -> {
                wcStatus.visibility = View.GONE
            }
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_balance, container, false)

    override fun onStart() {
        super.onStart()

        if (!ViteConfig.get().kvstore.getBoolean("has_clicked_exchange_mining", false)) {
            redDot.visibility = View.VISIBLE
        } else {
            redDot.visibility = View.GONE
        }

        if (VCFsmHolder.hasExistSession()) {
            wcStatus.visibility = View.VISIBLE
        } else {
            wcStatus.visibility = View.GONE
        }

        val nowPinCodes = accountTokenManager?.getPinnedTokenCodes() ?: kotlin.run {
            activity?.finish()
            return
        }

        if (!currentPinTk.containsAll(nowPinCodes) && nowPinCodes.containsAll(currentPinTk)) {
            currentPinTk.clear()
            currentPinTk.addAll(nowPinCodes)

            pinnedTokenAdapter.notifyDataSetChanged()
            balanceViewModel.refreshTokenPrice(currentPinTk)
        }

        if (System.currentTimeMillis() - lastTokenPriceRefreshTime > 60 * 1000) {
            balanceViewModel.refreshTokenPrice(currentPinTk)
        }

        if (System.currentTimeMillis() - lastTokenDetailPullTime > 2 * 60 * 1000) {
            balanceViewModel.batchQueryTokenDetail(currentPinTk)
        }

        currentAccount.apply {
            accountName.text = name
            val nowAddr = nowViteAddress()
            if (currentViteAddr != nowViteAddress()) {
                currentViteAddr = nowAddr ?: ""
                pinnedTokenAdapter.notifyDataSetChanged()
            }
        }
    }
}
