package net.vite.wallet.account.register

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.zxing.client.android.Utils
import kotlinx.android.synthetic.main.activity_remeber_mnemonic.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.utils.dp2px
import net.vite.wallet.viteappuri.BackupWalletAction
import org.vitelabs.mobile.Mobile


class RememberMnemonicActivity : BaseActivity() {
    companion object {
        const val MODE_FROM_BACKUP = 1
        const val MODE_FROM_NEW = 0
        fun show4Backup(context: Context, mnemonicText: String, accountName: String, passphrase: String) {
            context.startActivity(Intent(context, RememberMnemonicActivity::class.java).apply {
                putExtra("mode", MODE_FROM_BACKUP)
                putExtra("mnemonicText", mnemonicText)
                putExtra("name", accountName)
                putExtra("passphrase", passphrase)
            })
        }
    }

    var mode = MODE_FROM_NEW
    var mnemonic = ""
        set(value) {
            field = value
            refreshQrcode()
        }
    var length: Long = 12
    var lang: String = "en"
    lateinit var passphrase: String
    lateinit var name: String
    private lateinit var adapter: MnemonicAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remeber_mnemonic)
        mode = intent.getIntExtra("mode", MODE_FROM_NEW)
        name = intent.getStringExtra("name")!!
        passphrase = intent.getStringExtra("passphrase")!!

        if (mode == MODE_FROM_BACKUP) {
            switchLenText.visibility = View.GONE
            regenerateM.visibility = View.GONE
            switchLenIcon.visibility = View.GONE
            mnemonicLang.visibility = View.GONE
            mnemonic = intent.getStringExtra("mnemonicText")!!
            backupLater.setOnClickListener {
                finish()
            }
            memorizedMnemonicBtn.setOnClickListener {
                CheckMnemonicActivity.show(this, ArrayList(mnemonic.split(" ")), MODE_FROM_BACKUP)
            }
        } else {
            mnemonic = Mobile.newMnemonic("en", length)

            mnemonicLang.visibility = View.VISIBLE
            mnemonicLang.setOnCheckedChangeListener { _, _ ->
                lang = if (lang == "en") {
                    "zh-Hans"
                } else {
                    "en"
                }
                resetMnemonic()
            }


            resetLengthText()
            switchLenText.setOnClickListener {
                length = if (length == 24L) {
                    12
                } else {
                    24
                }
                resetLengthText()
                resetMnemonic()
            }
            regenerateM.setOnClickListener {
                resetMnemonic()
            }

            memorizedMnemonicBtn.setOnClickListener {
                CheckMnemonicActivity.show(this, ArrayList(mnemonic.split(" ")), MODE_FROM_NEW)
            }
        }

        mnemonicList.layoutManager = GridLayoutManager(this, 4)
        adapter = MnemonicAdapter(ArrayList(mnemonic.split(" ")))
        mnemonicList.adapter = adapter
    }


    private fun refreshQrcode() {

        val shareUrl = BackupWalletAction(
            name = name,
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

    private fun resetLengthText() {
        switchLenText.visibility = View.VISIBLE
        if (length == 24L) {
            switchLenText.setText(R.string.switch_to_12)
        } else {
            switchLenText.setText(R.string.switch_to_24)
        }
    }

    private fun resetMnemonic() {
        mnemonic = Mobile.newMnemonic(lang, length)
        adapter.list = ArrayList(mnemonic.split(" "))
        adapter.notifyDataSetChanged()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MODE_FROM_NEW) {
            if (resultCode == Activity.RESULT_CANCELED) {
                mnemonic = Mobile.newMnemonic(lang, length)
                adapter.list = ArrayList(mnemonic.split(" "))
                adapter.notifyDataSetChanged()
            } else {
                setResult(Activity.RESULT_OK)
                AccountCenter.recoverAccount(mnemonic, passphrase, name, lang)
                AccountCenter.login(name, passphrase)
                finish()
            }
        }

        if (requestCode == MODE_FROM_BACKUP) {
            if (resultCode == Activity.RESULT_OK) {
                AccountCenter.getCurrentAccount()?.sharedPreferences()?.edit()
                    ?.putBoolean("hasnotBackupMnemonic", false)
                    ?.apply()
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
