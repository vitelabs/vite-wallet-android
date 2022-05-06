package net.vite.wallet.balance.ethtxlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_eth_tx_detail.*
import net.vite.wallet.R
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.eth.EthTransaction
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.hexToBytes
import java.nio.charset.Charset

class EthTxDetailActivity : BaseActivity() {
    companion object {
        fun show(context: Context, address: String, transaction: EthTransaction) {
            context.startActivity(
                Intent(context, EthTxDetailActivity::class.java).putExtra(
                    "address", address
                ).putExtra("transaction", Gson().toJson(transaction))
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eth_tx_detail)

        val transaction =
            Gson().fromJson(intent.getStringExtra("transaction"), EthTransaction::class.java)
        val nowAddr = intent.getStringExtra("address")
        if (transaction == null) {
            finish()
        }

        transactionTime.text = transaction.timeStamp.toLongOrNull()?.let {
            if (it == 0L) {
                ""
            } else {
                EthTxItemVH.dataFormat.format(it * 1000)
            }
        } ?: ""

        transferResultTxt.text =
            transferResultTxt.context.getString(if (transaction.isError == "1") R.string.transfer_failed else R.string.transfer_success)
        transferResultImg.setImageResource(if (transaction.isError == "1") R.mipmap.transfer_failed_icon else R.mipmap.transfer_success_icon)

        toAddress.text = transaction.to
        fromAddress.text = transaction.from
        amount.text =
            transaction.getAmountReadableText(4, transaction.tokenDecimal?.toIntOrNull() ?: 18)
        minerFee.text =
            transaction.getGastReadableText(8, 18)
        hash.text = transaction.hash
        block.text = transaction.blockNumber
        if (transaction.input == null || transaction.input == "0x") {
            momoTxt.visibility = View.GONE
        } else {
            momoTxt.visibility = View.VISIBLE

            momo.text =
                transaction.input.substring(2).hexToBytes().toString(Charset.forName("UTF-8"))
        }
        tokenName1.text = transaction.tokenSymbol ?: "ETH"
        tokenName2.text = "ETH"

        pasteAddrTo.setOnClickListener {
            copyToBoard(toAddress)
        }

        pasteHash.setOnClickListener {
            copyToBoard(hash)
        }

        pasteAddrFrom.setOnClickListener {
            copyToBoard(fromAddress)
        }

        seeDetail.setOnClickListener {
            startActivity(createBrowserIntent("${NetConfigHolder.netConfig.remoteEtherscanPrefix}/address/$nowAddr"))
        }

    }

    fun copyToBoard(targetView: TextView) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.let { cm ->
            cm.setPrimaryClip(ClipData.newPlainText(null, targetView.text))
            Toast.makeText(this, this.getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }
    }

}
