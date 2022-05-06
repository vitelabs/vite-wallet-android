package net.vite.wallet.buycoin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

import kotlinx.android.synthetic.main.fragment_buy_coin.*
import net.vite.wallet.*
import net.vite.wallet.activities.BaseTxSendFragment
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.buycoin.list.BuyCoinRecordActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.network.http.coinpurchase.CoinPurchaseExchangeReq
import net.vite.wallet.network.http.coinpurchase.ConvertRateResp
import net.vite.wallet.network.rpc.NormalTxParams
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.network.toLocalReadableTextWithThouands
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.showToast
import net.vite.wallet.widget.DecimalLimitTextWatcher
import java.math.BigDecimal
import java.math.RoundingMode

class BuyCoinFragment : BaseTxSendFragment() {
    private lateinit var coinPurchaseViewModel: CoinPurchaseViewModel
    private lateinit var balanceViewModel: BalanceViewModel

    private var convertRateResp: ConvertRateResp? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        coinPurchaseViewModel = ViewModelProviders.of(this)[CoinPurchaseViewModel::class.java]
        balanceViewModel = ViewModelProviders.of(this)[BalanceViewModel::class.java]

        coinPurchaseViewModel.exchangeLd.observe(this, Observer {
            if (it.isLoading()) {
                cycleProgressBar.show()
                return@Observer
            }
            cycleProgressBar.dismiss()

            if (it?.resp?.code == 0) {
                BuyCoinRecordActivity.show(currentAccount.nowViteAddress()!!, this.activity!!)
            } else {
                activity?.showToast(getString(R.string.bugcoin_failed, it?.resp?.code?.toString()))
            }
        })

        coinPurchaseViewModel.convertRateLd.observe(this, Observer {
            it?.resp?.let {
                refreshConvertRate(it)

                convertRateResp = it
            }
        })

        balanceViewModel.rtViteAccInfo.observe(this, Observer {
            refreshTokenBalance()
        })

        setupAmountInputEditText()
        setupBuyBtn()

        currentAccount.nowViteAddress()?.let {
            coinPurchaseViewModel.convertRate(it)
        }

        bugCoinRecord.setOnClickListener {
            BuyCoinRecordActivity.show(currentAccount.nowViteAddress()!!, activity!!)
        }

        infor4FastBuyCoin.setOnClickListener {
            startActivity(createBrowserIntent("https://vite-static-pages.netlify.com/exchange/en/exchange.html"))
        }

        refreshTokenBalance()

    }

    private fun refreshConvertRate(it: ConvertRateResp) {
        ethVitePrice.text =
            getString(R.string.buy_coin_price, it.rightRate?.toBigDecimal()?.toLocalReadableText(8) ?: "")
        oneDayLimit.text = getString(
            R.string.buy_coin_max_limit_oneday,
            it.quota?.quotaTotal?.toBigDecimal()?.toLocalReadableTextWithThouands(8) ?: "",
            it.quota?.quotaRest?.toBigDecimal()?.toBigInteger()?.toBigDecimal()?.toLocalReadableTextWithThouands() ?: ""
        )
        singleBugLimit.text = getString(
            R.string.buy_coin_max_limit,
            it.quota?.unitQuotaMin?.toBigDecimal()?.toLocalReadableTextWithThouands(8) ?: "",
            it.quota?.unitQuotaMax?.toBigDecimal()?.toLocalReadableTextWithThouands(8) ?: ""
        )
        viteAmountEditText.hint = getString(
            R.string.buy_coin_vite_amount_hint,
            it.quota?.unitQuotaMin?.toBigDecimal()?.toLocalReadableTextWithThouands(8) ?: "",
            it.quota?.unitQuotaMax?.toBigDecimal()?.toLocalReadableTextWithThouands(8) ?: ""
        )
    }

    override fun onTxEnd(status: TxEndStatus) {
        when (status) {
            is TxSendSuccess -> {
                status.powProfile?.let {
                    baseTxSendFlow.showPowProfileDialog(it, {})
                }
                coinPurchaseViewModel.exchange(
                    CoinPurchaseExchangeReq(
                        address = currentAccount.nowViteAddress()!!,
                        hash = status.accountBlock.hash!!
                    )
                )
            }
            is TxSendFailed -> status.throwable.showToast(activity!!)
        }

    }

    private fun buy() {
        val convertRateResp = convertRateResp ?: return
        val rate = convertRateResp.rightRate ?: return
        val ethAmount = ethAmountEditText.editableText?.toString()?.toBigDecimalOrNull() ?: return

        val viteAmount = ethAmount.divide(rate.toBigDecimal(), 8, RoundingMode.DOWN)

        if (viteAmount > convertRateResp.quota?.unitQuotaMax?.toBigDecimal() ?: BigDecimal.ZERO) {
            TextTitleNotifyDialog(this.activity!!).apply {
                setTitle(R.string.notice)
                setMessage(R.string.buy_coin_exceed_quota_max)
                setBottom(R.string.close)
                show()
            }
            return
        }

        if (viteAmount > convertRateResp.quota?.quotaRest?.toBigDecimal() ?: BigDecimal.ZERO) {
            TextTitleNotifyDialog(this.activity!!).apply {
                setTitle(R.string.notice)
                setMessage(R.string.buy_coin_exceed_daily_quota_max)
                setBottom(R.string.close)
                show()
            }
            return
        }
        if (viteAmount < convertRateResp.quota?.unitQuotaMin?.toBigDecimal() ?: BigDecimal.ZERO) {
            TextTitleNotifyDialog(this.activity!!).apply {
                setTitle(R.string.notice)
                setMessage(R.string.buy_coin_exceed_quota_max)
                setBottom(R.string.close)
                show()
            }
            return
        }


        val viteEthTokenInfo =
            TokenInfoCenter.getTokenInfoInCache(TokenInfoCenter.ViteEthTokenCodes.tokenCode) ?: return
        val ethAmountSu = viteEthTokenInfo.baseToSmallestUnit(ethAmount).toBigInteger()
        if (ethAmountSu > viteEthTokenInfo.balance()) {
            activity?.showToast(R.string.balance_not_enough)
            ethAmountEditText.requestFocus()
            return
        }

        lastSendParams = NormalTxParams(
            accountAddr = currentAccount.nowViteAddress()!!,
            toAddr = convertRateResp.storeAddress!!,
            tokenId = viteEthTokenInfo.tokenAddress!!,
            amountInSu = ethAmountSu,
            data = null
        )

        val dialogParams = BigDialog.Params(
            bigTitle = getString(R.string.pay),
            amount = ethAmountEditText.editableText.toString(),
            secondTitle = getString(R.string.receive_address),
            tokenSymbol = viteEthTokenInfo.symbol!!,
            secondValue = convertRateResp.storeAddress,
            firstButtonTxt = getString(R.string.confirm_pay),
            tokenIcon = viteEthTokenInfo.icon
        )
        verifyIdentity(dialogParams,
            { lastSendParams?.let { sendTx(it) } },
            {})

    }

    private fun setupBuyBtn() {
        buyBtn.setOnClickListener {

            buy()
        }
    }

    private fun setupAmountInputEditText() {
        val viteEthTokenInfo =
            TokenInfoCenter.getTokenInfoInCache(TokenInfoCenter.ViteEthTokenCodes.tokenCode) ?: return
        ethCoinIcon.setup(
            viteEthTokenInfo.icon
        )
        ethCoinIcon.setTokenFooterSize(13.0F)
        ethAmountEditText.addTextChangedListener(DecimalLimitTextWatcher(viteEthTokenInfo.decimal!!))
        ethAmountEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!ethAmountEditText.isFocused) {
                    return
                }
                val ethAmount = s?.toString()?.toBigDecimalOrNull() ?: kotlin.run {
                    viteAmountEditText.setText("")
                    return
                }
                val rate = convertRateResp?.rightRate ?: return
                val ethBaseAmountText =
                    (ethAmount.divide(rate.toBigDecimal(), 8, RoundingMode.DOWN)).toLocalReadableText(8)
                viteAmountEditText.setText(ethBaseAmountText)
            }
        })


        val viteViteTokenInfo =
            TokenInfoCenter.getTokenInfoInCache(TokenInfoCenter.ViteViteTokenCodes.tokenCode) ?: return
        viteCoinIcon.setup(
            viteViteTokenInfo.icon
        )
        viteCoinIcon.setTokenFooterSize(13.0F)
        viteAmountEditText.addTextChangedListener(DecimalLimitTextWatcher(viteViteTokenInfo.decimal!!))
        viteAmountEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!viteAmountEditText.isFocused) {
                    return
                }
                val viteAmount = s?.toString()?.toBigDecimalOrNull() ?: kotlin.run {
                    ethAmountEditText.setText("")
                    return
                }
                val rate = convertRateResp?.rightRate ?: return
                val ethBaseAmountText = (viteAmount * rate.toBigDecimal()).toLocalReadableText(8)
                ethAmountEditText.setText(ethBaseAmountText)
            }

        })

    }


    private fun refreshTokenBalance() {
        ethCoinBalance.text =
            TokenInfoCenter.getTokenInfoInCache(TokenInfoCenter.ViteEthTokenCodes.tokenCode)?.balanceText(8)
        viteCoinBalance.text =
            TokenInfoCenter.getTokenInfoInCache(TokenInfoCenter.ViteViteTokenCodes.tokenCode)?.balanceText(8)
    }


    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            refreshTokenBalance()

            currentAccount.nowViteAddress()?.let {
                coinPurchaseViewModel.convertRate(it)
            }

            convertRateResp?.let {
                refreshConvertRate(it)
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_buy_coin, container, false)
    }


}