package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.abi.datatypes.Uint
import net.vite.wallet.account.AccountCenter
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

class DexBindInviter : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Use Referral Code",
                    zh = "使用邀请码"
                )
            ),
            inputs = listOf(
                WcNameItem(WcLangItem(base = "Beneficiary Address", zh = "接受邀请地址")),
                WcNameItem(WcLangItem(base = "Referral Code", zh = "邀请码"))
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
                throw ParseException.amountError("block.amount in DexBindInviter must zero but now value is ${block.amount}")
            }

            val codeValue = if (abiDataParseResult[0].value is Uint) {
                abiDataParseResult[0].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexBindInviter data 0 is not Uint")
            }

            WCConfirmInfo(
                desc.title(),
                listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], AccountCenter.currentViteAddress()!!),
                    WCConfirmItemInfo.create(desc.inputs[1], codeValue.toString())
                )
            )
        }
    }
}