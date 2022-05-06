package net.vite.wallet.contacts.readonly

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_readonly_contact_list.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.contacts.Contact
import net.vite.wallet.contacts.ContactCenter


class ReadOnlyContactListActivity : UnchangableAccountAwareActivity() {

    companion object {
        val VITE_CONTACT = 100
        val ETH_CONTACT = 101
        val GRIN_CONTACT = 103
        val MY_ADDRESS = 102
        val MY_ETH_ADDRESS = 104

        fun show(activity: Activity, type: Int, requestCode: Int) {
            activity.startActivityForResult(
                Intent(
                    activity,
                    ReadOnlyContactListActivity::class.java
                ).apply {
                    putExtra("type", type)
                }, requestCode
            )
        }
    }

    private var type = VITE_CONTACT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_readonly_contact_list)
        type = intent.getIntExtra(
            "type",
            VITE_CONTACT
        )
        val list = when (type) {
            VITE_CONTACT -> {
                titleText.setText(R.string.vite_contact_address)
                ContactCenter.contacts.filter { it.familyName == Contact.FAMILT_NAME_VITE }.map {
                    ReadOnlyItem(
                        name = it.name,
                        family = it.familyName,
                        familyColor = Color.parseColor("#ff007aff"),
                        icon = R.mipmap.contact_item_icon,
                        address = it.address
                    )
                }
            }
            ETH_CONTACT -> {
                titleText.setText(R.string.eth_contact_address)
                ContactCenter.contacts.filter { it.familyName == Contact.FAMILT_NAME_ETH }.map {
                    ReadOnlyItem(
                        name = it.name,
                        family = it.familyName,
                        familyColor = Color.parseColor("#ff5bc500"),
                        icon = R.mipmap.contact_item_icon,
                        address = it.address
                    )
                }
            }
            GRIN_CONTACT -> {
                titleText.setText(R.string.grin_contact_address)
                ContactCenter.contacts.filter { it.familyName == Contact.FAMILT_NAME_GRIN }.map {
                    ReadOnlyItem(
                        name = it.name,
                        family = it.familyName,
                        familyColor = Color.parseColor("#ffFFDB4D"),
                        icon = R.mipmap.contact_item_icon,
                        address = it.address
                    )
                }
            }
            MY_ADDRESS -> {
                val currentAccount = AccountCenter.getCurrentAccount()!!

                titleText.setText(R.string.my_address)
                currentAccount.nowDerivedViteAddresses().map { addr ->
                    ReadOnlyItem(
                        name = currentAccount.getAddressName(addr),
                        family = "VITE",
                        familyColor = Color.parseColor("#ff007aff"),
                        icon = R.mipmap.my_address_icon,
                        address = addr
                    )
                } ?: emptyList()
            }
            MY_ETH_ADDRESS -> {
                val currentAccount = AccountCenter.getCurrentAccount()!!

                titleText.setText(R.string.my_address)
                currentAccount.nowDerivedEthAddresses().map { addr ->
                    ReadOnlyItem(
                        name = currentAccount.getAddressName(addr),
                        family = "ETH",
                        familyColor = Color.parseColor("#ff5bc500"),
                        icon = R.mipmap.my_address_icon,
                        address = addr
                    )
                } ?: emptyList()
            }
            else -> emptyList()
        }

        if (type == MY_ADDRESS || type == MY_ETH_ADDRESS) {
            list.forEach {
                it.order = list.indexOf(it) + 1
            }
        }
        val adapter = ReadOnlyContactListAdapter(list, this)
        mainList.adapter = adapter

        if (list.isEmpty()) {
            emptyImg.visibility = View.VISIBLE
            emptyTxt.visibility = View.VISIBLE
            mainList.visibility = View.GONE
            emptyTxt.text = getString(
                R.string.none_contact_platform, when (type) {
                    VITE_CONTACT -> "VITE"
                    ETH_CONTACT -> "ETH"
                    else -> ""
                }
            )
        } else {
            mainList.visibility = View.VISIBLE
            emptyImg.visibility = View.GONE
            emptyTxt.visibility = View.GONE
        }

    }

}