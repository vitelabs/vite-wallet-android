package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcDesc
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.constants.WcStyle
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import org.walletconnect.Session

class DexDeposit : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Dex Deposit",
                    zh = "交易所充值"
                )
            ),
            inputs = listOf(
                WcNameItem(
                    WcLangItem(base = "Amount", zh = "充值金额"),
                    style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                )
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
            val amount = normalTokenInfo!!.amountTextWithSymbol(block.amount.toBigInteger(), 8)
            WCConfirmInfo(
                title = desc.title(),
                listItems = listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], amount)
                )
            )
        }
    }
}