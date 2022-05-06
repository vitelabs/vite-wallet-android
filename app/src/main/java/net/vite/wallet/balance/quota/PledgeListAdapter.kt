package net.vite.wallet.balance.quota

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import net.vite.wallet.network.rpc.PledgeInfo

class PledgeListAdapter(
    val onPledgeInfoRetrieve: onPledgeInfoRetrieve,
    val listOfCancelPledging: ArrayList<String>
) : PagedListAdapter<PledgeInfo, PledgeItemVH>(PledgeInfoComparator) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PledgeItemVH {
        return PledgeItemVH.create(parent, onPledgeInfoRetrieve)
    }

    override fun onBindViewHolder(holder: PledgeItemVH, position: Int) {
        holder.bind(getItem(position), listOfCancelPledging)
    }

    companion object {
        val PledgeInfoComparator = object : DiffUtil.ItemCallback<PledgeInfo>() {
            override fun areItemsTheSame(oldItem: PledgeInfo, newItem: PledgeInfo): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: PledgeInfo, newItem: PledgeInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}

