package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.abi.datatypes.TokenId
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcDesc
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.constants.WcStyle
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.rpc.ViteTokenInfo
import org.walletconnect.Session
import java.math.BigInteger
import java.util.concurrent.CountDownLatch

class DexNewMarket : Decoder {

    private val desc = WcDesc(
        function = WcNameItem(
            name = WcLangItem(
                base = "Open Trading Pair",
                zh = "开通交易对"
            )
        ),
        inputs = listOf(
            WcNameItem(WcLangItem(base = "Name", zh = "交易对名称")),
            WcNameItem(WcLangItem(base = "Current Address", zh = "当前地址")),
            WcNameItem(
                WcLangItem(base = "Listing Fees", zh = "上币费"),
                style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
            )
        )
    )

    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            if (block.amount.toBigInteger() != BigInteger.ZERO) {
                throw ParseException.amountError("block.amount in DexNewMarket must zero but now value is ${block.amount}")
            }
            if (block.tokenId != ViteTokenInfo.tokenId) {
                throw ParseException.tokenIdError("block.tokenId in DexNewMarket must be ViteTokenId but now value is ${block.tokenId}")
            }

            if (extend?.type != Session.MethodCall.SendTransactionExtension.DexNewMarket) {
                throw ParseException.extendError("DexNewMarket type error ${extend?.type}")
            }

            if (extend?.fee == null) {
                throw ParseException.extendError("DexNewMarket fee is null")
            }


            val tradeTokenIdValue = if (abiDataParseResult[0].value is TokenId) {
                abiDataParseResult[0].value.value.toString()
            } else {
                throw ParseException.dataError("DexNewMarket data 0 is not TokenId")
            }

            val quoteTokenIdValue = if (abiDataParseResult[1].value is TokenId) {
                abiDataParseResult[1].value.value.toString()
            } else {
                throw ParseException.dataError("DexNewMarket data 1 is not TokenId")
            }

            var result: Pair<WCConfirmInfo?, Throwable?>? = null
            val countDownLatch = CountDownLatch(1)

            Observable.zip(
                TokenInfoCenter.queryViteToken(tradeTokenIdValue),
                TokenInfoCenter.queryViteToken(quoteTokenIdValue),
                BiFunction<NormalTokenInfo, NormalTokenInfo, Pair<NormalTokenInfo, NormalTokenInfo>> { tradeTokenInfo, quoteTokenInfo ->
                    tradeTokenInfo to quoteTokenInfo
                }
            ).subscribeOn(Schedulers.io())
                .subscribe({
                    val tradeTokenInfo = it.first
                    val quoteTokenInfo = it.second
                    val market = "${tradeTokenInfo.uniqueName()}/${quoteTokenInfo.uniqueName()}"

                    result = WCConfirmInfo(
                        title = desc.title(),
                        listItems = listOf(
                            WCConfirmItemInfo.create(desc.inputs[0], market),
                            WCConfirmItemInfo.create(desc.inputs[1], AccountCenter.currentViteAddress()!!),
                            WCConfirmItemInfo.create(desc.inputs[2], extend!!.fee!!)
                        )
                    ) to null

                    countDownLatch.countDown()
                }, {
                    result = null to ParseException.dataError("DexNewMarket cannot find tokenid ${it.message}")
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