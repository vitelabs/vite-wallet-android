package net.vite.wallet.account.register

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ReplacementTransformationMethod
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_recover_mnemonic.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.CheckedMnemonic
import net.vite.wallet.account.MnemonicHelper
import net.vite.wallet.activities.SoftInputActivity
import net.vite.wallet.dialog.ProgressDialog
import net.vite.wallet.loge
import net.vite.wallet.utils.showToast
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.viteappuri.BackupWalletAction
import net.vite.wallet.viteappuri.ViteAppUri
import okhttp3.HttpUrl
import org.vitelabs.mobile.Mobile

class RecoverMnemonicActivity : SoftInputActivity() {
    val wordList by lazy {
        Mobile.getWordList("en")
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        if (qrcodeParseResult.requestCode == 988) {
            val code = HttpUrl.parse(qrcodeParseResult.rawData)?.queryParameter("vitex_invite_code")
                ?: return

            fillInviteCodeEditText.setText(code)
        } else {
            if (qrcodeParseResult.result !is ViteAppUri) {
                return
            }
            val backupWalletAction =
                BackupWalletAction.tryCreate(qrcodeParseResult.result.uri) ?: return

            val mnemonic = kotlin.runCatching {
                Mobile.entropyToMnemonic(
                    backupWalletAction.entropy,
                    backupWalletAction.language
                )
            }.getOrNull() ?: return

            recoverEditText.setText(mnemonic)
        }
    }

    val vm by lazy { ViewModelProviders.of(this)[CheckInviteCodeVM::class.java] }
    lateinit var cycleProgressBar: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_recover_mnemonic)
        cycleProgressBar = ProgressDialog(this)

        vm.checkCodeLd.observe(this, Observer {
            it.handleDialogShowOrDismiss(cycleProgressBar)
            if (it.isLoading()) {
                return@Observer
            }
            if (it.resp == true) {
                recoverAccount()
            } else {
                showToast(R.string.invite_code_error)
            }
        })
        qrCodeScan.setOnClickListener {
            scanQrcode(987)
        }

        fillInviteCodeEditText.onClickRightDrawableListener = {
            scanQrcode(988)
        }


        repeatPasswordEditTxt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                completeInput()
            }
            true
        }

        recoverEditText.transformationMethod = object : ReplacementTransformationMethod() {
            override fun getOriginal(): CharArray {
                return upCaseLetters
            }

            override fun getReplacement(): CharArray {
                return lowCaseLetters

            }
        }


        recoverEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    mnemonicHintLayout.visibility = View.GONE
                } else {
                    val nowinputword = s.trim().split(Regex("\\s+")).last()
                    if (nowinputword.isBlank()) {
                        mnemonicHintLayout.visibility = View.GONE
                        return
                    }

                    val foundList = wordList.findStartWith(nowinputword, 3)
                    if (foundList.size() >= 1) {
                        mnemonicHintLayout.visibility = View.VISIBLE
                    } else {
                        mnemonicHintLayout.visibility = View.GONE
                    }

                    try {
                        hint0.visibility = View.VISIBLE
                        hint0.text = foundList.get(0)
                    } catch (e: Exception) {
                        hint0.visibility = View.GONE
                    }
                    try {
                        hint1.visibility = View.VISIBLE
                        hint1.text = foundList.get(1)
                    } catch (e: Exception) {
                        hint1.visibility = View.GONE
                    }
                    try {
                        hint2.visibility = View.VISIBLE
                        hint2.text = foundList.get(2)
                    } catch (e: Exception) {
                        hint2.visibility = View.GONE
                    }
                }
            }
        })

        val clickToRecoverLis = { view: View ->
            if (view is TextView) {
                val text = "${view.text} "
                val lastSpaceIndex = recoverEditText.editableText.indexOfLast { it == ' ' }
                if (lastSpaceIndex == -1) {
                    recoverEditText.editableText.delete(0, recoverEditText.editableText.length)
                } else {
                    recoverEditText.editableText.delete(
                        lastSpaceIndex + 1,
                        recoverEditText.editableText.length
                    )
                }
                recoverEditText.editableText.append(text)
                mnemonicHintLayout.visibility = View.GONE
            }
        }
        hint0.setOnClickListener {
            clickToRecoverLis(it)
        }
        hint1.setOnClickListener {
            clickToRecoverLis(it)
        }
        hint2.setOnClickListener {
            clickToRecoverLis(it)
        }


        createAccBtn.setOnClickListener {
            completeInput()
        }
    }

    override fun onSoftInputShow(rootView: View, keypadHeight: Int) {
        val o = ObjectAnimator.ofFloat(mnemonicHintLayout, "translationY", -keypadHeight.toFloat())
        o.duration = 300
        o.start()
    }

    override fun onSoftInputDismiss(rootView: View, keypadHeight: Int) {
        mnemonicHintLayout.translationY = 0.toFloat()
        mnemonicHintLayout.visibility = View.GONE
    }

    private fun completeInput() {
        val code = fillInviteCodeEditText.editableText.toString().trim()
        if (code.isEmpty()) {
            recoverAccount()
        } else {
            vm.checkCode(code)
        }
    }

    private fun recoverAccount() {
        val accname = accountNameEditTxtC.editableText.toString().trim()
        if (accname.isBlank()) {
            Toast.makeText(this, R.string.acc_name_can_not_blank, Toast.LENGTH_SHORT).show()
            return
        }

        val pass1 = repeatPasswordEditTxt.editableText.toString()
        val pass0 = passwordEditTxt.editableText.toString()
        if (pass0.length < 8) {
            Toast.makeText(
                this,
                getString(R.string.password_must_at_least_8_char),
                Toast.LENGTH_SHORT
            ).show()
            passwordEditTxt.requestFocus()
            return
        }

        if (pass1 != pass0) {
            Toast.makeText(this, getString(R.string.password_not_equal), Toast.LENGTH_SHORT).show()
            return
        }
        val mnemonic = recoverEditText.editableText.toString().trim().toLowerCase()

        if (AccountCenter.hasAccount(accname)) {
            Toast.makeText(this, getString(R.string.account_name_duplicate), Toast.LENGTH_SHORT)
                .show()
            return
        }
        try {
            val checkedMnemonic = MnemonicHelper.checkMnemonic(mnemonic)
            val existAccount = AccountCenter.tryGetExistAccount(checkedMnemonic.primaryAddr)

            if (existAccount != null) {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.already_exist_same_account, existAccount.name))
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(R.string.replace) { _, _ ->
                        recover(
                            checkedMnemonic,
                            pass1,
                            accname
                        )
                    }
                    .create()
                    .show()
                return
            }
            recover(checkedMnemonic, pass1, accname)
        } catch (e: Exception) {
            loge(e)
            Toast.makeText(this, R.string.error_mnemonic, Toast.LENGTH_SHORT).show()
        }
    }

    private fun recover(mnemonic: CheckedMnemonic, pass0: String, accname: String) {
        try {
            if (AccountCenter.recoverAccount(
                    mnemonic.formatMnemonic,
                    pass0,
                    accname,
                    mnemonic.lang
                )
            ) {
                AccountCenter.login(accname, pass0)

                setResult(Activity.RESULT_OK, Intent().apply {
                    val code = fillInviteCodeEditText.editableText.toString().trim()
                    if (code.isNotEmpty()) {
                        putExtra("dex_invite_code", code)
                    }
                })
                Toast.makeText(this, R.string.recover_success, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, R.string.undefined_error, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Throwable) {
            loge(e)
            Toast.makeText(this, R.string.error_mnemonic, Toast.LENGTH_SHORT).show()
        }
    }

    private val upCaseLetters = CharArray(26) {
        arrayOf(
            'A',
            'B',
            'C',
            'D',
            'E',
            'F',
            'G',
            'H',
            'I',
            'J',
            'K',
            'L',
            'M',
            'N',
            'O',
            'P',
            'Q',
            'R',
            'S',
            'T',
            'U',
            'V',
            'W',
            'X',
            'Y',
            'Z'
        )[it]
    }
    private val lowCaseLetters = CharArray(26) {
        arrayOf(
            'a',
            'b',
            'c',
            'd',
            'e',
            'f',
            'g',
            'h',
            'i',
            'j',
            'k',
            'l',
            'm',
            'n',
            'o',
            'p',
            'q',
            'r',
            's',
            't',
            'u',
            'v',
            'w',
            'x',
            'y',
            'z'
        )[it]
    }
}
