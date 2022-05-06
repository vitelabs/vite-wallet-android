package net.vite.wallet.account.register

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R


class MnemonicVH(val view: View) : RecyclerView.ViewHolder(view) {
    val mnemonicText = view.findViewById<TextView>(R.id.mnemonicTxt)

    fun bind(mnemonic: String, position: Int, onClickListener: (position: Int) -> Unit = {}) {
        mnemonicText.text = mnemonic
        view.setOnClickListener {
            onClickListener(position)
        }
    }
}


class MnemonicAdapter(
    var list: ArrayList<String>,
    val onItemClick: (mnemonicAdapter: MnemonicAdapter, list: ArrayList<String>, position: Int) -> Unit = { _, _, _ -> }
) : RecyclerView.Adapter<MnemonicVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MnemonicVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mnemonic_textview, parent, false)
        return MnemonicVH(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MnemonicVH, position: Int) {
        holder.bind(list[position], position) { pos ->
            onItemClick(this, list, pos)
        }
    }
}