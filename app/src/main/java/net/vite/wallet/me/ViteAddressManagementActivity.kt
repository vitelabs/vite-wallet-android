package net.vite.wallet.me

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_vite_address_mangement.*
import net.vite.wallet.R
import net.vite.wallet.ViteApplication
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.walletconnect.session.VCFsmHolder
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.utils.showToast

class ViteAddressManagementActivity : UnchangableAccountAwareActivity() {

    lateinit var adapter: AddressListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vite_address_mangement)
        if (savedInstanceState != null) {
            (applicationContext as ViteApplication).restartApp()
            return
        }

        val currentAccount = AccountCenter.getCurrentAccount()!!

        inforImg.setOnClickListener {
            inforBubbleTxt.visibility = if (inforBubbleTxt.visibility == View.GONE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        val addressNameOnClick: (addressTextView: TextView, address: String) -> Unit =
            { addressItemTextView, address ->
                BigDialog(this).apply {
                    textInput.visibility = View.VISIBLE
                    textInput.inputType = EditorInfo.TYPE_CLASS_TEXT
                    if (currentAccount.getAddressName(address) != getString(R.string.unname)) {
                        textInput.setText(currentAccount.getAddressName(address))
                    }
                    inputLayout.visibility = View.VISIBLE
                    inputLayout.hint = getString(R.string.address_name_title_tag)
                    setBigTitle(getString(R.string.change_address_name_title))
                    setFirstButton(getString(R.string.confirm)) {
                        val newName = textInput.editableText.toString().trim()
                        if (newName.isBlank()) {
                            showToast(R.string.address_name_can_not_blank)
                        } else if (newName != currentAccount.getAddressName(address)) {

                            if (VCFsmHolder.hasExistSession()) {
                                with(TextTitleNotifyDialog(this@ViteAddressManagementActivity)) {
                                    setTitle(R.string.warm_notice)
                                    setMessage(R.string.wc_changeaddr_notice)
                                    setBottom(R.string.confirm) { warmNoticeDialog ->
                                        warmNoticeDialog.dismiss()
                                        VCFsmHolder.close()

                                        if (currentAccount.changeAddressName(
                                                address,
                                                newName
                                            ) == true
                                        ) {
                                            addressItemTextView.text = newName
                                            this@ViteAddressManagementActivity.addressName.text =
                                                newName
                                            showToast(R.string.change_success)
                                        }
                                    }
                                    setBottomLeft(R.string.cancel) { warmNoticeDialog ->
                                        warmNoticeDialog.dismiss()
                                    }
                                    show()
                                }
                            } else {
                                if (currentAccount.changeAddressName(address, newName) == true) {
                                    addressItemTextView.text = newName
                                    this@ViteAddressManagementActivity.addressName.text = newName
                                    showToast(R.string.change_success)
                                }
                            }
                        }
                        dismiss()
                    }
                    show()
                }
            }

        addressName.setOnClickListener {
            addressNameOnClick(addressName, defaultReceiverAddress.text.toString())
        }

        defaultReceiverAddress.text = currentAccount.nowViteAddress() ?: ""
        addressName.text = currentAccount.getAddressName(currentAccount.nowViteAddress() ?: "")

        adapter = AddressListAdapter(
            currentAccount.allIndex.toMutableList(),
            addressName,
            defaultReceiverAddress,
            currentAccount,
            addressNameOnClick
        )
        val linearLayoutManager = LinearLayoutManager(this)
        addressMainList.layoutManager = linearLayoutManager
        addressMainList.adapter = adapter


        if (currentAccount.nowMaxIndex == 99) {
            addAddressBtn.visibility = View.GONE
        } else {
            addAddressBtn.visibility = View.VISIBLE
            addAddressBtn.setOnClickListener {
                showDialog()
            }
        }

    }

    fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.add_address))
        builder.setMessage(getString(R.string.add_address_dialog_message))
        val dialogLayout =
            LayoutInflater.from(this).inflate(R.layout.dialog_add_address_intput, null)
        val input = dialogLayout.findViewById<EditText>(R.id.dialog_add_address_edt)
        builder.setView(dialogLayout)
        builder.setPositiveButton(getString(R.string.confirm)) { dialog, which ->
            val inputText = input.text.toString()
            val curIndex = currentAccount.allIndex.size
            if (inputText.toIntOrNull() == null) {

            } else if (inputText.toInt() > 100) {
                showToast(R.string.add_address_over_limit)
            } else if (inputText.toInt() < curIndex + 1) {
                showToast(R.string.add_address_toast_exist)
            } else {
                recoveryAddresses(inputText.toInt())
            }
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, which -> dialog.cancel() }
        builder.show()
    }

    fun recoveryAddresses(index: Int) {
        val curIndex = currentAccount.allIndex.size - 1

        for (i in curIndex until index - 1) {
            if (currentAccount.nowMaxIndex == 99) {
//                Toast.makeText(this, R.string.address_size_exceed, Toast.LENGTH_SHORT).show()
                return
            }

            currentAccount.deriveNewViteAddress()?.let {
                adapter.listIndex.add(it.first)
                adapter.notifyDataSetChanged()
            }
            if (currentAccount.nowMaxIndex == 99) {
                addAddressBtn.visibility = View.GONE
            }
        }
    }
}
