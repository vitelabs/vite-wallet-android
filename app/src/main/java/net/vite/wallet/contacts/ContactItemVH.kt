package net.vite.wallet.contacts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R


class ContactItemVH(val view: View) : RecyclerView.ViewHolder(view) {

    val contactName = view.findViewById<TextView>(R.id.contactName)
    val familyName = view.findViewById<TextView>(R.id.familyName)
    val address = view.findViewById<TextView>(R.id.address)
    val edit = view.findViewById<View>(R.id.edit)
    val paste = view.findViewById<View>(R.id.paste)

    companion object {
        fun create(parent: ViewGroup): ContactItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_contact, parent, false)
            return ContactItemVH(view)
        }
    }

    fun update(c: Contact) {
        contactName.text = c.name
        familyName.text = c.familyName
        familyName.setTextColor(
            when (c.familyName) {
                Contact.FAMILT_NAME_ETH -> Color.parseColor("#5bc500")
                Contact.FAMILT_NAME_VITE -> Color.parseColor("#007AFF")
                Contact.FAMILT_NAME_GRIN -> Color.parseColor("#FF9C00")
                else -> Color.BLACK
            }
        )
        familyName.setBackgroundColor(
            when (c.familyName) {
                Contact.FAMILT_NAME_ETH -> Color.parseColor("#F8FFF2")
                Contact.FAMILT_NAME_VITE -> Color.parseColor("#F2F8FF")
                Contact.FAMILT_NAME_GRIN -> Color.parseColor("#FFF9E1")
                else -> Color.WHITE
            }
        )

        address.text = c.address

        edit.setOnClickListener {
            ContactEditActivity.show(it.context, c, c.familyName)
        }
        paste.setOnClickListener {
            (view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.let { cm ->
                cm.setPrimaryClip(ClipData.newPlainText(null, c.address))
                Toast.makeText(
                    view.context,
                    view.context.getString(R.string.copy_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

}