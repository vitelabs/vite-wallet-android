package net.vite.wallet.balance.quota


import android.app.Activity
import android.app.Dialog
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import net.vite.wallet.*
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.constants.BlockDetailTypePledge
import net.vite.wallet.constants.BlockTypeToContactAddress
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.network.rpc.BuildInContractEncoder
import net.vite.wallet.network.rpc.NormalTxParams
import net.vite.wallet.network.rpc.ViteTokenInfo
import java.math.BigDecimal


class FastPledgeQuotaFlow(val activity: UnchangableAccountAwareActivity) {
    class UserCancelStatus(requestCode: Int) : TxEndStatus(requestCode)

    private var listener: TxSendFlowStatusListener? = null

    var lastSendParams: NormalTxParams? = null
    private val baseTxSendFlow: BaseTxSendFlow = BaseTxSendFlow(activity, {
        lastSendParams
    }, listener)

    fun onCreate() {
        baseTxSendFlow.onCreate()
    }

    private class PledgeQuotaFastDialog(val activity: Activity) : Dialog(activity, R.style.NormalAlertDialog) {
        val closeBtn: View
        val confirmBtn: View
        val cancelBtn: View
        val secondTitleValueTxt: TextView


        init {
            setContentView(R.layout.dialog_pledge_quota_fast)
            closeBtn = findViewById<View>(R.id.closeBtn)
            confirmBtn = findViewById<View>(R.id.firstButton)
            cancelBtn = findViewById<View>(R.id.cancelButton)
            secondTitleValueTxt = findViewById<TextView>(R.id.secondTitleValueTxt)
            val originText =
                activity.getString(R.string.dialog_pledge_quota_hint, activity.getString(R.string.click_here))
            secondTitleValueTxt.text = originText
            makeTextClickable(secondTitleValueTxt, originText, activity.getString(R.string.click_here))
            setCancelable(false)
        }


        private fun makeTextClickable(
            textView: TextView,
            originText: String,
            clickableText: String
        ) {
            textView.text = originText
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(ds: TextPaint) {
                    ds.color = activity.resources.getColor(R.color.viteblue)
                    ds.isUnderlineText = true
                }

                override fun onClick(widget: View) {
                    cancelBtn.performClick()
                    PledgeQuotaActivity.show(activity)
                }
            }
            val spannableBuilder = SpannableStringBuilder()
            val start = originText.indexOf(clickableText)
            val end = start + clickableText.length
            spannableBuilder.append(originText)
                .setSpan(clickableSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

            textView.movementMethod = LinkMovementMethod.getInstance()
            textView.text = spannableBuilder
        }
    }


    private fun pledgeToMySelf(requestCode: Int) {
        with(activity) {
            val nowAddress = AccountCenter.currentViteAddress()!!
            lastSendParams = NormalTxParams(
                accountAddr = nowAddress,
                toAddr = BlockTypeToContactAddress[BlockDetailTypePledge]!!,
                tokenId = ViteTokenInfo.tokenId!!,
                amountInSu = "134".toBigDecimal().multiply(BigDecimal.TEN.pow(ViteTokenInfo.decimals!!)).toBigInteger(),
                data = BuildInContractEncoder.encodePledgeNew(nowAddress)
            )

            val dialogParasm = BigDialog.Params(
                bigTitle = getString(R.string.pledge_vite),
                secondTitle = getString(R.string.quota_benifit_address),
                secondValue = nowAddress,
                amount = "134",
                quotaCost = "5",
                tokenSymbol = ViteTokenInfo.symbol ?: "",
                firstButtonTxt = getString(R.string.confirm_pay)
            )
            baseTxSendFlow.identityVerify.verifyIdentityCloseAware(
                dialogParasm,
                false,
                {
                    lastSendParams?.let { baseTxSendFlow.txSendViewModel.sendTx(it, requestCode) }
                },
                {
                    listener?.invoke(TxSendCancel(TxSendCancel.IdVerifyCancel, requestCode))
                })
        }
    }


    fun show(listener: TxSendFlowStatusListener, requestCode: Int) {
        this.listener = listener
        baseTxSendFlow.txSendFlowListener = listener
        val fastDialog = PledgeQuotaFastDialog(activity)
        with(fastDialog) {
            closeBtn.setOnClickListener {
                listener.invoke(UserCancelStatus(requestCode))
                dismiss()
            }
            confirmBtn.setOnClickListener {
                pledgeToMySelf(requestCode)
                dismiss()
            }
            cancelBtn.setOnClickListener {
                closeBtn.performClick()
            }
            show()
        }

    }
}