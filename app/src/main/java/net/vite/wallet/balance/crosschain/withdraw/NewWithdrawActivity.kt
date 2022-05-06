package net.vite.wallet.balance.crosschain.withdraw

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.util.Base64
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_new_crosschain_eth_withdraw.*
import net.vite.wallet.*
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.balance.crosschain.GwCrosschainVM
import net.vite.wallet.balance.crosschain.addMappedToken
import net.vite.wallet.balance.crosschain.setMappedTokenSelected
import net.vite.wallet.balance.crosschain.withdraw.list.WithdrawRecordsActivity
import net.vite.wallet.contacts.readonly.ReadOnlyContactListActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.network.http.gw.GwCrosschainApi
import net.vite.wallet.network.http.gw.MetaInfoResp
import net.vite.wallet.network.http.gw.OverChainApiException
import net.vite.wallet.network.http.gw.WithdrawInfoResp
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.rpc.NormalTxParams
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.utils.*
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.widget.DecimalLimitTextWatcher
import net.vite.wallet.widget.RxTextWatcher
import java.math.BigDecimal
import java.util.concurrent.TimeUnit


class NewWithdrawActivity : BaseTxSendActivity() {

    companion object {
        fun show(context: Context, tokenCode: String) {
            context.startActivity(Intent(context, NewWithdrawActivity::class.java).apply {
                putExtra("tokenCode", tokenCode)
            })
        }

        const val ScanForAddress = 2342
        const val ScanForLabel = 2343
    }

    private val crosschainVM by viewModels<GwCrosschainVM>()
    private val balanceVm by viewModels<BalanceViewModel>()

    private val vitechainNormalTokenInfo by lazy {
        TokenInfoCenter.getTokenInfoInCache(intent.getStringExtra("tokenCode")!!)!!
    }

    private val allMappedTokenInfos by lazy {
        vitechainNormalTokenInfo.allMappedToken()
    }

    private var currentGwNormalTokenInfo: NormalTokenInfo = NormalTokenInfo.EMPTY
        set(value) {
            field = value
            chooseMainNetContentContainer.setMappedTokenSelected(value)
        }

    private val myViteaddress by lazy { currentAccount.nowViteAddress()!! }
    private val myEthaddress by lazy { currentAccount.nowEthAddress()!! }

    private var maximumWithdrawAmountInSu: BigDecimal? = null
    private var minimumWithdrawAmountInSu: BigDecimal? = null
    private var gatewayAddress: String? = null
    private var metaType: Int? = null
    private var feeInSu: BigDecimal? = null
    private var needLabelFlag: Boolean = false

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_crosschain_eth_withdraw)
        currentGwNormalTokenInfo = vitechainNormalTokenInfo.gatewayInfo?.mappedToken ?: run {
            finish()
            return
        }

        setupByCurrentMappedTokeninfo()

        setupTopLayout()
        setUpMoneyEditDisposable()
        setupMoneyAmountEditTxt()

        bindVmObserver()

        setupOtherClick()

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupByCurrentMappedTokeninfo() {
        val mappedTokenFamily = currentGwNormalTokenInfo.family()

        if (mappedTokenFamily != TokenFamily.ETH) {
            val drawable = resources.getDrawable(R.mipmap.scan_blue)
            drawable.setBounds(0, 0, drawable.minimumWidth, drawable.minimumHeight)
            toAddrEditTxt.setCompoundDrawables(null, null, drawable, null)
            toAddrEditTxt.onClickRightDrawableListener = {
                scanQrcode(ScanForAddress)
            }
        } else {
            val drawable = resources.getDrawable(R.mipmap.add)
            drawable.setBounds(0, 0, drawable.minimumWidth, drawable.minimumHeight)
            toAddrEditTxt.setCompoundDrawables(null, null, drawable, null)
            toAddrEditTxt.onClickRightDrawableListener = {
                transferToExtra.visibility = if (transferToExtra.visibility == View.GONE) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }

        myAddressBtn.setOnClickListener {
            transferToExtra.visibility = View.GONE
            toAddrEditTxt.setText(myEthaddress)
        }
        contactBtn.setOnClickListener {
            ReadOnlyContactListActivity.show(this, ReadOnlyContactListActivity.ETH_CONTACT, 1001)
        }
        scanBtn.setOnClickListener {
            scanQrcode(ScanForAddress)
        }
        addraddrLabelTxt.onClickRightDrawableListener = {
            scanQrcode(ScanForLabel)
        }
    }

    override fun onTxEnd(status: TxEndStatus) {
        when (status) {
            is TxSendSuccess -> {
                status.powProfile?.let {
                    showPowProfileDialog(it) {
                        WithdrawRecordsActivity.show(
                            this,
                            vitechainNormalTokenInfo.tokenAddress!!,
                            myViteaddress,
                            currentGwNormalTokenInfo.url!!
                        )
                        finish()
                    }
                } ?: run {
                    baseTxSendFlow.showBaseTxSendSuccessDialog {
                        WithdrawRecordsActivity.show(
                            this,
                            vitechainNormalTokenInfo.tokenAddress!!,
                            myViteaddress,
                            currentGwNormalTokenInfo.url!!
                        )
                        finish()
                    }
                }
            }
            is TxSendFailed -> status.throwable.showToast(this)
        }
    }

    private fun bindVmObserver() {
        balanceVm.rtViteAccInfo.observe(this) {
            myBalance.text = vitechainNormalTokenInfo.balanceText(8)
        }

        crosschainVM.withdrawFeeLd.observe(this) {
            if (it.isLoading()) {
                return@observe
            }
            if (it.isError()) {
                showToast(it.tryGetErrorMsg(this))
                return@observe
            }

            if (moneyAmountEditTxt.editableText.isNullOrEmpty()) {
                feeAmount.text = "0"
            } else {
                feeInSu = it.resp?.fee?.toBigDecimalOrNull()
                val feeInSuDecimal = feeInSu ?: return@observe
                feeAmount.text =
                    vitechainNormalTokenInfo.amountText(feeInSuDecimal.toBigInteger(), 8)
            }
        }

        crosschainVM.withdrawAddrVerifyAndGetFeeLd.observe(this, Observer {
            it.handleDialogShowOrDismiss(cycleProgressBar)
            if (it.isLoading()) {
                return@Observer
            }
            if (it.isError()) {
                showToast(it.tryGetErrorMsg(this))
                return@Observer
            }
            feeInSu = it.resp?.second?.fee?.toBigDecimalOrNull()
            feeInSu?.let {
                feeAmount.text =
                    vitechainNormalTokenInfo.smallestToBaseUnit(it).toLocalReadableText(5)
            }

            if (it.resp?.first?.isValidAddress != true) {
                toAddrEditTxt.requestFocus()
                showToast(R.string.input_address_not_valid)
                return@Observer
            }
            val toAddr = toAddrEditTxt.editableText.toString()

            val userInputInSu = checkAndGetAmountInSu() ?: return@Observer

            val feeInSu = feeInSu ?: return@Observer
            val sendAmountInSu = userInputInSu + feeInSu
            if (sendAmountInSu > vitechainNormalTokenInfo.balance().toBigDecimal()) {
                moneyAmountEditTxt.requestFocus()
                showToast(R.string.balance_not_enough)
                return@Observer
            }

            val targetViteAddr = gatewayAddress ?: return@Observer

            val metaType = metaType ?: kotlin.run {
                showToast("metaType null")
                return@Observer
            }
            val extraData = if (needLabelFlag) {
                val labelValue = addraddrLabelTxt.text.toString()
                "0bc3" + metaType.toByte().toHex() + toAddr.length.toByte()
                    .toHex() + toAddr.toByteArray().toHex() + labelValue.length.toByte()
                    .toHex() + labelValue.toByteArray().toHex()
            } else {
                "0bc3" + metaType.toByte().toHex() + toAddr.toByteArray().toHex()
            }

            lastSendParams = NormalTxParams(
                accountAddr = myViteaddress,
                toAddr = targetViteAddr,
                tokenId = vitechainNormalTokenInfo.tokenAddress!!,
                amountInSu = sendAmountInSu.toBigInteger(),
                data = Base64.encodeToString(extraData.hexToBytes(), Base64.NO_WRAP)

            )


            val dialogParams = BigDialog.Params(
                bigTitle = getString(R.string.pay),
                amount = vitechainNormalTokenInfo.smallestToBaseUnit(sendAmountInSu)
                    .stripTrailingZeros().toPlainString(),
                secondTitle = getString(R.string.receive_address),
                tokenSymbol = vitechainNormalTokenInfo.symbol ?: "",
                secondValue = targetViteAddr,
                firstButtonTxt = getString(R.string.confirm_pay),
                tokenIcon = vitechainNormalTokenInfo.icon
            )
            verifyIdentity(dialogParams, {
                sendTx(lastSendParams!!)
            }, {})
        })
    }

    private fun checkAndGetAmountInSu(): BigDecimal? {
        val maximumWithdrawAmountInSU = maximumWithdrawAmountInSu ?: return null
        val minimumWithdrawAmountInSU = minimumWithdrawAmountInSu ?: return null
        val amountTxt = moneyAmountEditTxt.editableText.toString()

        val userInputInBase = amountTxt.toBigDecimalOrNull() ?: kotlin.run {
            moneyAmountEditTxt.requestFocus()
            showToast(R.string.please_input_correct_amount)
            return null
        }
        val userInputInSu = vitechainNormalTokenInfo.baseToSmallestUnit(userInputInBase)
        if (userInputInSu < minimumWithdrawAmountInSU) {
            showToast(
                getString(
                    R.string.withdraw_amount_too_low,
                    vitechainNormalTokenInfo.amountTextWithSymbol(
                        minimumWithdrawAmountInSU.toBigInteger(),
                        3
                    )
                )
            )
            moneyAmountEditTxt.requestFocus()
            return null
        }

        if (userInputInSu > maximumWithdrawAmountInSU) {
            moneyAmountEditTxt.requestFocus()
            showToast(
                getString(
                    R.string.withdraw_amount_too_high,
                    vitechainNormalTokenInfo.amountText(
                        maximumWithdrawAmountInSU.toBigInteger(),
                        3
                    ),
                    vitechainNormalTokenInfo.symbol!!
                )
            )
            return null
        }

        return userInputInSu
    }


    @SuppressLint("CheckResult")
    private fun setUpMoneyEditDisposable() {
        RxTextWatcher.onTextChanged(moneyAmountEditTxt) { s ->
            if (s.isEmpty()) {
                feeAmount.text = "0"
            }
            s.toBigDecimalOrNull()?.multiply(
                ExternalPriceCenter.getPrice(vitechainNormalTokenInfo.tokenCode!!)
            )?.toCurrencyText(2)?.let {
                transferValueTxt.text = it
                transferValueTxt.visibility = View.VISIBLE
            } ?: kotlin.run {
                transferValueTxt.visibility = View.GONE
            }

            s.isNotEmpty()
        }.debounce(500, TimeUnit.MILLISECONDS)
            .subscribe { amountInSu ->
                crosschainVM.withdrawFee(
                    currentGwNormalTokenInfo.url!!,
                    vitechainNormalTokenInfo.tokenAddress!!,
                    myViteaddress,
                    vitechainNormalTokenInfo
                        .baseToSmallestUnit(amountInSu.toBigDecimal())
                        .toBigInteger().toString()
                )
            }

    }

    private fun setupTopLayout() {
        myAddress.text = myViteaddress
        myAddressName.text = currentAccount.getAddressName(myViteaddress)
        symbol.text = vitechainNormalTokenInfo.symbol!!
        allMappedTokenInfos.forEach { tokenInfo ->
            chooseMainNetContentContainer.addMappedToken(tokenInfo) { selectedTokenInfo ->
                checkGWAvailableThenRefresh(selectedTokenInfo)
            }
        }
    }


    private fun setupMoneyAmountEditTxt() {
        withdrawAll.setOnClickListener {
            setAllAvailableAllAmount()
        }
        moneyAmountEditTxt.customRightText = vitechainNormalTokenInfo.symbol!!
        if (vitechainNormalTokenInfo.decimal != 0) {
            moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789.")
        } else {
            moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789")
        }
        moneyAmountEditTxt.addTextChangedListener(DecimalLimitTextWatcher(vitechainNormalTokenInfo.decimal!!))

    }


    private fun setupOtherClick() {
        transferBtn.setOnClickListener {
            if (!hasNetWork()) {
                showToast(R.string.net_work_error)
                return@setOnClickListener
            }

            checkAndGetAmountInSu()?.let {
                crosschainVM.withdrawAddressVerificationAndGetFee(
                    currentGwNormalTokenInfo.url!!,
                    vitechainNormalTokenInfo.tokenAddress!!,
                    toAddrEditTxt.editableText.toString(),
                    myViteaddress,
                    it.stripTrailingZeros().toPlainString(),
                    if (needLabelFlag) addraddrLabelTxt.text.toString() else null //label
                )
            }
        }


        infor4Fee.setOnClickListener {
            val content =
                if (feeInSu == null) getString(R.string.crosschain_withdraw_fee_what_is_no_fee) else getString(
                    R.string.crosschain_withdraw_fee_what_is
                ) + feeAmount.text + " " + vitechainNormalTokenInfo.symbol!!

            TextTitleNotifyDialog(this).apply {
                setTitle(R.string.crosschain_withdraw_fee)
                setBottom(R.string.i_see)
                setMessage(content)
                show()
            }
        }

        infoWithdrawNotice.setOnClickListener {
            minimumWithdrawAmountInSu ?: return@setOnClickListener
            maximumWithdrawAmountInSu ?: return@setOnClickListener
            TextTitleNotifyDialog(this).apply {
                setTitle(R.string.crosschain_withdraw_withdraw_amount_limit_title)
                setBottom(R.string.i_see)
                setMessage(
                    getString(
                        R.string.crosschain_withdraw_withdraw_amount_limit_content,
                        vitechainNormalTokenInfo.smallestToBaseUnit(minimumWithdrawAmountInSu!!)
                            .toLocalReadableText(5),
                        vitechainNormalTokenInfo.symbol!!,
                        vitechainNormalTokenInfo.smallestToBaseUnit(maximumWithdrawAmountInSu!!)
                            .toLocalReadableText(5),
                        vitechainNormalTokenInfo.symbol!!
                    )
                )
                show()
            }
        }

        overchainWithdrawRecord.setOnClickListener {
            WithdrawRecordsActivity.show(
                this,
                vitechainNormalTokenInfo.tokenAddress!!,
                myViteaddress,
                currentGwNormalTokenInfo.url!!
            )
        }

        tokenWidget.setOnClickListener {
            overchainWithdrawRecord.performClick()
        }
    }


    @SuppressLint("CheckResult")
    private fun setAllAvailableAllAmount() {
        GwCrosschainApi.withdrawFee(
            vitechainNormalTokenInfo.tokenAddress!!,
            myViteaddress,
            vitechainNormalTokenInfo.baseToSmallestUnit(
                vitechainNormalTokenInfo.balance().toBigDecimal()
            ).toBigInteger().toString(),
            currentGwNormalTokenInfo.url!!
        ).subscribe({
            feeInSu = it?.fee?.toBigDecimalOrNull()
            val feeInSuDecimal = feeInSu
            feeInSuDecimal?.let { feeInSuDecimal ->
                moneyAmountEditTxt.setText(
                    vitechainNormalTokenInfo.amountText(
                        (vitechainNormalTokenInfo.balance().toBigDecimal().minus(feeInSuDecimal)
                            .takeIf {
                                it > BigDecimal.ZERO
                            } ?: BigDecimal.ZERO).toBigInteger(),
                        8
                    )
                )
            }
        }, {
            loge(it)
        })
    }

    override fun onStart() {
        super.onStart()
        checkGWAvailableThenRefresh(currentGwNormalTokenInfo, true)
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {

        if (qrcodeParseResult.requestCode == ScanForAddress) {
            transferToExtra.visibility = View.GONE
            toAddrEditTxt.setText(qrcodeParseResult.rawData)
            return
        }
        if (qrcodeParseResult.requestCode == ScanForLabel) {
            addraddrLabelTxt.setText(qrcodeParseResult.rawData)
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

    private fun checkGWAvailableThenRefresh(
        selectedTokenInfo: NormalTokenInfo,
        forceRefesh: Boolean = false
    ) {
        if (selectedTokenInfo == currentGwNormalTokenInfo && !forceRefesh) return
        checkGWAvailable(selectedTokenInfo) {
            currentGwNormalTokenInfo = selectedTokenInfo

            gatewayAddress = it.gatewayAddress
            maximumWithdrawAmountInSu = it.maximumWithdrawAmount?.toBigDecimalOrNull()
            minimumWithdrawAmountInSu = it.minimumWithdrawAmount?.toBigDecimalOrNull()?.also {
                moneyAmountEditTxt.hint = getString(
                    R.string.crosschain_withdraw_amount_hint,
                    vitechainNormalTokenInfo.smallestToBaseUnit(it).toLocalReadableText(5),
                    vitechainNormalTokenInfo.symbol!!
                )
            }

            needLabelFlag = it.needLabel()

            if (needLabelFlag) {
                addrLabel.visibility = View.VISIBLE
                addraddrLabelTxt.hint = it.labelName.toString()
            } else {
                addrLabel.visibility = View.GONE
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun checkGWAvailable(
        selectedTokenInfo: NormalTokenInfo,
        onSuccess: (WithdrawInfoResp) -> Unit
    ) {
        cycleProgressBar.show()
        GwCrosschainApi.metaInfo(
            vitechainNormalTokenInfo.tokenAddress!!,
            selectedTokenInfo.url!!
        ).subscribe(
            {
                if (it.withdrawState != MetaInfoResp.OPEN) {
                    it.showCheckDialog(this) {
                        it.dismiss()
                    }
                    return@subscribe
                }
                metaType = it.type

                GwCrosschainApi.withdrawInfo(
                    vitechainNormalTokenInfo.tokenAddress!!,
                    currentAccount.nowViteAddress()!!,
                    selectedTokenInfo.url
                ).subscribe({
                    cycleProgressBar.dismiss()
                    onSuccess(it)
                }, errorHandler)

            },
            errorHandler
        )
    }

    private val errorHandler: (Throwable) -> Unit = {
        cycleProgressBar.dismiss()
        if (it is OverChainApiException) {
            it.showErrorDialog(this)
        } else {
            it.showDialogMsg(this)
        }
    }
}