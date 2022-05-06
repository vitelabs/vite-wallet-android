package net.vite.wallet.balance.txlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.ViteTxDetailActivity
import net.vite.wallet.constants.*
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.rpc.AccountBlock
import java.text.SimpleDateFormat

val BlockDetailTypeToText = mapOf(
    BlockDetailTypeSend to R.string.transfer_token,
    BlockDetailTypeReceive to R.string.transfer_token,
    BlockDetailTypeRegister to R.string.tx_type_register,
    BlockDetailTypeCancelRegister to R.string.tx_type_cancel_register,
    BlockDetailTypeRegisterUpdate to R.string.tx_type_register_update,
    BlockDetailTypeExtractReward to R.string.tx_type_extract_reward,
    BlockDetailTypeVote to R.string.tx_type_vote,
    BlockDetailTypeCancelVote to R.string.tx_type_cancel_vote,
    BlockDetailTypePledge to R.string.tx_type_pledge_quota,
    BlockDetailTypeCancelPledge to R.string.tx_type_cancel_pledge,
    BlockDetailTypeCancelPledgeNew to R.string.tx_type_cancel_pledge,
    BlockDetailTypeMintage to R.string.tx_type_mintage,
    BlockDetailTypeCancelMintage to R.string.tx_type_cancel_mintage,
    BlockDetailTypeContractCall to R.string.contract
)

val BlockDetailTypeToIcon = mapOf(
    BlockDetailTypeSend to R.mipmap.tx_icon_transfer,
    BlockDetailTypeReceive to R.mipmap.tx_icon_receive,
    BlockDetailTypeRegister to R.mipmap.tx_register_sbp,
    BlockDetailTypeCancelRegister to R.mipmap.tx_register_sbp,
    BlockDetailTypeRegisterUpdate to R.mipmap.tx_register_sbp,
    BlockDetailTypeExtractReward to R.mipmap.tx_get_reward,
    BlockDetailTypeVote to R.mipmap.tx_vote,
    BlockDetailTypeCancelVote to R.mipmap.tx_vote,
    BlockDetailTypePledge to R.mipmap.tx_get_quota,
    BlockDetailTypeCancelPledge to R.mipmap.tx_get_quota,
    BlockDetailTypeCancelPledgeNew to R.mipmap.tx_get_quota,
    BlockDetailTypeMintage to R.mipmap.tx_mintage,
    BlockDetailTypeCancelMintage to R.mipmap.tx_mintage,
    BlockDetailTypeContractCall to R.mipmap.tx_icon_contract
)


class TxItemVH(val view: View) : RecyclerView.ViewHolder(view) {


    companion object {
        val dataFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        fun create(parent: ViewGroup): TxItemVH {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_tx_list, parent, false)
            return TxItemVH(view)
        }
    }

    private val typeIcon = view.findViewById<ImageView>(R.id.txTypeIcon)
    private val txTimestamp = view.findViewById<TextView>(R.id.txTimestamp)
    private val txAddr = view.findViewById<TextView>(R.id.txAddr)
    private val txAmount = view.findViewById<TextView>(R.id.txAmount)
    private val txTokenSymbol = view.findViewById<TextView>(R.id.txTokenSymbol)
    private val txTypeTxt = view.findViewById<TextView>(R.id.txTypeTxt)
    private val creationRecord = view.findViewById<TextView>(R.id.creationRecord)

    fun bind(block: AccountBlock?) {
        block?.let { block ->

            val url = if (block.blockType == AccountBlock.BlockTypeGenenis) {
                creationRecord.visibility = View.VISIBLE
                typeIcon.visibility = View.GONE
                txTimestamp.visibility = View.GONE
                txAddr.visibility = View.GONE
                txAmount.visibility = View.GONE
                txTokenSymbol.visibility = View.GONE
                txTypeTxt.visibility = View.GONE
                NetConfigHolder.netConfig.viteGenesisUrlPrefix + AccountCenter.currentViteAddress()
            } else {
                creationRecord.visibility = View.GONE
                typeIcon.visibility = View.VISIBLE
                txTimestamp.visibility = View.VISIBLE
                txAddr.visibility = View.VISIBLE
                txAmount.visibility = View.VISIBLE
                txTokenSymbol.visibility = View.VISIBLE
                txTypeTxt.visibility = View.VISIBLE

                typeIcon.setImageResource(
                    BlockDetailTypeToIcon[block.blockDetailType()] ?: R.mipmap.tx_icon_contract
                )
                txTypeTxt.setText(
                    BlockDetailTypeToText[block.blockDetailType()] ?: R.string.contract
                )

                txTimestamp.text = block.getMsTimestamp()?.let {
                    if (it == 0L) {
                        ""
                    } else {
                        dataFormat.format(it)
                    }
                } ?: ""
                txTokenSymbol.text = block.viteChainTokenInfo?.symbol ?: ""
                if (block.getAmountReadableText(4) == "0") {
                    txAmount.text = "0"
                } else {
                    txAmount.text = if (block.isSendBlock()!!) {
                        "-"
                    } else {
                        "+"
                    } + block.getAmountReadableText(4)
                }

                if (block.isSendBlock()!!) {
                    txAmount.setTextColor(Color.parseColor("#ffff0008"))
                } else {
                    txAmount.setTextColor(Color.parseColor("#ff5bc500"))
                }

                if (block.isSendBlock()!!) {
                    txAddr.text = block.toAddress ?: ""
                } else {
                    txAddr.text = block.fromAddress ?: ""
                }

                "${NetConfigHolder.netConfig.explorerUrlPrefix}/tx/${block.hash}"
            }

            view.setOnClickListener {
//                H5WebActivity.show(view.context, url)
                ViteTxDetailActivity.show(view.context, block, url)
            }
        }

    }

}