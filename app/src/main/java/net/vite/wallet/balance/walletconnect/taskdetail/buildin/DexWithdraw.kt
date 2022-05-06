package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import io.reactivex.schedulers.Schedulers
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.abi.datatypes.TokenId
import net.vite.wallet.abi.datatypes.Uint
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcDesc
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.constants.WcStyle
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import org.walletconnect.Session
import java.math.BigInteger
import java.util.concurrent.CountDownLatch

class DexWithdraw : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Dex Withdraw",
                    zh = "交易所提现"
                )
            ),
            inputs = listOf(
                WcNameItem(
                    WcLangItem(base = "Amount", zh = "提现金额"),
                    style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                )
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
                throw ParseException.amountError("block.amount in DexWithdraw must zero but now value is ${block.amount}")
            }

            val tokenIdValue = if (abiDataParseResult[0].value is TokenId) {
                abiDataParseResult[0].value.value.toString()
            } else {
                throw ParseException.dataError("DexWithdraw data 0 is not TokenId")
            }

            val amountInMini = if (abiDataParseResult[1].value is Uint) {
                abiDataParseResult[1].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexWithdraw data 1 is not Uint")
            }
            var result: Pair<WCConfirmInfo?, Throwable?>? = null
            val countDownLatch = CountDownLatch(1)

            TokenInfoCenter.queryViteToken(tokenIdValue)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    result = WCConfirmInfo(
                        title = desc.title(),
                        listItems = listOf(
                            WCConfirmItemInfo.create(desc.inputs[0], it.amountTextWithSymbol(amountInMini, 8))
                        )
                    ) to null
                    countDownLatch.countDown()

                }, {
                    result = null to ParseException.dataError(
                        "DexWithdraw cannot find " +
                                "tokenid($tokenIdValue)`s tokeninfo ${it.message}"
                    )
                    countDownLatch.countDown()
                })
            countDownLatch.await()
            result?.second?.let {
                throw it
            }
            result?.first!!
        }
    }
}