package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import android.graphics.Color
import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.abi.datatypes.DynamicBytes
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcDesc
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.constants.WcStyle
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.utils.toHex
import org.walletconnect.Session
import java.math.BigInteger

class DexCancel : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Dex Cancel",
                    zh = "交易所撤单"
                )
            ),
            inputs = listOf(
                WcNameItem(WcLangItem(base = "Order ID", zh = "订单 ID")),
                WcNameItem(
                    WcLangItem(base = "Order Type", zh = "订单类型"),
                    style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                ),
                WcNameItem(WcLangItem(base = "Market", zh = "市场")),
                WcNameItem(WcLangItem(base = "Price", zh = "价格"))
            )
        )

        fun isThisType(wcConfirmInfo: WCConfirmInfo): Boolean {
            return wcConfirmInfo.title == desc.title()
        }
    }

    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            if (block.amount.toBigInteger() != BigInteger.ZERO) {
                throw ParseException.amountError("block.amount in DexCancel must zero but now value is ${block.amount}")
            }
            if (extend == null || extend.type != Session.MethodCall.SendTransactionExtension.DexCancel
                || extend.tradeTokenSymbol == null
                || extend.side == null
                || extend.quoteTokenSymbol == null
                || extend.price == null
            ) {
                throw ParseException.extendError()
            }

            val orderIdValue = if (abiDataParseResult[0].value is DynamicBytes) {
                abiDataParseResult[0].value.value as ByteArray
            } else {
                throw ParseException.dataError("DexCancel data 0 is not DynamicBytes")
            }
            val rawId = orderIdValue.toHex()
            if (rawId.length != 44) {
                throw ParseException.dataError("DexCancel rawId.length ${rawId.length}")
            }
            val id = "${rawId.substring(0, 8)}...${rawId.substring(rawId.length - 8)}"

            val orderType = if (extend!!.side == 1) {
                WCConfirmItemInfo(
                    title = desc.inputs[1].name?.getTitle() ?: "",
                    text = "${ViteConfig.get().context.getString(R.string.dex_sell)} ${extend!!.tradeTokenSymbol!!}",
                    textColor = Color.parseColor("#FF0008")
                )
            } else if (extend!!.side == 0) {
                WCConfirmItemInfo(
                    title = desc.inputs[1].name?.getTitle() ?: "",
                    text = "${ViteConfig.get().context.getString(R.string.dex_buy)} ${extend!!.tradeTokenSymbol}",
                    textColor = Color.parseColor("#5BC500")
                )
            } else {
                throw ParseException.dataError("DexCancel orderType")
            }
            val market = "${extend!!.tradeTokenSymbol}/${extend!!.quoteTokenSymbol}"

            WCConfirmInfo(
                desc.title(),
                listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], id),
                    orderType,
                    WCConfirmItemInfo.create(desc.inputs[2], market),
                    WCConfirmItemInfo.create(desc.inputs[3], extend!!.price!!)
                )
            )
        }
    }

}