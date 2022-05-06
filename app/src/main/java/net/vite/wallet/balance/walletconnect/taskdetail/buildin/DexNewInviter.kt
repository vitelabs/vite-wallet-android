package net.vite.wallet.balance.walletconnect.taskdetail.buildin

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

class DexNewInviter : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Create Referral Code",
                    zh = "生成邀请码"
                )
            ),
            inputs = listOf(
                WcNameItem(WcLangItem(base = "Current Address", zh = "当前地址")),
                WcNameItem(WcLangItem(base = "Cost", zh = "扣款金额"))
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
                throw ParseException.amountError("block.amount in DexNewInviter must zero but now value is ${block.amount}")
            }
            if (extend == null || extend.type != Session.MethodCall.SendTransactionExtension.DexNewInviter
                || extend.cost == null
            ) {
                throw ParseException.extendError("NewInviter extend something is empty or type error")
            }


            WCConfirmInfo(
                desc.title(),
                listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], AccountCenter.currentViteAddress()!!),
                    WCConfirmItemInfo.create(desc.inputs[1], extend!!.cost!!)
                )
            )
        }
    }
}