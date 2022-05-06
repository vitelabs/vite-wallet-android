package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import android.graphics.Color
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.ViteConfig
import net.vite.wallet.abi.datatypes.Bool
import net.vite.wallet.abi.datatypes.TokenId
import net.vite.wallet.abi.datatypes.Uint
import net.vite.wallet.abi.datatypes.Utf8String
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
import java.util.*
import java.util.concurrent.CountDownLatch

class DexPost : Decoder {
    companion object {
        val desc = WcDesc(
            function = WcNameItem(
                name = WcLangItem(
                    base = "Dex Post",
                    zh = "交易所挂单"
                )
            ),
            inputs = listOf(
                WcNameItem(
                    WcLangItem(base = "Order Type", zh = "订单类型"),
                    style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                ),
                WcNameItem(WcLangItem(base = "Market", zh = "市场")),
                WcNameItem(WcLangItem(base = "Price", zh = "价格")),
                WcNameItem(WcLangItem(base = "Amount", zh = "数量"))
            )
        )

        var remoteSupportList: ArrayList<Pair<String, String>>? = null

        private val supportList = listOf(
            "tti_5649544520544f4b454e6e40" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_22a818227bb47f072f92f428" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_e80bcafb642ce4898857eccc" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_687d8a93915393b219212c73" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_18823e6e0b95b7d77b3a1b3a" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_60e20567a20282bfd25ab56c" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_661d467c3f4d9c6d7b9e9dc9" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_289ee0569c7d3d75eac1b100" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_26472d9be08f8f2fdeb3030d" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_5649544520544f4b454e6e40" to "tti_687d8a93915393b219212c73",
            "tti_0570e763918b4355074661ac" to "tti_687d8a93915393b219212c73",
            "tti_289ee0569c7d3d75eac1b100" to "tti_687d8a93915393b219212c73",
            "tti_e80bcafb642ce4898857eccc" to "tti_5649544520544f4b454e6e40",
            "tti_661d467c3f4d9c6d7b9e9dc9" to "tti_5649544520544f4b454e6e40",
            "tti_0570e763918b4355074661ac" to "tti_5649544520544f4b454e6e40",
            "tti_289ee0569c7d3d75eac1b100" to "tti_5649544520544f4b454e6e40",
            "tti_26472d9be08f8f2fdeb3030d" to "tti_5649544520544f4b454e6e40",
            "tti_22a818227bb47f072f92f428" to "tti_5649544520544f4b454e6e40",
            "tti_60e20567a20282bfd25ab56c" to "tti_5649544520544f4b454e6e40",
            "tti_687d8a93915393b219212c73" to "tti_80f3751485e4e83456059473",
            "tti_b90c9baffffc9dae58d1f33f" to "tti_80f3751485e4e83456059473",
            "tti_5649544520544f4b454e6e40" to "tti_80f3751485e4e83456059473",
            "tti_18823e6e0b95b7d77b3a1b3a" to "tti_80f3751485e4e83456059473",
            "tti_25e5f191cbb00a88a6267e0f" to "tti_80f3751485e4e83456059473",
            "tti_564954455820434f494e69b5" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_f7e187a151e9c74b81e87cce" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_9b8b779b0ca5b55464e1cfda" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_4a04203e28bb8f3dc3d8f9b1" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_3d3bd4c43620ad1a3bcc630a" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_c1968fde12be9bc60e56a055" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_dfa872e9f0fccb1cc08502dd" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_2f875c97d3a51b66a59c4411" to "tti_b90c9baffffc9dae58d1f33f",
            "tti_22a818227bb47f072f92f428" to "tti_687d8a93915393b219212c73",
            "tti_dfa872e9f0fccb1cc08502dd" to "tti_687d8a93915393b219212c73",
            "tti_564954455820434f494e69b5" to "tti_687d8a93915393b219212c73",
            "tti_c1968fde12be9bc60e56a055" to "tti_687d8a93915393b219212c73",
            "tti_564954455820434f494e69b5" to "tti_5649544520544f4b454e6e40",
            "tti_564954455820434f494e69b5" to "tti_80f3751485e4e83456059473",
            "tti_c8c9ad17bc7b45e38eb88a44" to "tti_80f3751485e4e83456059473",
            "tti_22a818227bb47f072f92f428" to "tti_80f3751485e4e83456059473",
            "tti_c1968fde12be9bc60e56a055" to "tti_80f3751485e4e83456059473",
            "tti_3d3bd4c43620ad1a3bcc630a" to "tti_80f3751485e4e83456059473"
        )

        fun isSupportPair(wcConfirmInfo: WCConfirmInfo): Boolean {
            if (wcConfirmInfo.title != desc.title()) {
                return false
            }
            val tp =
                kotlin.runCatching { wcConfirmInfo.extraData as? Pair<NormalTokenInfo, NormalTokenInfo> }.getOrNull()
                    ?: return false

            val found = (remoteSupportList ?: supportList).find {
                it.first == tp.first.tokenAddress && it.second == tp.second.tokenAddress
            }

            return found != null || "tti_564954455820434f494e69b5" == tp.first.tokenAddress
        }
    }

    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            if (block.amount.toBigInteger() != BigInteger.ZERO) {
                throw ParseException.amountError("block.amount in DexPost must zero but now value is ${block.amount}")
            }
            val tradeTokenIdValue = if (abiDataParseResult[0].value is TokenId) {
                abiDataParseResult[0].value.value.toString()
            } else {
                throw ParseException.dataError("DexPost data 0 is not TokenId")
            }
            val quoteTokenIdValue = if (abiDataParseResult[1].value is TokenId) {
                abiDataParseResult[1].value.value.toString()
            } else {
                throw ParseException.dataError("DexPost data 1 is not TokenId")
            }

            val sideValue = if (abiDataParseResult[2].value is Bool) {
                abiDataParseResult[2].value.value as Boolean
            } else {
                throw ParseException.dataError("DexPost data 2 is not Bool")
            }

            val orderTypeValue = if (abiDataParseResult[3].value is Uint) {
                abiDataParseResult[3].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexPost data 3 is not Uint")
            }

            val priceValue = if (abiDataParseResult[4].value is Utf8String) {
                abiDataParseResult[4].value.value.toString()
            } else {
                throw ParseException.dataError("DexPost data 4 is not Utf8String")
            }

            val quantityValue = if (abiDataParseResult[5].value is Uint) {
                abiDataParseResult[5].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexPost data 5 is not Uint")
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

                    val orderType = if (sideValue) {
                        WCConfirmItemInfo(
                            title = desc.inputs[0].name?.getTitle() ?: "",
                            text = "${ViteConfig.get().context.getString(R.string.dex_sell)} ${tradeTokenInfo.uniqueName()}",
                            textColor = Color.parseColor("#FF0008")
                        )
                    } else {
                        WCConfirmItemInfo(
                            title = desc.inputs[0].name?.getTitle() ?: "",
                            text = "${ViteConfig.get().context.getString(R.string.dex_buy)} ${tradeTokenInfo.uniqueName()}",
                            textColor = Color.parseColor("#5BC500")
                        )
                    }
                    val market = "${tradeTokenInfo.uniqueName()}/${quoteTokenInfo.uniqueName()}"
                    val price = "${priceValue} ${quoteTokenInfo.symbol}"
                    val quantity = tradeTokenInfo.amountTextWithSymbol(quantityValue, 8)
                    result = WCConfirmInfo(
                        title = desc.title(),
                        listItems = listOf(
                            orderType,
                            WCConfirmItemInfo.create(desc.inputs[1], market),
                            WCConfirmItemInfo.create(desc.inputs[2], price),
                            WCConfirmItemInfo.create(desc.inputs[3], quantity)
                        ),
                        extraData = tradeTokenInfo to quoteTokenInfo
                    ) to null
                    countDownLatch.countDown()
                }, {
                    result =
                        null to ParseException.dataError("DexPost cannot find tokenid ${it.message}")
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