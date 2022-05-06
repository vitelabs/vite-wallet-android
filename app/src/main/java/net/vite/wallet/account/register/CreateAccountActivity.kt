package net.vite.wallet.account.register

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_create_account.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.dialog.ProgressDialog
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.showToast
import net.vite.wallet.vep.QrcodeParseResult
import okhttp3.HttpUrl

class CreateAccountActivity : BaseActivity() {

    private lateinit var passphrase: String
    private lateinit var name: String


    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        val code = HttpUrl.parse(qrcodeParseResult.rawData)?.queryParameter("vitex_invite_code")
            ?: return

        fillInviteCodeEditText.setText(code)
    }

    val vm by lazy { ViewModelProviders.of(this)[CheckInviteCodeVM::class.java] }
    lateinit var cycleProgressBar: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        fillInviteCodeEditText.onClickRightDrawableListener = {
            scanQrcode(988)
        }

        cycleProgressBar = ProgressDialog(this)

        vm.checkCodeLd.observe(this, Observer {
            it.handleDialogShowOrDismiss(cycleProgressBar)
            if (it.isLoading()) {
                return@Observer
            }
            if (it.resp == true) {
                createAccount()
            } else {
                showToast(R.string.invite_code_error)
            }
        })

        passwordEditTxt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                }
            }
        })

        repeatPasswordEditTxt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                completeInput()
            }
            true
        }

        createAccBtn.setOnClickListener {
            completeInput()
        }
        setupRemeber3Text()
    }

    private fun completeInput() {
        val code = fillInviteCodeEditText.editableText.toString().trim()
        if (code.isEmpty()) {
            createAccount()
        } else {
            vm.checkCode(code)
        }
    }

    private fun setupRemeber3Text() {
        val clickableSpan = object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                ds.color = resources.getColor(R.color.viteblue)
                ds.isUnderlineText = true
            }

            override fun onClick(widget: View) {
                startActivity(createBrowserIntent("https://growth.vite.net/term"))
            }
        }
        val spannableBuilder = SpannableStringBuilder()
        val originStr = getString(R.string.remember_check3)
        val clickableText = getString(R.string.user_term)
        val start = originStr.indexOf(clickableText)
        val end = start + clickableText.length
        spannableBuilder.append(originStr)
            .setSpan(clickableSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

        remember3txt.movementMethod = LinkMovementMethod.getInstance()
        remember3txt.text = spannableBuilder
    }

    private fun createAccount() {
        if (!remember3.isChecked) {
            showToast(R.string.you_must_check_all)
            return
        }

        val editableText = repeatPasswordEditTxt.editableText.toString()
        val editableText1 = passwordEditTxt.editableText.toString()
        if (editableText1.length < 8) {
            Toast.makeText(
                this,
                getString(R.string.password_must_at_least_8_char),
                Toast.LENGTH_SHORT
            ).show()
            passwordEditTxt.requestFocus()
            return
        }
        if (editableText != editableText1) {
            Toast.makeText(this, getString(R.string.password_not_equal), Toast.LENGTH_SHORT).show()
            return
        }
        val accname = toAddrEditTxtC.editableText.toString().trim()
        if (AccountCenter.hasAccount(accname)) {
            Toast.makeText(this, getString(R.string.account_name_duplicate), Toast.LENGTH_SHORT)
                .show()
            return
        }
        passphrase = editableText
        name = accname

        val intent = Intent(this, MnemonicNoticeActivity::class.java)
        intent.putExtra("passphrase", editableText)
        intent.putExtra("name", accname)
        startActivityForResult(intent, 9899)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 9899) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK, Intent().apply {
                    val code = fillInviteCodeEditText.editableText.toString().trim()
                    if (code.isNotEmpty()) {
                        putExtra("dex_invite_code", code)
                    }
                })
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}
