package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.abi.datatypes.Address
import net.vite.wallet.abi.datatypes.Uint
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

class QuotaRegainStakefor : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Regain Stake for Quota",
                    zh = "取回配额抵押"
                )
            ),
            inputs = listOf(
                WcNameItem(WcLangItem(base = "Amount", zh = "取回抵押金额"))
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
                throw ParseException.amountError("block.amount in CancelPledge must zero but now value is ${block.amount}")
            }
            val address = if (abiDataParseResult[0].value is Address) {
                abiDataParseResult[0].value.value.toString()
            } else {
                throw ParseException.dataError("CancelPledge data 0 is not Address")
            }
            val amountInMini = if (abiDataParseResult[1].value is Uint) {
                abiDataParseResult[1].value.value as BigInteger
            } else {
                throw ParseException.dataError("CancelPledge data 1 is not Uint")
            }
            val amountWithSymbol = normalTokenInfo!!.amountTextWithSymbol(amountInMini, 8)

            WCConfirmInfo(
                title = desc.title(),
                listItems = listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], amountWithSymbol)
                )
            )
        }
    }
}