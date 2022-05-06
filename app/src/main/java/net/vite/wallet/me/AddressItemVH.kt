package net.vite.wallet.me

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter

class AddressItemVH(
    val view: View,
    val addressNameOnClick: (addressTextView: TextView, address: String) -> Unit
) :
    RecyclerView.ViewHolder(view) {
    private var address: String? = null
    var index: Int = 0

    private val addressTxt = view.findViewById<TextView>(R.id.addressTxt)
    private val addressName = view.findViewById<TextView>(R.id.addressName)
    private val addressOrder = view.findViewById<TextView>(R.id.addressOrder)
    val changeRadioButton = view.findViewById<RadioButton>(R.id.checkoutRadio)
    private val pasteAddr = view.findViewById<View>(R.id.pasteAddr)


    companion object {
        fun create(
            parent: ViewGroup,
            addressNameOnClick: (addressTextView: TextView, address: String) -> Unit
        ): AddressItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_address_list, parent, false)
            return AddressItemVH(view, addressNameOnClick)
        }
    }

    fun setAddress(address: String, index: Int) {
        this.address = address
        this.index = index
        addressName.text =
            AccountCenter.getCurrentAccount()?.getAddressName(address)
        addressName.setOnClickListener {
            addressNameOnClick.invoke(addressName, address)
        }
        addressTxt.text = address
        pasteAddr.setOnClickListener {
            (view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.let { cm ->
                cm.setPrimaryClip(ClipData.newPlainText(null, addressTxt.text))
                Toast.makeText(
                    view.context,
                    view.context.getString(R.string.copy_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        addressOrder.text = "#${index + 1}"
    }


}