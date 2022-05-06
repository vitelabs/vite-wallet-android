package net.vite.wallet.balance.walletconnect.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.balance.walletconnect.session.VcRequestTask
import java.text.SimpleDateFormat

class VCSignListAdapter(var data: List<VcRequestTask> = ArrayList()) :
    RecyclerView.Adapter<SignListViewHolder>() {

    override fun onBindViewHolder(holder: SignListViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignListViewHolder {
        return SignListViewHolder.create(parent)
    }

    override fun getItemCount(): Int = data.size
}


class SignListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var textViewType: TextView = view.findViewById(R.id.signListType)
    var textViewTime: TextView = view.findViewById(R.id.signListTime)
    var textViewState: TextView = view.findViewById(R.id.signListState)

    companion object {
        fun create(parent: ViewGroup): SignListViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_vc_sign_list, parent, false)
            return SignListViewHolder(view)
        }

        private val dataFormat = SimpleDateFormat(" HH:mm:ss")
    }

    fun bind(task: VcRequestTask) {
        textViewState.setText(
            when (task.currentState) {
                VcRequestTask.WaitUserProcessing -> {
                    R.string.sign_processing
                }
                VcRequestTask.AutoProcessing -> {
                    R.string.sign_processing
                }
                VcRequestTask.Success -> {
                    R.string.success
                }
                VcRequestTask.Cancel -> {
                    R.string.cancel
                }
                VcRequestTask.Failed -> {
                    R.string.failed
                }
                VcRequestTask.Pending -> {
                    R.string.wait_sign
                }
                else -> {
                    R.string.wait_sign
                }
            }
        )
        textViewTime.text = dataFormat.format(task.timepStamp)
        textViewType.text = task.confirmInfo.title
        if (textViewType.text.isEmpty()) {
            textViewType.setText(R.string.transactions)
        }
    }


}
