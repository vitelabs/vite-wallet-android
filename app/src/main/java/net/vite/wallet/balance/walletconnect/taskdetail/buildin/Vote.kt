package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.abi.datatypes.Utf8String
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcDesc
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import org.walletconnect.Session
import java.math.BigInteger

class Vote : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Vote",
                    zh = "投票"
                )
            ),
            inputs = listOf(
                WcNameItem(WcLangItem(base = "SBP Candidate", zh = "投票节点名称")),
                WcNameItem(WcLangItem(base = "Votes", zh = "投票量"))
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
                throw ParseException.amountError("block.amount in Vote must zero but now value is ${block.amount}")
            }
            val sbpName = if (abiDataParseResult[0].value is Utf8String) {
                abiDataParseResult[0].value.value.toString()
            } else {
                throw ParseException.dataError("Vote data 0 is not String")
            }

            val balance = normalTokenInfo!!.balance()

            WCConfirmInfo(
                title = desc.title(),
                listItems = listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], sbpName),
                    WCConfirmItemInfo.create(
                        desc.inputs[1],
                        normalTokenInfo.amountTextWithSymbol(balance, 8)
                    )
                )
            )
        }
    }
}