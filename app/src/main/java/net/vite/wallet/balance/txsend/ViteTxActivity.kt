package net.vite.wallet.balance.txsend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.Base64
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_transfer_vite.*
import net.vite.wallet.*
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.balance.quota.QuotaAndTxNumPoll
import net.vite.wallet.constants.DexCancelContractAddress
import net.vite.wallet.constants.DexContractAddress
import net.vite.wallet.contacts.readonly.ReadOnlyContactListActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.rpc.*
import net.vite.wallet.utils.hasNetWork
import net.vite.wallet.utils.hexToBytes
import net.vite.wallet.utils.showToast
import net.vite.wallet.utils.toHex
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.vep.ViteUrlTransferParams
import net.vite.wallet.widget.DecimalLimitTextWatcher
import net.vite.wallet.widget.RxTextWatcher
import org.vitelabs.mobile.Mobile
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class ViteTxActivity : BaseTxSendActivity() {

    var presetData: ByteArray? = null

    companion object {
        fun show(context: Context, params: ViteUrlTransferParams) {
            context.startActivity(Intent(context, ViteTxActivity::class.java).apply {
                putExtra("ViteUrlTransferParams", Gson().toJson(params))
            })
        }

        fun show(context: Context, tokenCode: String) {
            context.startActivity(Intent(context, ViteTxActivity::class.java).apply {
                putExtra("tokenCode", tokenCode)
            })
        }
    }

    private lateinit var balanceVm: BalanceViewModel
    private lateinit var normalTokenInfo: NormalTokenInfo
    private var disposable: Disposable? = null
    private var quotaCost: String? = null

    private fun setupExtraFunc() {
        scanBtn.setOnClickListener {
            scanQrcode()
        }

        myAddressBtn.setOnClickListener {
            ReadOnlyContactListActivity.show(this, ReadOnlyContactListActivity.MY_ADDRESS, 1001)
        }
        contactBtn.setOnClickListener {
            ReadOnlyContactListActivity.show(this, ReadOnlyContactListActivity.VITE_CONTACT, 1001)
        }

        toAddrEditTxt.onClickRightDrawableListener = {
            transferToExtra.visibility = if (transferToExtra.visibility == View.GONE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    fun setupTopLayout() {
        tokenShowNameTxt.text =
            getString(R.string.transfer_token_with_coin, normalTokenInfo.uniqueName())
        tokenWidget.setup(normalTokenInfo.icon)
    }

    override fun onTxEnd(status: TxEndStatus) {
        when (status) {
            is TxSendSuccess -> {
                status.powProfile?.let {
                    showPowProfileDialog(it) {
                        finish()
                    }
                } ?: kotlin.run {
                    baseTxSendFlow.showBaseTxSendSuccessDialog {
                        finish()
                    }
                }
                GlobalKVCache.store("needRefreshViteList" to "1")
            }
            is TxSendFailed -> status.throwable.showToast(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_vite)
        val tokenCode = intent.getStringExtra("tokenCode")
        normalTokenInfo = if (tokenCode == null) {
            val v = intent.getStringExtra("ViteUrlTransferParams")
            if (v == null) {
                showToast("not passed any value")
                finish()
                return
            }
            val viteUrlTransferParams = Gson().fromJson<ViteUrlTransferParams>(
                v,
                ViteUrlTransferParams::class.java
            )
            initPreSetSendParams(viteUrlTransferParams)

            TokenInfoCenter.getTokenInfoIncacheByTokenAddr(viteUrlTransferParams.tti)
        } else {
            TokenInfoCenter.getTokenInfoInCache(tokenCode)
        } ?: kotlin.run {
            showToast("empty token info")
            finish()
            return
        }

        setupTopLayout()
        setupExtraFunc()
        setupUserProfile()

        balanceVm = ViewModelProviders.of(this)[BalanceViewModel::class.java]
        balanceVm.apply {
            rtViteAccInfo
                .observe(this@ViteTxActivity, Observer<ViteAccountInfo> {
                    myBalance.text = normalTokenInfo.balanceText(8)
                })

            rtQuotaAndTxNum.observe(this@ViteTxActivity, Observer<GetPledgeQuotaResp> {
                myQuota.text = "${it.currentUt} /${it.utpe} Quota"
            })
        }

        baseTxSendFlow.txSendViewModel.calcQuotaRequiredLd.observe(this, Observer {
            quotaCost = it.resp?.utRequired
            quotaUseWidget.setQuotaNeedCost(quotaCost ?: "--")
        })



        setupMoneyEditText()
        setupInputAddrEditTxt()
        setupRemarkEditTxt()

        transferBtn.setOnClickListener {
            if (!hasNetWork()) {
                showToast(R.string.net_work_error)
                return@setOnClickListener
            }

            val inputAddrTxt = toAddrEditTxt.editableText.toString()
            if (!Mobile.isValidAddress(inputAddrTxt)) {
                toAddrEditTxt.requestFocus()
                showToast(R.string.input_address_not_valid)
                return@setOnClickListener
            }

            if (inputAddrTxt == DexContractAddress || inputAddrTxt == DexCancelContractAddress) {
                toAddrEditTxt.requestFocus()
                showToast(R.string.input_address_not_valid)
                return@setOnClickListener
            }


            val amountTxt = moneyAmountEditTxt.editableText.toString()

            val sendAmountInBase = amountTxt.toBigDecimalOrNull() ?: kotlin.run {
                moneyAmountEditTxt.requestFocus()
                showToast(R.string.please_input_correct_amount)
                return@setOnClickListener
            }
            val sendAmountInSu = normalTokenInfo.baseToSmallestUnit(sendAmountInBase)

            if (sendAmountInSu > normalTokenInfo.balance().toBigDecimal()) {
                moneyAmountEditTxt.requestFocus()
                showToast(R.string.balance_not_enough)
                return@setOnClickListener
            }

            val remarkStr = remarkEditTxt.editableText.toString()

            val data = if (presetData != null) {
                Base64.encodeToString(presetData!!, Base64.NO_WRAP)
            } else if (Mobile.isContactAddress(inputAddrTxt)) {
                if (remarkStr.length.rem(2) != 0) {
                    remarkEditTxt.requestFocus()
                    showToast(R.string.remark_contract_invalid)
                    return@setOnClickListener
                }
                try {
                    if (remarkStr.isEmpty()) {
                        null
                    } else {
                        Base64.encodeToString(remarkStr.hexToBytes(), Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    remarkEditTxt.requestFocus()
                    showToast(R.string.remark_contract_invalid)
                    return@setOnClickListener
                }
            } else {
                val remarkByte = remarkStr.toByteArray()
                if (remarkByte.size > 120) {
                    remarkEditTxt.requestFocus()
                    showToast(R.string.remark_text_too_long)
                    return@setOnClickListener
                }
                if (remarkByte.isEmpty()) {
                    null
                } else {
                    Base64.encodeToString(
                        remarkStr.toByteArray(), Base64.NO_WRAP
                    )
                }
            }

            lastSendParams = NormalTxParams(
                accountAddr = currentAccount.nowViteAddress()!!,
                toAddr = inputAddrTxt,
                tokenId = normalTokenInfo.tokenAddress!!,
                amountInSu = sendAmountInSu.toBigInteger(),
                data = data
            )

            val dialogParams = BigDialog.Params(
                bigTitle = getString(R.string.pay),
                amount = amountTxt.toBigDecimal().stripTrailingZeros().toPlainString(),
                secondTitle = getString(R.string.receive_address),
                tokenSymbol = normalTokenInfo.symbol!!,
                secondValue = inputAddrTxt,
                firstButtonTxt = getString(R.string.confirm_pay),
                tokenIcon = normalTokenInfo.icon,
                quotaCost = quotaCost ?: "--"
            )
            verifyIdentity(dialogParams,
                { lastSendParams?.let { sendTx(it) } },
                {})

        }
    }

    private fun setupInputAddrEditTxt() {
        toAddrEditTxt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.toString()?.let {
                    if (Mobile.isValidAddress(it)) {
                        requestQuotaUsed("")
                    }
                }
            }
        })

    }

    private fun requestQuotaUsed(remarkStr: String) {
        val inputAddrTxt = toAddrEditTxt.editableText.toString()
        val encodedData = if (presetData != null) {
            Base64.encodeToString(presetData!!, Base64.NO_WRAP)
        } else if (Mobile.isContactAddress(inputAddrTxt)) {
            try {
                Base64.encodeToString(remarkStr.hexToBytes(), Base64.NO_WRAP)
            } catch (e: Exception) {
                null
            }
        } else {
            val remarkByte = remarkStr.toByteArray()
            if (remarkByte.isEmpty()) {
                null
            } else {
                Base64.encodeToString(remarkStr.toByteArray(), Base64.NO_WRAP)
            }
        }


        baseTxSendFlow.txSendViewModel.calcQuotaRequired(
            CalcQuotaRequiredReq(
                selfAddr = currentAccount.nowViteAddress()!!,
                blockType = AccountBlock.BlockTypeSendCall,
                toAddr = inputAddrTxt,
                data = encodedData ?: ""
            )
        )
    }

    private fun setupRemarkEditTxt() {
        disposable = RxTextWatcher.onTextChanged(remarkEditTxt) { s ->
            val inputAddrTxt = toAddrEditTxt.editableText.toString()
            Mobile.isValidAddress(inputAddrTxt)
        }.debounce(500, TimeUnit.MILLISECONDS)
            .subscribe { remarkStr ->
                requestQuotaUsed(remarkStr)
            }

    }


    private fun setupMoneyEditText() {
        moneyAmountEditTxt.addTextChangedListener(DecimalLimitTextWatcher(normalTokenInfo.decimal!!))
        moneyAmountEditTxt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.toString()?.toBigDecimalOrNull()?.multiply(
                    ExternalPriceCenter.getPrice(normalTokenInfo.tokenCode ?: "")
                )?.toCurrencyText(2)?.let {
                    transferValueTxt.text = it
                    transferValueTxt.visibility = View.VISIBLE
                } ?: kotlin.run {
                    transferValueTxt.visibility = View.GONE
                }

            }
        })
        if (normalTokenInfo.decimal == 0) {
            moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789")
        } else {
            moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789.")
        }
    }


    private fun initPreSetSendParams(p: ViteUrlTransferParams) {
        p.amount?.let {
            moneyAmountEditTxt.setText(it)
            moneyAmountEditTxt.isEnabled = false
        }
        p.toAddr.let {
            toAddrEditTxt.setText(it)
            toAddrEditTxt.isEnabled = false
        }
        p.data?.let { data ->
            presetData = data
            val showString = if (Mobile.isContactAddress(p.toAddr)) {
                data.toHex()
            } else {
                try {
                    String(data, Charset.forName("UTF-8"))
                } catch (e: Exception) {
                    data.toHex()
                }
            }
            remarkEditTxt.setText(showString)
            remarkEditTxt.isEnabled = false
        }
        requestQuotaUsed(remarkEditTxt.editableText.toString())
    }

    private fun setupUserProfile() {
        myAddressName.text = currentAccount.getAddressName(currentAccount.nowViteAddress()!!)
        myAddress.text = currentAccount.nowViteAddress()!!
        myBalance.text = normalTokenInfo.balanceText(8)
        moneyAmountEditTxt.customRightText = getString(R.string.all)
        moneyAmountEditTxt.colorStr = "#007AFF"
        moneyAmountEditTxt.setOnRightTextClickListener {
            moneyAmountEditTxt.setText(normalTokenInfo.balanceText(8))
        }
//        moneyAmountEditTxt.customRightText = normalTokenInfo.symbol ?: ""
        QuotaAndTxNumPoll.getLatestData(currentAccount.nowViteAddress()!!)?.let {
            myQuota.text = "${it.currentUt} Quota/${it.utpe} Quota"
        }
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        transferToExtra.visibility = View.GONE
        if (qrcodeParseResult.result is ViteUrlTransferParams) {
            toAddrEditTxt.setText(qrcodeParseResult.result.toAddr)
        } else if (Mobile.isValidAddress(qrcodeParseResult.rawData)) {
            toAddrEditTxt.setText(qrcodeParseResult.rawData)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        transferToExtra.visibility = View.GONE
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra("address")?.let {
                toAddrEditTxt.setText(it)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
