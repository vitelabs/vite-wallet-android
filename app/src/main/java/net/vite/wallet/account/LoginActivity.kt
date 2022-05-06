package net.vite.wallet.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*
import net.vite.wallet.MainActivity
import net.vite.wallet.R
import net.vite.wallet.account.register.CreateAccountActivity
import net.vite.wallet.account.register.RecoverMnemonicActivity
import net.vite.wallet.widget.TextPickerDialog

class LoginActivity : LoginPrepareBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        passwordEditTxt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
            }
            true
        }
        qrCodeScan.setOnClickListener {
            scanQrcode()
        }

        backMnemonic.setOnClickListener {
            login()
        }
        createAcc.setOnClickListener {
            val intent = Intent(this@LoginActivity, CreateAccountActivity::class.java)
            startActivityForResult(intent, 100)
        }
        recoverAcc.setOnClickListener {
            val intent = Intent(this@LoginActivity, RecoverMnemonicActivity::class.java)
            startActivityForResult(intent, 100)
        }

        toAddrEditTxtCLayout.hint = getString(R.string.choose_wallet_account)
        toAddrEditTxtC.isCursorVisible = false
        toAddrEditTxtC.isFocusable = false
        toAddrEditTxtC.isFocusableInTouchMode = false

        toAddrEditTxtC.setText(AccountCenter.getLastLoginAccount()?.name ?: "")
        toAddrEditTxtC.setOnClickListener {
            TextPickerDialog(this, R.string.choose_wallet_account,
                AccountCenter.getAccountList().map {
                    it.name
                }) { name, dialog ->
                toAddrEditTxtC.setText(name)
                dialog.dismiss()
            }.show()
        }
    }

    private fun login() {
        AccountCenter.login(
            toAddrEditTxtC.editableText.toString(),
            passwordEditTxt.editableText.toString()
        )?.let {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } ?: kotlin.run {
            Toast.makeText(
                this@LoginActivity,
                getString(R.string.password_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100) {
            if (resultCode == Activity.RESULT_OK) {
                val code = data?.getStringExtra("dex_invite_code")
                startActivity(Intent(this, MainActivity::class.java).apply {
                    code?.let {
                        putExtra("dex_invite_code", code)
                    }
                })

                finish()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }


}
