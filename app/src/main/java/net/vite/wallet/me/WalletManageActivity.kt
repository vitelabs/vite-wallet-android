package net.vite.wallet.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_wallet_manage.*
import net.vite.wallet.R
import net.vite.wallet.Wallet
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.loge

class WalletManageActivity : UnchangableAccountAwareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_manage)

        addressManage.setOnClickListener {
            startActivity(Intent(this, ViteAddressManagementActivity::class.java))
        }

        extractMnemonic.setOnClickListener {
            BigDialog(this).let { dialog ->
                dialog.setBigTitle(getString(R.string.extract_mnemonic))
                dialog.setPasswordEnable(true)
                dialog.setFirstButton(getString(R.string.confirm)) {
                    try {
                        val mnemonicTxt = Wallet.instance.extractMnemonic(
                            currentAccount.entropyStore,
                            dialog.getPwdInputText()
                        )
                        dialog.dismiss()
                        BigDialog(this).let { showMnemonicDialog ->
                            showMnemonicDialog.setBigTitle(getString(R.string.extract_mnemonic))
                            showMnemonicDialog.setMessageWithDarkBg(mnemonicTxt)
                            showMnemonicDialog.setFirstButton(getString(R.string.confirm)) {
                                showMnemonicDialog.dismiss()
                            }
                            showMnemonicDialog.show()
                        }

                    } catch (e: Exception) {
                        loge(e)
                        Toast.makeText(this, R.string.password_error, Toast.LENGTH_SHORT).show()
                    }

                }

                dialog.show()
            }

        }

        walletNameEditText.isCursorVisible = false
        walletNameEditText.isFocusable = false
        walletNameEditText.isFocusableInTouchMode = false
        walletNameEditText.setOnClickListener {
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
                        Toast.makeText(this@WalletManageActivity, R.string.acc_name_can_not_blank, Toast.LENGTH_SHORT)
                            .show()
                    } else if (newName != currentAccount.name) {
                        if (AccountCenter.changeUserName(newName)) {
                            this@WalletManageActivity.walletNameEditText.setText(
                                currentAccount.name
                            )
                            Toast.makeText(this@WalletManageActivity, R.string.change_success, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    dismiss()
                }
                show()
            }

        }

        walletNameEditText.setText(currentAccount.name)


    }


}
