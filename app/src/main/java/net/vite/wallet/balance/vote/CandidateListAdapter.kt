package net.vite.wallet.balance.vote

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.network.rpc.CandidateItem

class CandidateListAdapter(var candidateList: List<CandidateItem>, var voteFunc: (name: String) -> Unit = {}) :
    RecyclerView.Adapter<CandidateItemVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemVH {
        return CandidateItemVH.create(parent)
    }

    override fun getItemCount(): Int {
        return candidateList.size
    }

    override fun onBindViewHolder(holder: CandidateItemVH, position: Int) {
        holder.bind(candidateList[position], voteFunc)
    }

}