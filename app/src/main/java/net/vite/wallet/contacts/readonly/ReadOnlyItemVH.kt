package net.vite.wallet.contacts.readonly

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R


data class ReadOnlyItem(
    val name: String,
    val family: String,
    val familyColor: Int,
    val icon: Int,
    val address: String,
    var order: Int? = 0
)


class ReadOnlyItemVH(val view: View, val activity: Activity) : RecyclerView.ViewHolder(view) {
    val contactName = view.findViewById<TextView>(R.id.contactName)
    val familyName = view.findViewById<TextView>(R.id.familyName)
    val address = view.findViewById<TextView>(R.id.address)
    val contactIcon = view.findViewById<ImageView>(R.id.contactIcon)
    val addressOrder = view.findViewById<TextView>(R.id.addressOrder)

    companion object {
        fun create(parent: ViewGroup, activity: Activity): ReadOnlyItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_contact_readonly, parent, false)
            return ReadOnlyItemVH(view, activity)
        }
    }

    fun update(item: ReadOnlyItem) {
        view.setOnClickListener {
            activity.setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("address", item.address)
            })
            activity.finish()
        }
        contactName.text = item.name
        familyName.text = item.family
        familyName.setTextColor(item.familyColor)
        familyName.setBackgroundColor(
            when (item.family) {
                "ETH" -> Color.parseColor("#F8FFF2")
                "VITE" -> Color.parseColor("#F2F8FF")
                "GRIN" -> Color.parseColor("#FFF9E1")
                else -> Color.parseColor("#F2F8FF")
            }
        )

        if (item.family == "ETH" || item.family == "VITE") {
            contactIcon.visibility = View.GONE
            addressOrder.visibility = View.VISIBLE
            addressOrder.text = "#${item.order}"
        } else {
            contactIcon.visibility = View.VISIBLE
            addressOrder.visibility = View.GONE
            contactIcon.setImageResource(item.icon)
        }
        address.text = item.address
    }
}
