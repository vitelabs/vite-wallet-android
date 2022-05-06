package net.vite.wallet.dialog

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import net.vite.wallet.R
import net.vite.wallet.widget.TokenIconWidget


class BigDialog(val activity: Activity) : Dialog(activity, R.style.NormalAlertDialog) {


    class Params(
        var bigTitle: String? = null,
        var secondTitle: String? = null,
        var secondValue: String? = null,
        var amount: String? = null,
        var tokenSymbol: String? = null,
        var firstButtonTxt: String? = null,
        var firstButtonOnClick: (() -> Unit)? = null,
        var secondButtonTxt: String? = null,
        var secondButtonOnClick: (() -> Unit)? = null,
        var passwordEnable: Boolean? = null,
        var closeListener: (() -> Unit)? = null,
        var feeAmount: String? = null,
        var feeTokenSymbol: String? = null,
        var tokenIcon: String? = null,
        var quotaCost: String? = null,
        var inviteCostHint: String? = null,
        var balance: String? = null,
        var balanceSymbol: String? = null,
        var stakeAmount: String? = null,
        var stakeSymbol: String? = null
    )


    fun setUiParams(p: Params) {
        p.apply {
            bigTitle?.let { setBigTitle(it) }
            secondTitle?.let { setSecondTitle(it) }
            amount?.let { setAmount(it) }
            tokenSymbol?.let { setTokenSymbol(it) }
            passwordEnable?.let { setPasswordEnable(it) }
            firstButtonTxt?.let { setFirstButton(it, firstButtonOnClick) }
            secondButtonTxt?.let { setFirstButton(it, secondButtonOnClick) }
            secondValue?.let { setSecondValue(it) }
            closeListener?.let { this@BigDialog.closeListener = it }
            feeAmount?.let { setFeeAmount(it) }
            feeTokenSymbol?.let { setFeeTokenSymbol(it) }
            tokenIcon?.let { tokenWidget.setup(it) }
            quotaCost?.let {
                setQuotaTxt(it)
            }
            inviteCostHint?.let {
                setInviteCostHint(it)
            }
            balance?.let { setBalance(it) }
            balanceSymbol?.let {
                setBalanceSymbol(it)
            }
            stakeAmount?.let {
                setStakeAmount(it)
            }
            stakeSymbol?.let {
                setStakeSymbol(it)
            }
        }
        return
    }

    private fun setQuotaTxt(quota: String) {
        quotLayout.visibility = View.VISIBLE
        quotaCostUtTxt.text = "$quota Quota"
    }

    private var titleTxt: TextView
    private var secondTitleTxt: TextView
    private var secondTitleValueTxt: TextView
    var tokenWidget: TokenIconWidget
        private set

    private var amountLayout: View
    private var balanceLayout: View
    private var stakeLayout: View
    private var sendAmountTxt: TextView
    private var tokenSymbolTxt: TextView
    private var balanceSymbol: TextView
    private var stakeSymbol: TextView
    private var stakeAmount: TextView

    private var quotLayout: View
    private var quotaCostUtTxt: TextView

    private var feeLayout: View
    private var feeAmountTxt: TextView
    private var feeTokenSymbolTxt: TextView
    private var inviteCostHint: TextView
    private var balanceAmountTxt: TextView


    private var firstButton: TextView
    private var secondButton: TextView

    var textInput: EditText
    var inputLayout: TextInputLayout

    var closeListener: (() -> Unit)? = null

    init {
        setContentView(R.layout.dialog_big)
        findViewById<View>(R.id.closeBtn)?.setOnClickListener {
            dismiss()
            closeListener?.invoke()
        }
        titleTxt = findViewById(R.id.titleTxt)!!
        secondTitleTxt = findViewById(R.id.secondTitleTxt)!!
        secondTitleValueTxt = findViewById(R.id.secondTitleValueTxt)!!
        tokenWidget = findViewById(R.id.tokenWidget)!!

        amountLayout = findViewById(R.id.amountLayout)
        balanceLayout = findViewById(R.id.balanceLayout)
        stakeLayout = findViewById(R.id.stakeLayout)
        sendAmountTxt = findViewById(R.id.sendAmountTxt)!!
        tokenSymbolTxt = findViewById(R.id.tokenSymbolTxt)!!
        balanceSymbol = findViewById(R.id.balanceTokenSymbolTxt)!!
        stakeSymbol = findViewById(R.id.stakeTokenSymbolTxt)!!
        stakeAmount = findViewById(R.id.stakeAmountTxt)!!

        quotLayout = findViewById(R.id.quotLayout)
        quotaCostUtTxt = findViewById(R.id.quotaCostUt)!!

        feeLayout = findViewById(R.id.feeLayout)
        feeAmountTxt = findViewById(R.id.feeAmountTxt)!!
        feeTokenSymbolTxt = findViewById(R.id.feeTokenSymbolTxt)!!

        textInput = findViewById(R.id.passwordInput)!!
        inputLayout = findViewById(R.id.inputLayout)!!

        firstButton = findViewById(R.id.firstButton)!!
        secondButton = findViewById(R.id.secondButton)!!

        inviteCostHint = findViewById(R.id.inviteCostHint)!!
        balanceAmountTxt = findViewById(R.id.balanceAmountTxt)!!
        setCancelable(false)

    }

    fun setMessageWithDarkBg(str: String, selectable: Boolean = true) {
        secondTitleValueTxt.visibility = View.VISIBLE
        secondTitleValueTxt.setBackgroundResource(R.drawable.mnemonic_input_big_rect)
        secondTitleValueTxt.setTextIsSelectable(selectable)
        secondTitleValueTxt.setPadding(16, 16, 16, 16)
        secondTitleValueTxt.text = str
    }

    fun setBalance(str: String) {
        balanceLayout.visibility = View.VISIBLE
        balanceAmountTxt.text = str
    }

    fun setBalanceSymbol(str: String) {
        balanceSymbol.visibility = View.VISIBLE
        balanceSymbol.text = str
    }

    fun setStakeAmount(str: String) {
        stakeLayout.visibility = View.VISIBLE
        stakeAmount.text = str
    }

    fun setStakeSymbol(str: String) {
        stakeSymbol.visibility = View.VISIBLE
        stakeSymbol.text = str
    }

    fun setBigTitle(str: String) {
        titleTxt.visibility = View.VISIBLE
        titleTxt.text = str
    }

    fun setSecondTitle(str: String) {
        secondTitleTxt.visibility = View.VISIBLE
        secondTitleTxt.text = str
    }

    fun setSecondValue(str: String) {
        secondTitleValueTxt.visibility = View.VISIBLE
        secondTitleValueTxt.text = str
    }

    fun setAmount(str: String) {
        amountLayout.visibility = View.VISIBLE
        sendAmountTxt.text = str
    }

    fun setTokenSymbol(str: String) {
        amountLayout.visibility = View.VISIBLE
        tokenSymbolTxt.text = str
    }

    fun setFeeAmount(str: String) {
        feeLayout.visibility = View.VISIBLE
        feeAmountTxt.text = str
    }

    fun setFeeTokenSymbol(str: String) {
        feeLayout.visibility = View.VISIBLE
        feeTokenSymbolTxt.text = str
    }

    fun setInviteCostHint(str: String) {
        inviteCostHint.visibility = View.VISIBLE
        inviteCostHint.text = str
    }


    fun getPwdInputText() = textInput.editableText.toString()

    fun setPasswordEnable(enable: Boolean) {
        inputLayout.visibility = if (enable) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setFirstButton(text: String, onclick: (() -> Unit)?) {
        firstButton.visibility = View.VISIBLE
        firstButton.text = text
        firstButton.setOnClickListener { onclick?.invoke() }
    }

    fun setFirstButtonClickListener(onclick: (() -> Unit)?) {
        firstButton.visibility = View.VISIBLE
        firstButton.setOnClickListener { onclick?.invoke() }
    }

    fun setSecondButton(text: String, onclick: (() -> Unit)?) {
        secondButton.visibility = View.VISIBLE
        secondButton.text = text
        secondButton.setOnClickListener { onclick?.invoke() }
    }


}