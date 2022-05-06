package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.abi.datatypes.generated.Bytes32
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcDesc
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.utils.toHex
import org.walletconnect.Session

class DexCancelStakeById : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Retrieve Staking",
                    zh = "取回抵押"
                )
            ),
            inputs = listOf(
                WcNameItem(WcLangItem(base = "Current Address", zh = "当前地址"))
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

            val id = if (abiDataParseResult[0].value is Bytes32) {
                val array: ByteArray = abiDataParseResult[0].value.value as ByteArray
                array.toHex()
            } else {
                throw ParseException.dataError("dexCancelStakeById data 0 is not Bytes32")
            }

            WCConfirmInfo(
                title = desc.title(),
                listItems = listOf(
                    WCConfirmItemInfo.create(desc.inputs[0], AccountCenter.currentViteAddress()!!)
                )
            )
        }
    }
}