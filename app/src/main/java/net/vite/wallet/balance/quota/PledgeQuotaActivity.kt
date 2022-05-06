package net.vite.wallet.balance.quota

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_pledge_quota.*
import net.vite.wallet.R
import net.vite.wallet.TxEndStatus
import net.vite.wallet.TxSendSuccess
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.constants.BlockDetailTypePledge
import net.vite.wallet.constants.BlockTypeToContactAddress
import net.vite.wallet.contacts.readonly.ReadOnlyContactListActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.LogoTitleNotifyDialog
import net.vite.wallet.network.rpc.*
import net.vite.wallet.utils.hasNetWork
import net.vite.wallet.utils.showToast
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.vep.ViteUrlTransferParams
import net.vite.wallet.widget.DecimalLimitTextWatcher
import org.vitelabs.mobile.Mobile
import java.math.BigDecimal
import java.math.BigInteger

class PledgeQuotaActivity : BaseTxSendActivity() {


    companion object {
        fun show(context: Context, presetAddr: String? = null, presetAmount: String? = null) {
            context.startActivity(Intent(context, PledgeQuotaActivity::class.java).apply {
                presetAddr?.let {
                    putExtra("presetAddr", it)
                }
                presetAmount?.let {
                    putExtra("presetAmount", it)
                }
            })
        }

    }

    val balanceVm by viewModels<BalanceViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pledge_quota)

        setupUserProfile()

        pledgeList.setOnClickListener {
            PledgeListActivity.show(this@PledgeQuotaActivity, currentAccount.nowViteAddress()!!)
        }

        setupExtraFunc()

        setupMoneyEditText()
        intent.getStringExtra("presetAddr")?.let {
            toAddrEditTxt.setText(it)
        }

        intent.getStringExtra("presetAmount")?.let {
            moneyAmountEditTxt.setText(it)
        }

        balanceVm.apply {
            rtViteAccInfo
                .observe(this@PledgeQuotaActivity, Observer<ViteAccountInfo> {
                    myBalance.text = it.getBalanceReadableText(4, ViteTokenInfo.tokenId!!)
                })

            rtQuotaAndTxNum.observe(this@PledgeQuotaActivity, Observer<GetPledgeQuotaResp> {
                myQuota.text = "${it.currentUt} /${it.utpe} Quota"
                myAcquiredStaking.text =
                    ViteTokenInfo.amountText(
                        it.pledgeAmount?.toBigIntegerOrNull() ?: BigInteger.ZERO, 4
                    )
            })
        }

        quotaCostContainer.setQuotaNeedCost("5")
        stakeFreezeTimeContainer.customEndText = getString(R.string.about_3_day)

        transferBtn.setOnClickListener {
            transfer()
        }
    }

    private fun setupMoneyEditText() {
        moneyAmountEditTxt.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                transfer()
                return@setOnEditorActionListener true
            }
            false
        }
        moneyAmountEditTxt.addTextChangedListener(DecimalLimitTextWatcher(ViteTokenInfo.decimals!!))
        moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789.")
    }

    private fun transfer() {
        if (!hasNetWork()) {
            showToast(R.string.net_work_error)
            return
        }

        val inputAddrTxt = toAddrEditTxt.editableText.toString()
        if (!Mobile.isValidAddress(inputAddrTxt)) {
            toAddrEditTxt.requestFocus()
            showToast(R.string.input_address_not_valid)
            return
        }

        val amountTxt = moneyAmountEditTxt.editableText.toString()
        val sendAmount = amountTxt.toBigDecimalOrNull() ?: kotlin.run {
            moneyAmountEditTxt.requestFocus()
            showToast(R.string.please_input_correct_amount)
            return
        }



        if (sendAmount < BigDecimal.valueOf(134)) {
            moneyAmountEditTxt.requestFocus()
            showToast(R.string.pledge_amount_must_bigger_134)
            return
        }

        val nowAddr = currentAccount.nowViteAddress()!!

        val sendAmountBig =
            sendAmount.multiply(BigDecimal.TEN.pow(ViteTokenInfo.decimals!!)).toBigInteger()

        val currentBalance =
            ViteAccountInfoPoll.latestViteAccountInfo[nowAddr]?.getBalance(ViteTokenInfo.tokenId!!)
                ?: BigInteger.ZERO

        if (sendAmountBig > currentBalance) {
            moneyAmountEditTxt.requestFocus()
            showToast(R.string.balance_not_enough)
            return
        }


        lastSendParams = NormalTxParams(
            accountAddr = nowAddr,
            toAddr = BlockTypeToContactAddress[BlockDetailTypePledge]!!,
            tokenId = ViteTokenInfo.tokenId!!,
            amountInSu = sendAmountBig,
            data = BuildInContractEncoder.encodePledgeNew(inputAddrTxt)
        )

        val dialogParams = BigDialog.Params(
            bigTitle = getString(R.string.pledge_vite),
            secondTitle = getString(R.string.quota_benifit_address),
            secondValue = inputAddrTxt,
            amount = amountTxt,
            tokenSymbol = ViteTokenInfo.symbol ?: "",
            firstButtonTxt = getString(R.string.confirm_pay),
            quotaCost = "5"
        )
        verifyIdentity(dialogParams,
            { lastSendParams?.let { sendTx(it) } },
            {})

    }

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

    override fun onTxEnd(status: TxEndStatus) {
        if (status is TxSendSuccess) {
            with(LogoTitleNotifyDialog(this)) {
                enableTopImage(true)
                setMessage(getString(R.string.pledge_quota_success))
                setCancelable(false)
                setBottom(getString(R.string.confirm)) {
                    this.dismiss()
                    PledgeListActivity.show(
                        this@PledgeQuotaActivity,
                        currentAccount.nowViteAddress()!!
                    )
                }
                show()
            }
        }

    }

    private fun setupUserProfile() {
        val nowAddr = currentAccount.nowViteAddress() ?: kotlin.run {
            finish()
            return
        }

        myAddress.text = nowAddr
        myBalance.text =
            ViteAccountInfoPoll.latestViteAccountInfo[nowAddr].getBalanceReadableText(
                4,
                ViteTokenInfo.tokenId!!
            )

        moneyAmountEditTxt.customRightText = ViteTokenInfo.symbol!!
        QuotaAndTxNumPoll.latestData[nowAddr]?.let {
            myQuota.text = "${it.currentUt} Quota/${it.utpe} Quota"
            myAcquiredStaking.text =
                ViteTokenInfo.amountText(
                    it.pledgeAmount?.toBigIntegerOrNull() ?: BigInteger.ZERO,
                    4
                )
        }
    }


    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        transferToExtra.visibility = View.GONE
        if (qrcodeParseResult.result is ViteUrlTransferParams) {
            toAddrEditTxt.setText(qrcodeParseResult.result.toAddr)
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
