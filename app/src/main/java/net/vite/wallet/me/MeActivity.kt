package net.vite.wallet.me

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast

import kotlinx.android.synthetic.main.fragment_me.*
import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.Wallet
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.LoginActivity
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.account.register.RememberMnemonicActivity
import net.vite.wallet.contacts.ContactsActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.setup.AboutUsActivity
import net.vite.wallet.vitebridge.H5WebActivity

class MeActivity : UnchangableAccountAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_me)
        manageContact.setOnClickListener {

            startActivity(Intent(this, ContactsActivity::class.java))
        }
        manageMnemonic.setOnClickListener {

            val hasnotBackupMnemonic = currentAccount.sharedPreferences()?.getBoolean(
                "hasnotBackupMnemonic",
                false
            ) == true

            if (hasnotBackupMnemonic) {
                showMnemonicNoticeDialog()
            } else {
                BigDialog(this).let { dialog ->
                    dialog.setBigTitle(getString(R.string.extract_mnemonic))
                    dialog.setPasswordEnable(true)
                    dialog.setFirstButton(getString(R.string.confirm)) {
                        try {

                            val passphrase = dialog.getPwdInputText()
                            val mnemonicTxt = Wallet.instance.extractMnemonic(
                                currentAccount.entropyStore,
                                passphrase
                            )
                            ExportMnemonicActivity.show(
                                this,
                                mnemonicTxt = mnemonicTxt,
                                passphrase = passphrase
                            )
                            dialog.dismiss()
                        } catch (e: Exception) {
                            Toast.makeText(this, R.string.password_error, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    dialog.show()
                }
            }
        }

        accountName.text = currentAccount.name

        val editnameFunc = {
            BigDialog(this).apply {
                textInput.visibility = View.VISIBLE
                textInput.inputType = EditorInfo.TYPE_CLASS_TEXT
                textInput.setText(currentAccount.name)
                inputLayout.visibility = View.VISIBLE
                inputLayout.hint = getString(R.string.account_name_tag)
                setBigTitle(getString(R.string.change_name_title))
                setFirstButton(getString(R.string.confirm)) {
                    val newName = textInput.editableText.toString().trim()
                    if (newName.isBlank()) {
                        Toast.makeText(
                            activity,
                            R.string.acc_name_can_not_blank,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (newName != currentAccount.name) {
                        if (AccountCenter.changeUserName(newName)) {
                            this@MeActivity.accountName.text = currentAccount.name
                            Toast.makeText(activity, R.string.change_success, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    dismiss()
                }
                show()
            }
        }

        accountName.setOnClickListener { editnameFunc() }
        nameEditor.setOnClickListener { editnameFunc() }

        logoutBtn.setOnClickListener {

            if (currentAccount.sharedPreferences()?.getBoolean(
                    "hasnotBackupMnemonic",
                    false
                ) == true
            ) {
                showMnemonicNoticeDialog()
            } else {
                finish()
                AccountCenter.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        if (!ViteConfig.get().kvstore.getBoolean("has_clicked_exchange_mining", false)) {
            inviteBtnBadge.visibility = View.VISIBLE
        } else {
            inviteBtnBadge.visibility = View.GONE
        }

        inviteContainer.setOnClickListener {

            ViteConfig.get().kvstore.edit().putBoolean("has_clicked_exchange_mining", true).apply()
            inviteBtnBadge.visibility = View.GONE
            openInviteUrl()
        }

        rewardQueryContainer.setOnClickListener {
            H5WebActivity.show(
                this,
                "${NetConfigHolder.netConfig.rewardQueryUrl}?address=${currentAccount.nowViteAddress()}&&localize=en"
            )
        }

        systemSetupContainer.setOnClickListener {

            startActivity(Intent(this, SystemSetupActivity::class.java))
        }

        securitySetupContainer.setOnClickListener {
            SystemSetupActivity.showSecuritySetting(this, true)
        }

        forumContainer.setOnClickListener {

            startActivity(Intent().apply {
                data = Uri.parse("https://forum.vite.net/")
                action = Intent.ACTION_VIEW
            })
        }

        aboutusContainer.setOnClickListener {

            startActivity(Intent(this, AboutUsActivity::class.java))
        }
    }

    private fun openInviteUrl() {
        H5WebActivity.show(this, NetConfigHolder.netConfig.exchangeInviteUrl)
    }

    private var mnemonicNoticeDialog: BigDialog? = null
    fun showMnemonicNoticeDialog() {
        if (mnemonicNoticeDialog?.isShowing == true) {
            return
        }
        mnemonicNoticeDialog = BigDialog(this)

        mnemonicNoticeDialog?.apply {
            setBigTitle(getString(R.string.mnemonic_not_backup))
            setSecondTitle(getString(R.string.please_input_password_to_backup))
            setPasswordEnable(true)
            setFirstButton(getString(R.string.goto_backup)) {
                try {
                    val mnemonicTxt = Wallet.instance.extractMnemonic(
                        currentAccount.entropyStore, getPwdInputText()
                    )
                    dismiss()
                    RememberMnemonicActivity.show4Backup(
                        context = this@MeActivity,
                        mnemonicText = mnemonicTxt,
                        accountName = AccountCenter.getCurrentAccount()?.name ?: "",
                        passphrase = getPwdInputText()
                    )
                } catch (e: Exception) {
                    Toast.makeText(activity, R.string.password_error, Toast.LENGTH_SHORT).show()
                }
            }
            setSecondButton(getString(R.string.temprory_not_backup)) {
                dismiss()
            }
            show()
        }
    }
}