package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import io.reactivex.schedulers.Schedulers
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.abi.datatypes.TokenId
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
import java.util.concurrent.CountDownLatch

class MintageChangeToNonIssuable: Decoder {
    private val desc = WcDesc(
        function = WcNameItem(
            name = WcLangItem(
                base = "Change to Non-issuable",
                zh = "改为不可增发"
            )
        ),
        inputs = listOf(
            WcNameItem(WcLangItem(base = "Token Name", zh = "代币全称")),
            WcNameItem(WcLangItem(base = "Token Symbol", zh = "代币简称"))
        )
    )

    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            if (block.amount.toBigInteger() != BigInteger.ZERO) {
                throw ParseException.amountError("block.amount in MintageChangeToNonIssuable must zero but now value is ${block.amount}")
            }

            val tokenIdValue = if (abiDataParseResult[0].value is TokenId) {
                abiDataParseResult[0].value.value.toString()
            } else {
                throw ParseException.dataError("MintageChangeToNonIssuable data 0 is not TokenId")
            }


            var result: Pair<WCConfirmInfo?, Throwable?>? = null
            val countDownLatch = CountDownLatch(1)

            TokenInfoCenter.queryViteToken(tokenIdValue).subscribeOn(Schedulers.io())
                .subscribe({
                    result = WCConfirmInfo(
                        title = desc.title(),
                        listItems = listOf(
                            WCConfirmItemInfo.create(desc.inputs[0], it.name ?: ""),
                            WCConfirmItemInfo.create(desc.inputs[1], it.uniqueName())
                        )
                    ) to null

                    countDownLatch.countDown()
                }, {
                    result =
                        null to ParseException.dataError("MintageChangeToNonIssuable cannot find tokenid ${it.message}")
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