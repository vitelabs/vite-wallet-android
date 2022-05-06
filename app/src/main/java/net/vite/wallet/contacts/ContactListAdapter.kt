package net.vite.wallet.contacts

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ContactListAdapter(val contacts: List<Contact>) : RecyclerView.Adapter<ContactItemVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactItemVH {
        return ContactItemVH.create(parent)
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    override fun onBindViewHolder(holder: ContactItemVH, position: Int) {
        holder.update(contacts[position])
    }
}