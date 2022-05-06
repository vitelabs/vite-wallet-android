package net.vite.wallet.buycoin.list

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import net.vite.wallet.network.http.coinpurchase.PurchaseRecordItem

class BuyCoinRecordsAdapter : PagedListAdapter<PurchaseRecordItem, BuyCoinRecordVH>(
    RecordComparator
) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyCoinRecordVH {
        return BuyCoinRecordVH.create(parent)
    }


    override fun onBindViewHolder(holder: BuyCoinRecordVH, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    companion object {
        val RecordComparator = object : DiffUtil.ItemCallback<PurchaseRecordItem>() {
            override fun areItemsTheSame(oldItem: PurchaseRecordItem, newItem: PurchaseRecordItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: PurchaseRecordItem, newItem: PurchaseRecordItem): Boolean {
                return oldItem == newItem
            }

        }
    }

}

