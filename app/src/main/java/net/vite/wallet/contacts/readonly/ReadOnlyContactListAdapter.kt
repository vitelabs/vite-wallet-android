package net.vite.wallet.contacts.readonly

import android.app.Activity
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ReadOnlyContactListAdapter(val contacts: List<ReadOnlyItem>, val activity: Activity) :
    RecyclerView.Adapter<ReadOnlyItemVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ReadOnlyItemVH.create(parent, activity)

    override fun getItemCount() = contacts.size

    override fun onBindViewHolder(holder: ReadOnlyItemVH, position: Int) {
        holder.update(contacts[position])
    }
}