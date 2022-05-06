package net.vite.wallet.balance.crosschain.withdraw.list

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import net.vite.wallet.network.http.gw.WithdrawRecord
import net.vite.wallet.network.http.vitex.NormalTokenInfo

class WithdrawRecordsAdapter(var gwTokenInfo: NormalTokenInfo? = null) :
    PagedListAdapter<WithdrawRecord, WithdrawRecordVH>(
        DepositRecordComparator
    ) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WithdrawRecordVH {
        return WithdrawRecordVH.create(parent)
    }


    override fun onBindViewHolder(holder: WithdrawRecordVH, position: Int) {
        getItem(position)?.let {
            holder.bind(it, gwTokenInfo)
        }
    }

    companion object {
        val DepositRecordComparator = object : DiffUtil.ItemCallback<WithdrawRecord>() {
            override fun areItemsTheSame(
                oldItem: WithdrawRecord,
                newItem: WithdrawRecord
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: WithdrawRecord,
                newItem: WithdrawRecord
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

}

