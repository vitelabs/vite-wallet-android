package net.vite.wallet.balance.walletconnect.taskdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R

class NormalItemVH(val view: View) : RecyclerView.ViewHolder(view) {

    val keyText = view.findViewById<TextView>(R.id.keyText)
    val valueText = view.findViewById<TextView>(R.id.valueText)

    companion object {
        fun create(parent: ViewGroup): NormalItemVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wc_tx_detail_normal, parent, false)
            return NormalItemVH(view)
        }
    }

    fun show(itemInfo: WCConfirmItemInfo) {
        keyText.text = itemInfo.title
        valueText.text = itemInfo.text
    }
}


class RichItemVH(val view: View) : RecyclerView.ViewHolder(view) {

    val keyText = view.findViewById<TextView>(R.id.keyText)
    val valueText = view.findViewById<TextView>(R.id.valueText)

    companion object {
        fun create(parent: ViewGroup): RichItemVH {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_wc_tx_detail_main_rich_style, parent, false)
            return RichItemVH(view)
        }
    }

    fun show(itemInfo: WCConfirmItemInfo) {
        keyText.text = itemInfo.title
        valueText.text = itemInfo.text
        itemInfo.backgroundColor?.let {
            view.setBackgroundColor(itemInfo.backgroundColor)
        }
        itemInfo.textColor?.let {
            valueText.setTextColor(itemInfo.textColor)
        }
    }
}

class TitleTextVH(val view: View) : RecyclerView.ViewHolder(view) {
    val titleText = view.findViewById<TextView>(R.id.titleText)

    companion object {
        fun create(parent: ViewGroup): TitleTextVH {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_wc_tx_detail_title, parent, false)
            return TitleTextVH(view)
        }
    }

    fun show(title: String) {
        titleText.text = title
    }
}