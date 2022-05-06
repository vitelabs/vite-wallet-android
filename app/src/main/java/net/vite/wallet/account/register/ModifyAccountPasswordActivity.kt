package net.vite.wallet.account.register

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_modify_password.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.utils.showToast

class ModifyAccountPasswordActivity : UnchangableAccountAwareActivity() {

    companion object {
        fun show(context: Context, password: String) {
            context.startActivity(Intent(context, ModifyAccountPasswordActivity::class.java).apply {
                putExtra("password", password)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_password)
        repeatPasswordEditTxt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                modifyPassword()
            }
            true
        }

        modifyPassphraseBtn.setOnClickListener {
            modifyPassword()
        }
    }


    private fun modifyPassword() {

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

        if (AccountCenter.modifyPassphrase(
                currentAccount.name,
                intent.getStringExtra("password")!!,
                editableText
            )
        ) {
            showToast(R.string.success)
            finish()
        } else {
            showToast(R.string.failed)
        }


    }

}
