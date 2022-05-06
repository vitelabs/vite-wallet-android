package net.vite.wallet.me

import android.app.Activity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.AccountProfile
import net.vite.wallet.balance.walletconnect.session.VCFsmHolder
import net.vite.wallet.dialog.TextTitleNotifyDialog

class AddressListAdapter(
    val listIndex: MutableList<Int>,
    val defaultReceiverAddressName: TextView,
    val defaultReceiverAddress: TextView,
    val currentAcc: AccountProfile,
    val addressNameOnClick: (addressTextView: TextView, address: String) -> Unit
) :
    RecyclerView.Adapter<AddressItemVH>() {

    private var selectedIndex = -1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressItemVH {
        return AddressItemVH.create(parent, addressNameOnClick)
    }

    override fun getItemCount() = listIndex.size

    override fun onBindViewHolder(holder: AddressItemVH, position: Int) {
    }


    fun changeAddress(position: Int, holder: AddressItemVH) {
        currentAcc.changeViteByBip44Index(listIndex[position])?.let { newAddr ->
            holder.changeRadioButton.isChecked = true
            notifyItemChanged(selectedIndex)
            selectedIndex = position
            defaultReceiverAddress.text = newAddr
            defaultReceiverAddressName.text =
                AccountCenter.getCurrentAccount()?.getAddressName(newAddr)
                    ?: defaultReceiverAddress.context.getString(
                        R.string.unname
                    )
        }
    }

    override fun onBindViewHolder(holder: AddressItemVH, position: Int, payloads: MutableList<Any>) {
        currentAcc.getViteAddressByBip44Index(listIndex[position])?.let { addr ->
            if (payloads.isEmpty()) {
                holder.setAddress(addr, position)
            }
            if (addr == currentAcc.nowViteAddress()) {
                holder.changeRadioButton.isChecked = true
                selectedIndex = position
            } else {
                holder.changeRadioButton.isChecked = false
            }


            holder.view.setOnClickListener {
                holder.changeRadioButton.isChecked = true
                if (position != selectedIndex) {
                    if (VCFsmHolder.hasExistSession()) {
                        with(TextTitleNotifyDialog(holder.view.context as Activity)) {
                            setTitle(R.string.warm_notice)
                            setMessage(R.string.vitebirost_already_connected_change_address)
                            setCancelable(true)
                            setBottom(R.string.confirm) {
                                it.dismiss()
                                VCFsmHolder.close()
                                changeAddress(position, holder)
                            }
                            setBottomLeft(R.string.cancel) {
                                it.dismiss()
                            }
                            show()
                        }
                    } else {
                        changeAddress(position, holder)
                    }
                }
            }

        }


    }
}