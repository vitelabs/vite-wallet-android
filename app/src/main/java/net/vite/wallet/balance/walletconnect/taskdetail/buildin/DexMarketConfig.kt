package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.abi.datatypes.*
import net.vite.wallet.abi.datatypes.Int
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
import net.vite.wallet.network.toLocalReadableText
import org.walletconnect.Session
import java.math.BigInteger
import java.util.concurrent.CountDownLatch

class DexMarketConfig : Decoder {
    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            if (block.amount.toBigInteger() != BigInteger.ZERO) {
                throw ParseException.amountError("block.amount in DexMarketConfig must zero but now value is ${block.amount}")
            }

            val operationCodeValue = if (abiDataParseResult[0].value is Uint) {
                abiDataParseResult[0].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexMarketConfig data 0 is not Uint")
            }.toInt()

            val tradeTokenIdValue = if (abiDataParseResult[1].value is TokenId) {
                abiDataParseResult[1].value.value.toString()
            } else {
                throw ParseException.dataError("DexMarketConfig data 1 is not TokenId")
            }

            val quoteTokenIdValue = if (abiDataParseResult[2].value is TokenId) {
                abiDataParseResult[2].value.value.toString()
            } else {
                throw ParseException.dataError("DexMarketConfig data 2 is not TokenId")
            }

            val ownerAddressValue = if (abiDataParseResult[3].value is Address) {
                abiDataParseResult[3].value.value.toString()
            } else {
                throw ParseException.dataError("DexMarketConfig data 3 is not Address")
            }

            val takerFeeRateValue = if (abiDataParseResult[4].value is Int) {
                abiDataParseResult[4].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexMarketConfig data 4 is not Int")
            }

            val makerFeeRateValue = if (abiDataParseResult[5].value is Int) {
                abiDataParseResult[5].value.value as BigInteger
            } else {
                throw ParseException.dataError("DexMarketConfig data 5 is not Int")
            }


            val stopMarketValue = if (abiDataParseResult[6].value is Bool) {
                abiDataParseResult[6].value.value as Boolean
            } else {
                throw ParseException.dataError("DexMarketConfig data 6 is not Int")
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


                    if (operationCodeValue == 1) {
                        result = transferPairConfirmInfo(market, ownerAddressValue) to null
                    } else if (operationCodeValue == 2 || operationCodeValue == 4 || operationCodeValue == 6) {
                        result = adjustFeeConfirmInfo(
                            market,
                            AccountCenter.currentViteAddress()!!,
                            operationCodeValue,
                            takerFeeRateValue,
                            makerFeeRateValue
                        ) to null
                    } else if (operationCodeValue == 8) {
                        result = stopMarketConfirmInfo(
                            market, AccountCenter.currentViteAddress()!!, stopMarketValue
                        ) to null
                    } else {
                        result = null to ParseException.error("unknown operationCodeValue $operationCodeValue")
                    }
                    countDownLatch.countDown()
                }, {
                    result = null to ParseException.dataError("DexMarketConfig cannot find tokenid ${it.message}")
                    countDownLatch.countDown()
                })

            countDownLatch.await()
            result?.second?.let {
                throw it
            }
            result?.first!!
        }
    }

    private val dexTransferPair = WcDesc(
        function = WcNameItem(
            name = WcLangItem(
                base = "Transfer Trading pair's Ownership",
                zh = "转移交易对权限"
            )
        ),
        inputs = listOf(
            WcNameItem(WcLangItem(base = "Name", zh = "交易对名称")),
            WcNameItem(WcLangItem(base = "Recipient Address", zh = "接受权限地址"))
        )
    )

    private fun transferPairConfirmInfo(market: String, ownerAddress: String): WCConfirmInfo {
        return WCConfirmInfo(
            dexTransferPair.title(),
            listOf(
                WCConfirmItemInfo.create(dexTransferPair.inputs[0], market),
                WCConfirmItemInfo.create(dexTransferPair.inputs[1], ownerAddress)
            )
        )

    }

    private fun adjustFeeConfirmInfo(
        market: String, currentAddress: String, code: kotlin.Int,
        takerFeeRate: BigInteger, makerFeeRate: BigInteger
    ): WCConfirmInfo {

        val descItems = mutableListOf(
            WCConfirmItemInfo.create(WcNameItem(WcLangItem(base = "Name", zh = "交易对名称")), market),
            WCConfirmItemInfo.create(WcNameItem(WcLangItem(base = "Current Address", zh = "当前地址")), currentAddress)
        )

        val takerFeeRateStr =
            takerFeeRate.toBigDecimal().divide(1000.toBigDecimal()).toLocalReadableText(3)
        val makerFeeRateStr =
            makerFeeRate.toBigDecimal().divide(1000.toBigDecimal()).toLocalReadableText(3)

        if (code and 2 == 2) {
            descItems.add(
                WCConfirmItemInfo.create(
                    WcNameItem(
                        WcLangItem(base = "Adjusted Taker Fees", zh = "调整后Taker费率"),
                        style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                    ), takerFeeRateStr
                )
            )
        }
        if (code and 4 == 4) {
            descItems.add(
                WCConfirmItemInfo.create(
                    WcNameItem(
                        WcLangItem(base = "Adjusted Maker Fees", zh = "调整后Maker费率"),
                        style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                    ), makerFeeRateStr
                )
            )
        }

        return WCConfirmInfo(
            title = WcLangItem(
                base = "Adjust Fees",
                zh = "调整费率"
            ).getTitle(),
            listItems = descItems
        )
    }


    private fun stopMarketConfirmInfo(market: String, currentAddress: String, isStop: Boolean): WCConfirmInfo {
        val descItems = listOf(
            WCConfirmItemInfo.create(WcNameItem(WcLangItem(base = "Name", zh = "交易对名称")), market),
            WCConfirmItemInfo.create(WcNameItem(WcLangItem(base = "Current Address", zh = "当前地址")), currentAddress)
        )

        val title = if (isStop) {
            WcNameItem(
                name = WcLangItem(
                    base = "Suspend Trading Pair",
                    zh = "暂停交易对"
                )
            )
        } else {
            WcNameItem(
                name = WcLangItem(
                    base = "Recover Trading Pair",
                    zh = "恢复交易对"
                )
            )
        }.name!!.getTitle()

        return WCConfirmInfo(
            title = title,
            listItems = descItems
        )
    }

}