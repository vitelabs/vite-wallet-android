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
import net.vite.wallet.network.rpc.SyncNet
import net.vite.wallet.utils.toHex
import org.walletconnect.Session
import java.math.BigInteger

class QuotaRegainStakeforNew : Decoder {
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

            val id = if (abiDataParseResult[0].value is Bytes32) {
                val array: ByteArray = abiDataParseResult[0].value.value as ByteArray
                array.toHex()
            } else {
                throw ParseException.dataError("CancelPledge data 0 is not Bytes32")
            }

            var amountInMini = BigInteger.ZERO
            val resp = SyncNet.getBlocksByHash(AccountCenter.currentViteAddress()!!, id, 1)
            if (resp.success()) {
                val list = resp.resp
                    ?: throw ParseException.dataError("CancelPledge can't find this id on chain")

                if (list.isEmpty()) {
                    throw ParseException.dataError("CancelPledge can't find this id on chain")
                } else {
                    amountInMini = list[0].amount?.toBigIntegerOrNull()
                        ?: throw ParseException.dataError("CancelPledge can't find this id on chain")
                }
            } else {
                throw ParseException.dataError("CancelPledge can't find this id on chain")
            }

//            WCConfirmInfo(
//                title = desc.title(),
//                listItems = listOf(
//                    WCConfirmItemInfo.create(desc.inputs[0], id)
//                )
//            )

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