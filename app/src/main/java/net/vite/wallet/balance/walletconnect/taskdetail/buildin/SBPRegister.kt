package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.abi.datatypes.Address
import net.vite.wallet.abi.datatypes.Utf8String
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcDesc
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.constants.WcStyle
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import org.walletconnect.Session

class SBPRegister : Decoder {
    private val desc = WcDesc(
        function = WcNameItem(
            name = WcLangItem(
                base = "Register SBP",
                zh = "注册 SBP"
            )
        ),
        inputs = listOf(
            WcNameItem(WcLangItem(base = "SBP Name", zh = "SBP 名称")),
            WcNameItem(
                WcLangItem(base = "Amount", zh = "抵押金额"),
                style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
            )
        )
    )

    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            val nameValue = if (abiDataParseResult[0].value is Utf8String) {
                abiDataParseResult[0].value.value.toString()
            } else {
                throw ParseException.dataError("Register data 0 is not String")
            }

            val blockProducingAddress = if (abiDataParseResult[1].value is Address) {
                abiDataParseResult[1].value.value.toString()
            } else {
                throw ParseException.dataError("Register data 1 is not Address")
            }

            val rewardWithdrawAddress = if (abiDataParseResult[2].value is Address) {
                abiDataParseResult[2].value.value.toString()
            } else {
                throw ParseException.dataError("Register data 2 is not Address")
            }

            val amount = normalTokenInfo!!.amountTextWithSymbol(block.amount.toBigInteger(), 8)
            WCConfirmInfo(
                title = desc.title(),
                listItems = listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], nameValue),
                    WCConfirmItemInfo.create(desc.inputs[1], amount)
                )
            )
        }
    }

}