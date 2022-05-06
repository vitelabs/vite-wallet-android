package net.vite.wallet.balance.walletconnect.taskdetail.buildin

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

class VoteRevoke : Decoder {

    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Revoke Vote",
                    zh = "撤销投票"
                )
            ),
            inputs = listOf(
                WcNameItem(WcLangItem(base = "Votes Revoked", zh = "撤销投票量"))
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
                throw ParseException.amountError("block.amount in CancelVote must zero but now value is ${block.amount}")
            }

            val balance = normalTokenInfo!!.balance()

            WCConfirmInfo(
                title = desc.title(),
                listItems = listOf(
                    WCConfirmItemInfo.create(
                        desc.inputs[0],
                        normalTokenInfo.amountTextWithSymbol(balance, 8)
                    )
                )
            )
        }
    }
}