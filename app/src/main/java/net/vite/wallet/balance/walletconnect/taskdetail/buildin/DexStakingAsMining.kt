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
import net.vite.wallet.network.rpc.ViteTokenInfo
import net.vite.wallet.network.toLocalReadableText
import org.walletconnect.Session
import java.math.BigDecimal
import java.math.BigInteger

class DexStakingAsMining : Decoder {

    companion object {
        val stakingAsMining = WcLangItem(
            base = "Staking as Mining",
            zh = "抵押挖矿"
        )

        val cancelStakingAsMining = WcLangItem(
            base = "Cancel Staking as Mining",
            zh = "撤销抵押挖矿"
        )

        fun isThisType(wcConfirmInfo: WCConfirmInfo): Boolean {
            return wcConfirmInfo.title == stakingAsMining.getTitle() || wcConfirmInfo.title == cancelStakingAsMining.getTitle()
        }
    }

    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            if (block.amount.toBigInteger() != BigInteger.ZERO) {
                throw ParseException.amountError("block.amount in DexStakingAsMining must zero but now value is ${block.amount}")
            }
            val actionType = if (abiDataParseResult[0].value is Uint) {
                abiDataParseResult[0].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexStakingAsMining data 0 is not Uint")
            }.toInt()

            val amount = if (abiDataParseResult[1].value is Uint) {
                abiDataParseResult[1].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexStakingAsMining data 1 is not Uint")
            }


            val amountStr =
                amount.toBigDecimal()
                    .divide(BigDecimal.TEN.pow(ViteTokenInfo.decimals!!))
                    .toLocalReadableText(8) + " VITE"


            val title = when (actionType) {
                1 -> stakingAsMining
                2 -> cancelStakingAsMining
                else -> {
                    throw ParseException.dataError("DexStakingAsMining actionType $actionType")
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
                            WcLangItem(base = "Amount", zh = "抵押金额"),
                            style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                        ), amountStr
                    )
                )
            )
        }
    }
}