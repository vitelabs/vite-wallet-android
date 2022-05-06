package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.abi.datatypes.Address
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcDesc
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import org.walletconnect.Session

class QuotaAcquire : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Get Quota",
                    zh = "获取配额"
                )
            ),
            inputs = listOf(
                WcNameItem(WcLangItem(base = "Amount", zh = "抵押金额")),
                WcNameItem(WcLangItem(base = "Beneficiary Address", zh = "配额受益地址"))
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
            val address = if (abiDataParseResult[0].value is Address) {
                abiDataParseResult[0].value.value.toString()
            } else {
                throw ParseException.dataError("Pledge data 0 is not Address")
            }
            val amount = normalTokenInfo!!.amountTextWithSymbol(block.amount.toBigInteger(), 8)
            WCConfirmInfo(
                title = desc.title(),
                listItems = listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], amount),
                    WCConfirmItemInfo.create(desc.inputs[1], address)
                )
            )
        }
    }
}