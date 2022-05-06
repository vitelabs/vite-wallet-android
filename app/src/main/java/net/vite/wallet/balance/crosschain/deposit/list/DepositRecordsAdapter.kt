package net.vite.wallet.balance.crosschain.deposit.list

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import net.vite.wallet.network.http.gw.DepositRecord
import net.vite.wallet.network.http.vitex.NormalTokenInfo

class DepositRecordsAdapter(var gwTokenInfo: NormalTokenInfo? = null) : PagedListAdapter<DepositRecord, DepositRecordVH>(
    DepositRecordComparator
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepositRecordVH {
        return DepositRecordVH.create(parent)
    }

    override fun onBindViewHolder(holder: DepositRecordVH, position: Int) {
        getItem(position)?.let {
            holder.bind(it,gwTokenInfo)
        }
    }

    companion object {
        val DepositRecordComparator = object : DiffUtil.ItemCallback<DepositRecord>() {
            override fun areItemsTheSame(oldItem: DepositRecord, newItem: DepositRecord): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: DepositRecord, newItem: DepositRecord): Boolean {
                return oldItem == newItem
            }

        }
    }

}

