package net.vite.wallet.balance

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_vite_tx_detail.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.loge
import net.vite.wallet.network.rpc.AccountBlock
import net.vite.wallet.network.rpc.AccountBlock.Companion.BlockTypeSendCall
import net.vite.wallet.network.rpc.AsyncNet
import net.vite.wallet.network.rpc.logi
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.fromBase64ToBytesOrNull
import net.vite.wallet.utils.isUTF8
import net.vite.wallet.utils.toHex
import org.vitelabs.mobile.Mobile
import java.text.SimpleDateFormat

class ViteTxDetailActivity : BaseActivity() {
    companion object {
        fun show(context: Context, accountBlock: AccountBlock, url: String) {
            context.startActivity(
                Intent(context, ViteTxDetailActivity::class.java).putExtra(
                    "accountBlock",
                    Gson().toJson(accountBlock)
                ).putExtra("url", url)
            )
        }
    }

    val dataFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vite_tx_detail)

        val accountBlock =
            Gson().fromJson(intent.getStringExtra("accountBlock"), AccountBlock::class.java)
        if (accountBlock == null) {
            finish()
        }

        val url = intent.getStringExtra("url")!!

        val blockDetailType = accountBlock.blockType

        transactionTime.text = accountBlock.timestamp?.let {
            if (it == 0L) {
                ""
            } else {
                dataFormat.format(it * 1000)
            }
        } ?: ""

        if (Mobile.isContactAddress(accountBlock.toAddress)) {// contact
            if (accountBlock.receiveBlockHash == null) {
                transferResultTxt.text =
                    transferResultTxt.context.getString(R.string.contract_call_pending)
                transferResultImg.setImageResource(R.mipmap.transfer_pending_icon)
            } else {
                AsyncNet.getBlocksByHash(
                    AccountCenter.currentViteAddress(),
                    accountBlock.receiveBlockHash!!,
                    1
                ).subscribe({ resp ->
                    if (resp.success()) {
                        val list = resp.resp ?: emptyList()
                        if (list.isNotEmpty()) {
                            var dataString = list[0].data?.fromBase64ToBytesOrNull()
                            if (dataString != null
                                && dataString?.size == 33 && dataString?.get(32)?.toInt() == 0x00
                            ) {
                                transferResultTxt.text =
                                    transferResultTxt.context.getString(R.string.contract_call_success)
                                transferResultImg.setImageResource(R.mipmap.transfer_success_icon)
                            } else {
                                transferResultTxt.text =
                                    transferResultTxt.context.getString(R.string.contract_call_failed)
                                transferResultImg.setImageResource(R.mipmap.transfer_failed_icon)
                            }
                        }
                    } else {
                        logi("ERROR:viteTxDetailActivity getBlocksByHash not success")
                    }
                }, {
                    loge(it)
                })
            }
        } else {//transfer, no failed
            transferResultTxt.text =
                transferResultTxt.context.getString(R.string.transfer_token_success)
            transferResultImg.setImageResource(R.mipmap.transfer_success_icon)
        }

        toAddress.text = accountBlock.toAddress
        fromAddress.text = accountBlock.fromAddress
        amount.text = accountBlock.getAmountReadableText(4)
        tranferType.text =
            "${
                if (BlockTypeSendCall == blockDetailType) amount.context.getString(R.string.request) else amount.context.getString(
                    R.string.response
                )
            }"

        hash.text = accountBlock.hash ?: "--"
        block.text = accountBlock.firstSnapshotHeight ?: "--"

        if (BlockTypeSendCall == blockDetailType) { // send / call
            if (accountBlock.data == null || accountBlock.data == "") {
                momoTxt.visibility = View.GONE
            } else {
                momoTxt.visibility = View.VISIBLE
                momo.text =
                    accountBlock.data?.fromBase64ToBytesOrNull()?.let {
                        if (it.isUTF8()) {
                            it.toString(Charsets.UTF_8)
                        } else {
                            it.toHex()
                        }
                    }
            }
        } else { // receive
            accountBlock.fromBlockHash?.let { fromBlockHash ->
                AsyncNet.getBlocksByHash(AccountCenter.currentViteAddress(), fromBlockHash, 1)
                    .subscribe({ resp ->
                        if (resp.success()) {
                            momoTxt.visibility = View.VISIBLE
                            momo.text =
                                resp?.resp?.get(0)?.data?.fromBase64ToBytesOrNull()?.let {
                                    if (it.isUTF8()) {
                                        it.toString(Charsets.UTF_8)
                                    } else {
                                        it.toHex()
                                    }
                                }
                        } else {
                            logi("ERROR:viteTxDetailActivity getBlocksByHash not success")
                        }
                    }, {
                        loge(it)
                    })
            }
        }

        tokenName1.text = accountBlock.viteChainTokenInfo?.symbol ?: "VITE"

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
            startActivity(createBrowserIntent(url))
        }
    }

    fun copyToBoard(targetView: TextView) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.let { cm ->
            cm.setPrimaryClip(ClipData.newPlainText(null, targetView.text))
            Toast.makeText(this, this.getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }
    }

}
