package net.vite.wallet.contacts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog

import kotlinx.android.synthetic.main.activity_contact_edit.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.isValidEthAddress
import net.vite.wallet.isValidGrinContractAddress
import net.vite.wallet.utils.showToast
import net.vite.wallet.vep.EthUrlTransferParams
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.vep.ViteUrlTransferParams
import org.vitelabs.mobile.Mobile

class ContactEditActivity : UnchangableAccountAwareActivity() {

    companion object {
        private val MODE_ADD = 1
        private val MODE_EDIT = 2

        fun show(
            context: Context,
            contact: Contact? = null,
            fromWhere: String = Contact.FAMILT_NAME_VITE
        ) {
            val i = Intent(context, ContactEditActivity::class.java)
            i.putExtra("fromWhere", fromWhere)
            contact?.let {
                i.putExtra("address", it.address)
            }
            context.startActivity(i)
        }
    }

    private var mode = MODE_ADD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_edit)
        val address = intent.getStringExtra("address")
        mode = if (address == null) {
            lineDelete.visibility = View.GONE
            deleteContact.visibility = View.GONE
            titleText.setText(R.string.add_contact)
            Handler().postDelayed({
                contactTypeText.text = intent.getStringExtra("fromWhere")
            }, 10)
            MODE_ADD
        } else {
            titleText.setText(R.string.edit_contact)
            val contact = ContactCenter.contacts.find { it.address == address }
            if (contact == null) {
                finish()
                return
            }
            contactTypeText.text = contact.familyName
            addressInput.setText(contact.address)
            nameEditContent.setText(contact.name)
            lineDelete.visibility = View.VISIBLE
            deleteContact.visibility = View.VISIBLE
            deleteContact.setOnClickListener {
                AlertDialog.Builder(this)
                    .setMessage(R.string.confirm_delete_contact)
                    .setPositiveButton(R.string.confirm) { dialog, _ ->
                        ContactCenter.delete(contact.address)
                        dialog.dismiss()
                        finish()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            }
            MODE_EDIT
        }


        val arrayAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Contact.NowValidArray)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        familyNameSpinner.adapter = arrayAdapter
        familyNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                contactTypeText.text = Contact.NowValidArray[position]
            }
        }

        when (contactTypeText.text) {
            Contact.FAMILT_NAME_VITE ->
                familyNameSpinner.setSelection(0)
            Contact.FAMILT_NAME_ETH ->
                familyNameSpinner.setSelection(1)
            Contact.FAMILT_NAME_GRIN ->
                familyNameSpinner.setSelection(2)
        }


        contactTypeText.setOnClickListener {
            familyNameSpinner.performClick()
        }

        saveBtn.setOnClickListener {

            val nameInput = nameEditContent.editableText?.toString() ?: ""
            if (nameInput.isBlank()) {
                showToast(R.string.name_must_not_be_blank)
                nameEditContent.requestFocus()
                return@setOnClickListener
            }

            val family = contactTypeText.text.toString()
            val inputAddress = addressInput.editableText?.toString() ?: ""
            when (family) {
                Contact.FAMILT_NAME_ETH -> {
                    if (!isValidEthAddress(inputAddress)) {
                        showToast(R.string.input_address_not_valid)
                        addressInput.requestFocus()
                        return@setOnClickListener
                    }
                }
                Contact.FAMILT_NAME_VITE -> {
                    if (!Mobile.isValidAddress(inputAddress)) {
                        addressInput.requestFocus()
                        showToast(R.string.input_address_not_valid)
                        return@setOnClickListener
                    }
                }
                Contact.FAMILT_NAME_GRIN -> {
                    if (!isValidGrinContractAddress(inputAddress)) {
                        addressInput.requestFocus()
                        showToast(R.string.input_address_not_valid)
                        return@setOnClickListener
                    }
                }
                else -> {
                    return@setOnClickListener
                }
            }

            val newContact = Contact(
                address = inputAddress,
                name = nameInput,
                familyName = family,
                createTime = System.currentTimeMillis()
            )

            ContactCenter.contacts.indexOfFirst { it.address == inputAddress && it.familyName == contactTypeText.text }
                .let { index ->
                    if (index == -1) {
                        ContactCenter.add(newContact)
                        showToast(R.string.add_contact_success)
                        finish()
                        return@setOnClickListener
                    }

                    val contact = ContactCenter.contacts[index]

                    if (mode == MODE_ADD) {
                        AlertDialog.Builder(this)
                            .setMessage(
                                getString(
                                    R.string.already_exist_same_contact,
                                    contact.name
                                )
                            )
                            .setPositiveButton(R.string.yes) { dialog, _ ->
                                ContactCenter.delete(contact.address)
                                ContactCenter.add(newContact)
                                dialog.dismiss()
                                finish()
                            }
                            .setNegativeButton(R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setCancelable(false)
                            .create()
                            .show()
                    } else {
                        ContactCenter.delete(contact.address)
                        ContactCenter.add(newContact)
                        finish()
                    }
                }
        }


        scan.setOnClickListener {
            scanQrcode()
        }
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        if (contactTypeText.text == Contact.FAMILT_NAME_GRIN) {
            if (qrcodeParseResult.result is ViteUrlTransferParams) {
                contactTypeText.text = Contact.FAMILT_NAME_GRIN
                addressInput.setText(qrcodeParseResult.result.toAddr)
            } else if (isValidGrinContractAddress(qrcodeParseResult.rawData)) {
                contactTypeText.text = Contact.FAMILT_NAME_GRIN
                addressInput.setText(qrcodeParseResult.rawData)
            }
        } else {
            when (qrcodeParseResult.result) {
                is ViteUrlTransferParams -> {
                    contactTypeText.text = Contact.FAMILT_NAME_VITE
                    addressInput.setText(qrcodeParseResult.result.toAddr)
                }
                is EthUrlTransferParams -> {
                    contactTypeText.text = Contact.FAMILT_NAME_ETH
                    addressInput.setText(qrcodeParseResult.result.toAddr ?: "")
                }
                else -> {

                }
            }
        }
    }
}