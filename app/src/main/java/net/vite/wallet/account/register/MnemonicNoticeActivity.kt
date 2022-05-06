package net.vite.wallet.account.register

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_mnemonic_notice.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.utils.showToast
import org.vitelabs.mobile.Mobile

class MnemonicNoticeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mnemonic_notice)
        backMnemonic.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        val name = intent.getStringExtra("name") ?: ""
        val passphrase = intent.getStringExtra("passphrase") ?: ""

        backupLater.setOnClickListener {
            if (!isCheckAllTerm()) {
                showToast(R.string.you_must_check_all)
                return@setOnClickListener
            }

            val mnemonic = Mobile.newMnemonic("en", 12)
            AccountCenter.recoverAccount(mnemonic, passphrase, name, "en")
            AccountCenter.login(name, passphrase)
            AccountCenter.getCurrentAccount()?.sharedPreferences()?.edit()
                ?.putBoolean("hasnotBackupMnemonic", true)
                ?.apply()
            setResult(Activity.RESULT_OK)
            finish()
        }

        backMnemonic.setOnClickListener {
            if (!isCheckAllTerm()) {
                showToast(R.string.you_must_check_all)
                return@setOnClickListener
            }

            val intent = Intent(this, RememberMnemonicActivity::class.java)
            intent.putExtra("passphrase", passphrase)
            intent.putExtra("name", name)
            startActivityForResult(intent, 1212)
        }

    }

    private fun isCheckAllTerm() = remember1.isChecked && remember2.isChecked

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1212) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            return
        }


    }


}
