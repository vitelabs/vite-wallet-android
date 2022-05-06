package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.abi.datatypes.Uint
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.constants.WcStyle
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import org.walletconnect.Session
import java.math.BigInteger

class DexBecomeVIP : Decoder {

    companion object {
        val becomeVip = WcLangItem(
            base = "Become a VIP",
            zh = "开通VIP"
        )
        val cancelVip = WcLangItem(
            base = "Cancel VIP",
            zh = "撤销VIP"
        )

        fun isThisType(wcConfirmInfo: WCConfirmInfo): Boolean {
            return wcConfirmInfo.title == becomeVip.getTitle() || wcConfirmInfo.title == cancelVip.getTitle()
        }
    }

    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            if (block.amount.toBigInteger() != BigInteger.ZERO) {
                throw ParseException.amountError("block.amount in DexBecomeVIP must zero but now value is ${block.amount}")
            }
            val actionType = if (abiDataParseResult[0].value is Uint) {
                abiDataParseResult[0].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexBecomeVIP data 0 is not Uint")
            }.toInt()


            if (extend?.type != Session.MethodCall.SendTransactionExtension.DexFundPledgeForVip || extend?.amount == null) {
                throw ParseException.extendError("DexBecomeVIP extend error")
            }

            val amountStr = extend.amount ?: ""


            val title = when (actionType) {
                1 -> becomeVip
                2 -> cancelVip
                else -> {
                    throw ParseException.dataError("DexBecomeVIP actionType $actionType")
                }
            }.getTitle()

            WCConfirmInfo(
                title = title,
                listItems = listOf(
                    WCConfirmItemInfo.create(
                        WcNameItem(WcLangItem(base = "Current Address", zh = "当前地址")),
                        AccountCenter.currentViteAddress()!!
                    ),
                    WCConfirmItemInfo.create(
                        WcNameItem(
                            WcLangItem(base = "Amount", zh = "抵押金额"),
                            style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                        ), amountStr
                    )
                )
            )
        }
    }
}