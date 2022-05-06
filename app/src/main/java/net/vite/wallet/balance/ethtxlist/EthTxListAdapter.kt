package net.vite.wallet.balance.ethtxlist

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import net.vite.wallet.network.eth.EthTransaction

class EthTxListAdapter : PagedListAdapter<EthTransaction, EthTxItemVH>(AccountComparator) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EthTxItemVH {
        return EthTxItemVH.create(parent)
    }

    override fun onBindViewHolder(holder: EthTxItemVH, position: Int) {
        holder.bind(getItem(position))
    }


    companion object {
        val AccountComparator = object : DiffUtil.ItemCallback<EthTransaction>() {
            override fun areItemsTheSame(
                oldItem: EthTransaction,
                newItem: EthTransaction
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: EthTransaction,
                newItem: EthTransaction
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

}

