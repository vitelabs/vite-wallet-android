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
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import org.walletconnect.Session
import java.math.BigInteger

class SBPExtractReward : Decoder {
    private val desc = WcDesc(
        function = WcNameItem(
            name = WcLangItem(
                base = "Extract Rewards",
                zh = "提取奖励"
            )
        ),
        inputs = listOf(
            WcNameItem(WcLangItem(base = "Receive Address", zh = "收款地址"))
        )
    )

    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            if (block.amount.toBigInteger() != BigInteger.ZERO) {
                throw ParseException.amountError("block.amount in SBPExtractReward must zero but now value is ${block.amount}")
            }
            val sbpName = if (abiDataParseResult[0].value is Utf8String) {
                abiDataParseResult[0].value.value.toString()
            } else {
                throw ParseException.dataError("SBPExtractReward data 0 is not String")
            }
            val receiveAddress = if (abiDataParseResult[1].value is Address) {
                abiDataParseResult[1].value.value.toString()
            } else {
                throw ParseException.dataError("SBPExtractReward data 1 is not Address")
            }
            WCConfirmInfo(
                title = desc.title(),
                listItems = listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], receiveAddress)
                )
            )
        }
    }

}