package net.vite.wallet.balance.txlist

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import net.vite.wallet.network.rpc.AccountBlock

class TxListAdapter : PagedListAdapter<AccountBlock, TxItemVH>(AccountComparator) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxItemVH {
        return TxItemVH.create(parent)
    }

    override fun onBindViewHolder(holder: TxItemVH, position: Int) {
        holder.bind(getItem(position))
    }


    companion object {
        val AccountComparator = object : DiffUtil.ItemCallback<AccountBlock>() {
            override fun areItemsTheSame(oldItem: AccountBlock, newItem: AccountBlock): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: AccountBlock, newItem: AccountBlock): Boolean {
                return oldItem == newItem
            }

        }
    }

}

