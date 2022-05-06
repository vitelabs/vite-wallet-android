package net.vite.wallet.balance.walletconnect.taskdetail

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R


class ConfirmInfoAdapter(var confirmInfo: WCConfirmInfo) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_wc_tx_detail_title -> TitleTextVH.create(parent)
            R.layout.item_wc_tx_detail_normal -> NormalItemVH.create(parent)
            R.layout.item_wc_tx_detail_main_rich_style -> RichItemVH.create(parent)
            else -> TitleTextVH.create(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> R.layout.item_wc_tx_detail_title
            confirmInfo.listItems[position - 1].textColor != null -> R.layout.item_wc_tx_detail_main_rich_style
            else -> R.layout.item_wc_tx_detail_normal
        }
    }

    override fun getItemCount(): Int {
        return confirmInfo.listItems.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TitleTextVH -> holder.show(confirmInfo.title)
            is NormalItemVH -> holder.show(confirmInfo.listItems[position - 1])
            is RichItemVH -> holder.show(confirmInfo.listItems[position - 1])
        }
    }

}