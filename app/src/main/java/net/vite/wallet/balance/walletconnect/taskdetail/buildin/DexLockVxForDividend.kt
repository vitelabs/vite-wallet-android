package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.abi.datatypes.Uint
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.constants.WcStyle
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import org.walletconnect.Session
import java.math.BigInteger

class DexLockVxForDividend : Decoder {

    companion object {
        val lock = WcLangItem(
            base = "Stake",
            zh = "锁仓VX"
        )
        val unlock = WcLangItem(
            base = "Retrieve",
            zh = "取回锁仓VX"
        )

        fun isThisType(wcConfirmInfo: WCConfirmInfo): Boolean {
            return wcConfirmInfo.title == lock.getTitle() || wcConfirmInfo.title == unlock.getTitle()
        }
    }

    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            if (block.amount.toBigInteger() != BigInteger.ZERO) {
                throw ParseException.amountError("block.amount in DexLockVxForDividend must zero but now value is ${block.amount}")
            }
            val actionType = if (abiDataParseResult[0].value is Uint) {
                abiDataParseResult[0].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexLockVxForDividend data 0 is not Uint")
            }.toInt()

            val amount = if (abiDataParseResult[1].value is Uint) {
                abiDataParseResult[1].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexLockVxForDividend data 1 is not Uint")
            }


            val amountStr = normalTokenInfo!!.amountTextWithSymbol(amount, 9).replace("VITE", "VX")

            val title = when (actionType) {
                1 -> lock
                2 -> unlock
                else -> {
                    throw ParseException.dataError("DexLockVxForDividend actionType $actionType")
                }
            }.getTitle()

            WCConfirmInfo(
                title = title,
                listItems = listOf(
                    WCConfirmItemInfo.create(
                        WcNameItem(WcLangItem(base = "Current Address", zh = "当前地址")),
                        AccountCenter.currentViteAddress()!!
                    ),
                    WCConfirmItemInfo.create(
                        WcNameItem(
                            WcLangItem(base = "Amount", zh = "金额"),
                            style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                        ), amountStr
                    )
                )
            )
        }
    }
}