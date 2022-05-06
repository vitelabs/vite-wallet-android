package net.vite.wallet.dexassets

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.network.http.vitex.DexDepositWithdrawRecord
import net.vite.wallet.network.rpc.AccountBlock
import net.vite.wallet.utils.viewbinding.findMyView
import java.text.SimpleDateFormat

class DexItemTxAdapter : PagedListAdapter<DexTokenTxVo, ItemDexTokenTx>(comparator) {
    var bottomHeight = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemDexTokenTx {
        return ItemDexTokenTx.create(parent)
    }

    override fun onBindViewHolder(holder: ItemDexTokenTx, position: Int) {
        getItem(position)?.let {
            holder.bind(it, position == itemCount - 1, bottomHeight)
        }
    }

    companion object {
        val comparator = object : DiffUtil.ItemCallback<DexTokenTxVo>() {
            override fun areItemsTheSame(oldItem: DexTokenTxVo, newItem: DexTokenTxVo) =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: DexTokenTxVo, newItem: DexTokenTxVo) =
                oldItem == newItem
        }
    }
}


data class DexTokenTxVo(
    val amount: String,
    val time: Long,
    val symbol: String,
    val isIn: Boolean
) {
    companion object {
        fun fromVitex(r: DexDepositWithdrawRecord) = DexTokenTxVo(
            amount = r.amount,
            time = r.time,
            symbol = r.tokenSymbol,
            isIn = r.type == DexDepositWithdrawRecord.DEPOSIT
        )

        fun fromAccountBlock(ab: AccountBlock) = DexTokenTxVo(
            amount = ab.getAmountReadableText(8),
            time = ab.timestamp ?: 0L,
            symbol = ab.viteChainTokenInfo?.symbol ?: "",
            isIn = ab.isReceiveBlock()!!
        )
    }
}

class ItemDexTokenTx(val view: View) : RecyclerView.ViewHolder(view) {
    val txTypeIcon by findMyView<ImageView>(R.id.txTypeIcon)
    val txTypeTxt by findMyView<TextView>(R.id.txTypeTxt)
    val txTokenSymbol by findMyView<TextView>(R.id.txTokenSymbol)
    val txAmount by findMyView<TextView>(R.id.txAmount)
    val txTimestamp by findMyView<TextView>(R.id.txTimestamp)

    companion object {
        val dataFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        fun create(parent: ViewGroup): ItemDexTokenTx {
            return ItemDexTokenTx(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_dex_token_tx, parent, false)
            )
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(vo: DexTokenTxVo, islast: Boolean, bottomHeight: Int) {
        if (islast) {
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomHeight)
        } else {
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, 0)
        }
        with(vo) {
            txTypeIcon.setImageResource(
                if (isIn) {
                    R.mipmap.dex_token_detail_tx_type_transfer_in
                } else {
                    R.mipmap.dex_token_detail_tx_type_transfer_out
                }
            )
            txTypeTxt.setText(
                if (isIn) {
                    R.string.transfer_in
                } else {
                    R.string.transfer_out
                }
            )
            txTimestamp.text = dataFormat.format(time * 1000)
            txTokenSymbol.text = symbol
            txAmount.text = if (isIn) {
                "+"
            } else {
                "-"
            } + amount
        }
    }

}
