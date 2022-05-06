package net.vite.wallet.account

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import net.vite.wallet.MainActivity
import net.vite.wallet.R
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.viteappuri.BackupWalletAction
import net.vite.wallet.viteappuri.ViteAppUri
import org.vitelabs.mobile.Mobile

open class LoginPrepareBaseActivity : BaseActivity() {

    private fun recover(mnemonic: String, pass0: String, accname: String, lang: String) {
        try {
            if (AccountCenter.recoverAccount(mnemonic, pass0, accname, lang)) {
                AccountCenter.login(accname, pass0)
                AccountCenter.getCurrentAccount()?.apply {
                    deriveNewViteAddress()
                }

                AccountCenter.login(accname, pass0)
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("showMnemonic", mnemonic)
                    putExtra("MnemonicPassphrase", pass0)
                    putExtra("MnemonicLang", lang)
                })
                finish()
            } else {
                Toast.makeText(this, R.string.undefined_error, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Throwable) {
            Toast.makeText(this, R.string.error_mnemonic, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
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

        if (AccountCenter.hasAccount(backupWalletAction.name)) {
            Toast.makeText(this, getString(R.string.account_name_duplicate), Toast.LENGTH_SHORT)
                .show()
            return
        }

        val address =
            kotlin.runCatching {
                Mobile.tryTransformMnemonic(mnemonic, backupWalletAction.language, "").hex
            }.getOrNull() ?: return


        val existAccount = AccountCenter.tryGetExistAccount(address)
        if (existAccount != null) {
            AlertDialog.Builder(this)
                .setMessage(
                    getString(
                        R.string.already_exist_same_account, existAccount.name
                    )
                )
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(R.string.replace) { dialog, _ ->
                    dialog.dismiss()
                    recover(
                        mnemonic,
                        backupWalletAction.password,
                        backupWalletAction.name,
                        backupWalletAction.language
                    )
                }
                .create()
                .show()
            return
        }




        TextTitleNotifyDialog(this).apply {
            setTitle(R.string.notice)
            setMessage(R.string.please_confirm_to_gen_account)

            setBottom(R.string.confirm) {
                it.dismiss()
                recover(
                    mnemonic,
                    backupWalletAction.password,
                    backupWalletAction.name,
                    backupWalletAction.language
                )

            }
            setBottomLeft(R.string.cancel) {
                it.dismiss()
            }
            show()
        }


    }
}