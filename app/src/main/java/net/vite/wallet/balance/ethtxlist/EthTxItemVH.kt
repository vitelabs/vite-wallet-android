package net.vite.wallet.balance.ethtxlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.network.eth.EthTransaction
import java.text.SimpleDateFormat

class EthTxItemVH(val view: View) : RecyclerView.ViewHolder(view) {


    companion object {
        val dataFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        fun create(parent: ViewGroup): EthTxItemVH {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_tx_eth_list, parent, false)
            return EthTxItemVH(view)
        }
    }

    private val typeIcon = view.findViewById<ImageView>(R.id.txTypeIcon)
    private val txTimestamp = view.findViewById<TextView>(R.id.txTimestamp)
    private val txAddr = view.findViewById<TextView>(R.id.txAddr)
    private val txAmount = view.findViewById<TextView>(R.id.txAmount)
    private val txTokenSymbol = view.findViewById<TextView>(R.id.txTokenSymbol)
    private val txTypeTxt = view.findViewById<TextView>(R.id.txTypeTxt)
    private val txFee = view.findViewById<TextView>(R.id.txFee)
    private val errorTxt = view.findViewById<TextView>(R.id.errorTxt)

    fun bind(transaction: EthTransaction?) {
        transaction?.let { transaction ->

            txTypeTxt.text = txTypeTxt.context.getString(R.string.eth_transfer_type)

            txTimestamp.text = transaction.timeStamp.toLongOrNull()?.let {
                if (it == 0L) {
                    ""
                } else {
                    dataFormat.format(it * 1000)
                }
            } ?: ""

            txTokenSymbol.text = transaction.tokenSymbol ?: "ETH"
            txFee.text =
                txFee.context.getString(R.string.eth_miner_fee) + " " + transaction.getGastReadableText(
                    8,
                    18
                )

            errorTxt.visibility = if (transaction.isError == "1") View.VISIBLE else View.GONE

            if (AccountCenter.currentEthAddress().equals(transaction.to, true)) { // receive
                txAmount.setTextColor(Color.parseColor("#ff5bc500"))
                typeIcon.setImageResource(R.mipmap.tx_icon_receive)
                txAmount.text = "+" + transaction.getEthAmountReadableText()
                txAddr.text = transaction.from
            } else {
                txAmount.setTextColor(Color.parseColor("#ffff0008"))
                typeIcon.setImageResource(R.mipmap.tx_icon_transfer)
                txAmount.text = "-" + transaction.getEthAmountReadableText()
                txAddr.text = transaction.to
            }

            view.setOnClickListener {
                EthTxDetailActivity.show(
                    view.context, AccountCenter.currentEthAddress()!!,
                    transaction
                )
            }
        }

    }

}