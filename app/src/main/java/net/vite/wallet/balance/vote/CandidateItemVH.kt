package net.vite.wallet.balance.vote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.rpc.CandidateItem
import net.vite.wallet.network.rpc.ViteTokenInfo
import net.vite.wallet.utils.dp2px
import net.vite.wallet.vitebridge.H5WebActivity
import java.math.BigInteger

class CandidateItemVH(val view: View) : RecyclerView.ViewHolder(view) {

    val sbpName = view.findViewById<TextView>(R.id.sbpName)
    val sbpRank = view.findViewById<TextView>(R.id.sbpRank)
    val digitNumBg = view.findViewById<ImageView>(R.id.digitNumBg)
    val voteAddr = view.findViewById<TextView>(R.id.voteAddr)
    val voteNum = view.findViewById<TextView>(R.id.voteNum)
    val voteBtn = view.findViewById<TextView>(R.id.voteBtn)
    val itemRoot = view.findViewById<View>(R.id.itemRoot)

    companion object {
        fun create(parent: ViewGroup): CandidateItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sbp_list, parent, false)
            return CandidateItemVH(view)
        }
    }

    fun bind(item: CandidateItem, voteFunc: (name: String) -> Unit = {}) {
        sbpName.text = item.name
        voteNum.text =
            ViteTokenInfo.amountText(item.voteNum.toBigIntegerOrNull() ?: BigInteger.ZERO, 4)

        voteAddr.text = item.nodeAddr
        sbpRank.text = item.rank.toString()
        var width = 0.0F
        var imgId = -1
        when (sbpRank.text.length) {
            1 -> {
                width = 20.0F.dp2px()
                imgId = R.mipmap.vote_rank_digit_1
            }
            2 -> {
                width = 25.0F.dp2px()
                imgId = R.mipmap.vote_rank_digit_2
            }
            3 -> {
                width = 30.0F.dp2px()
                imgId = R.mipmap.vote_rank_digit_3
            }
        }
        digitNumBg.setImageResource(imgId)
        digitNumBg.layoutParams.width = width.toInt()

        voteBtn.setOnClickListener {
            voteFunc(item.name)
        }

        itemRoot.setOnClickListener {
            H5WebActivity.show(
                itemRoot.context,
                "${NetConfigHolder.netConfig.explorerUrlPrefix}/sbp/${item.name}"
            )
        }

    }
}