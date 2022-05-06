package net.vite.wallet.balance.crosschain.withdraw.list

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.network.http.gw.WithdrawRecord
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.vitebridge.H5WebActivity
import java.text.SimpleDateFormat


class WithdrawRecordVH(val view: View) : RecyclerView.ViewHolder(view) {
    val txTypeIcon = view.findViewById<ImageView>(R.id.txTypeIcon)
    val txStateTxt = view.findViewById<TextView>(R.id.txStateTxt)
    val txTimestamp = view.findViewById<TextView>(R.id.txTimestamp)
    val txAmount = view.findViewById<TextView>(R.id.txAmount)
    val startHashTxt = view.findViewById<TextView>(R.id.startHashTxt)
    val endHashTxt = view.findViewById<TextView>(R.id.endHashTxt)
    val feeAmount = view.findViewById<TextView>(R.id.feeAmount)

    companion object {
        val dataFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        fun create(parent: ViewGroup): WithdrawRecordVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_crosschain_tx_list, parent, false)
            return WithdrawRecordVH(view)
        }
    }

    fun bind(record: WithdrawRecord?, gwTokenInfo: NormalTokenInfo?) {
        if (record == null) return
        val tokenInfo = TokenInfoCenter.findTokenInfo { it.tokenAddress == record.tokenAddress }

        txAmount.text = record.amount?.toBigDecimalOrNull()?.let {
            tokenInfo?.amountTextWithSymbol(it.toBigInteger(), 4)
        } ?: ""


        feeAmount.text = record.fee?.toBigDecimalOrNull()?.let {
            "Fee ${tokenInfo?.amountTextWithSymbol(it.toBigInteger(), 4)}"
        } ?: ""
        txTimestamp.text = record.dateTime?.toLongOrNull()?.let {
            dataFormat.format(it)
        } ?: ""

        startHashTxt.text = "Vite Hash: ${record.inTxHash ?: ""}"
        endHashTxt.text =
            "${gwTokenInfo?.platform} Hash: ${record.outTxHash ?: ""}"
        if (!record.inTxHash.isNullOrEmpty()) {
            startHashTxt.setTextColor(Color.parseColor("#007AFF"))
            startHashTxt.setOnClickListener {
                val inTxHashFormat =
                    record.rawResponse?.inTxExplorerFormat ?: return@setOnClickListener
                if (inTxHashFormat.isNotEmpty()) {
                    H5WebActivity.show(
                        it.context,
                        inTxHashFormat.replace("{\$tx}", record.inTxHash)
                    )
                }
            }
        } else {
            startHashTxt.setOnClickListener { }
            startHashTxt.setTextColor(Color.parseColor("#733e4a59"))
        }

        if (!record.outTxHash.isNullOrEmpty()) {
            endHashTxt.setTextColor(Color.parseColor("#007AFF"))
            endHashTxt.setOnClickListener {
                val endTxHashFormat =
                    record.rawResponse?.outTxExplorerFormat ?: return@setOnClickListener
                if (endTxHashFormat.isNotEmpty()) {
                    H5WebActivity.show(
                        it.context,
                        endTxHashFormat.replace("{\$tx}", record.outTxHash)
                    )
                }
            }
        } else {
            endHashTxt.setOnClickListener { }
            endHashTxt.setTextColor(Color.parseColor("#733e4a59"))
        }

        when (record.state) {
            WithdrawRecord.OPPOSITE_PROCESSING -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_eth_received)
                txStateTxt.text = txStateTxt.context.getString(
                    R.string.crosschain_opposite_processing,
                    tokenInfo?.symbol
                )
            }
            WithdrawRecord.OPPOSITE_CONFIRMED -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_complete)
                txStateTxt.setText(R.string.crosschain_tot_confirmed)
            }
            WithdrawRecord.TOT_PROCESSING -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_vite_received)
                txStateTxt.text =
                    txStateTxt.context.getString(R.string.crosschain_tot_processing) + " (${record.inTxConfirmedCount}/${record.inTxConfirmationCount})"
            }
            WithdrawRecord.TOT_CONFIRMED -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_gw_received)
                txStateTxt.setText(R.string.crosschain_opposite_confirmed)
            }
            WithdrawRecord.TOT_EXCEED_THE_LIMIT -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_record_failed)
                txStateTxt.setText(R.string.crosschain_tot_exceed_the_limit)
            }
            WithdrawRecord.WRONG_WITHDRAW_ADDRESS -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_record_failed)
                txStateTxt.setText(R.string.crosschain_wrong_withdraw_address)
            }
            else -> {
                txStateTxt.text = record.state ?: "null"
            }
        }
    }

}