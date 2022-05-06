package net.vite.wallet.balance.crosschain.deposit.list

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.network.http.gw.DepositRecord
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.vitebridge.H5WebActivity
import org.web3j.utils.Convert
import java.text.SimpleDateFormat


class DepositRecordVH(val view: View) : RecyclerView.ViewHolder(view) {
    val txTypeIcon = view.findViewById<ImageView>(R.id.txTypeIcon)
    val txStateTxt = view.findViewById<TextView>(R.id.txStateTxt)
    val failReason = view.findViewById<TextView>(R.id.failReason)
    val failLine = view.findViewById<View>(R.id.failLine)
    val txTimestamp = view.findViewById<TextView>(R.id.txTimestamp)
    val txAmount = view.findViewById<TextView>(R.id.txAmount)
    val startHashTxt = view.findViewById<TextView>(R.id.startHashTxt)
    val endHashTxt = view.findViewById<TextView>(R.id.endHashTxt)
    val feeAmount = view.findViewById<TextView>(R.id.feeAmount)

    companion object {
        val dataFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        fun create(parent: ViewGroup): DepositRecordVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_crosschain_tx_list, parent, false)
            return DepositRecordVH(view)
        }
    }

    fun bind(record: DepositRecord?, gwTokenInfo: NormalTokenInfo?) {
        if (record == null) return
        val tokenInfo = TokenInfoCenter.findTokenInfo { it.tokenAddress == record.tokenAddress }
        txAmount.visibility = View.VISIBLE
        failReason.visibility = View.GONE
        failLine.visibility = View.GONE
        feeAmount.text = record.fee?.toBigDecimalOrNull()?.let {
            "Fee ${Convert.fromWei(it, Convert.Unit.ETHER).toLocalReadableText(4)}"
        } ?: ""

        txAmount.text = record.amount?.toBigDecimalOrNull()?.let {
            tokenInfo?.amountTextWithSymbol(it.toBigInteger(), 4)
        } ?: ""
        txTimestamp.text = record.dateTime?.toLongOrNull()?.let {
            dataFormat.format(it)
        } ?: ""
        startHashTxt.text =
            "${gwTokenInfo?.platform} Hash: ${record.inTxHash ?: ""}"
        endHashTxt.text = "Vite Hash: ${record.outTxHash ?: ""}"
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
            DepositRecord.OPPOSITE_PROCESSING -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_eth_received)
                txStateTxt.text = txStateTxt.context.getString(
                    R.string.crosschain_opposite_processing,
                    tokenInfo?.symbol
                ) + " (${record.inTxConfirmedCount}/${record.inTxConfirmationCount})"
            }
            DepositRecord.OPPOSITE_CONFIRMED -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_gw_received)
                txStateTxt.setText(R.string.crosschain_opposite_confirmed)
            }
            DepositRecord.TOT_PROCESSING -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_vite_received)
                txStateTxt.text =
                    txStateTxt.context.getString(R.string.crosschain_tot_processing)
//                + " (${record.inTxConfirmedCount}/${record.inTxConfirmationCount})"
            }
            DepositRecord.TOT_CONFIRMED -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_complete)
                txStateTxt.setText(R.string.crosschain_tot_confirmed)
            }
            DepositRecord.BELOW_MINIMUM -> {
                txAmount.visibility = View.INVISIBLE
                failReason.visibility = View.VISIBLE
                failLine.visibility = View.VISIBLE

                txTypeIcon.setImageResource(R.mipmap.crosschain_record_failed)
                txStateTxt.setText(R.string.crosschain_deposit_failed)
                failReason.setText(R.string.crosschain_below_minimum)
            }
            DepositRecord.OPPOSITE_CONFIRMED_FAIL -> {
                txTypeIcon.setImageResource(R.mipmap.crosschain_record_failed)
                txStateTxt.setText(R.string.crosschain_deposit_failed)
            }
            else -> {
                txStateTxt.text = record.state ?: "null"
            }
        }


    }

}