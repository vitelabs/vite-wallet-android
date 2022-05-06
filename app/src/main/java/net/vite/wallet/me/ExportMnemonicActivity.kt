package net.vite.wallet.me

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.google.zxing.client.android.Utils

import kotlinx.android.synthetic.main.activity_export_mnemonic.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.MnemonicHelper
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.utils.dp2px
import net.vite.wallet.viteappuri.BackupWalletAction
import org.vitelabs.mobile.Mobile

class ExportMnemonicActivity : BaseActivity() {

    companion object {
        fun show(context: Context, mnemonicTxt: String, passphrase: String) {
            context.startActivity(Intent(context, ExportMnemonicActivity::class.java).apply {
                putExtra("mnemonic", mnemonicTxt)
                putExtra("passphrase", passphrase)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_export_mnemonic)

        confirmBtn.setOnClickListener {

            finish()
        }
        val mnemonic = intent.getStringExtra("mnemonic")
        mnemonicTxt.text = mnemonic
        refreshQrcode()
    }

    private fun refreshQrcode() {
        val mnemonic = intent.getStringExtra("mnemonic")!!
        val passphrase = intent.getStringExtra("passphrase")!!
        val lang = kotlin.runCatching { MnemonicHelper.checkMnemonic(mnemonic) }.getOrNull()?.lang
            ?: kotlin.run {
                finish()
                return
            }
        val shareUrl = BackupWalletAction(
            name = AccountCenter.getCurrentAccount()?.name ?: "",
            entropy = Mobile.mnemonicToEntropy(mnemonic, lang),
            language = lang,
            password = passphrase
        ).toViteAppUriString()

        Utils.creteQrCodeImage(
            shareUrl,
            140.0F.dp2px().toInt(),
            140.0F.dp2px().toInt(),
            true
        )?.let {
            qrImg.setImageBitmap(it)
        }
    }
}