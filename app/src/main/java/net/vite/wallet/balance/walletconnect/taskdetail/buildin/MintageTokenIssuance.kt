package net.vite.wallet.balance.walletconnect.taskdetail.buildin

import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.abi.datatypes.Bool
import net.vite.wallet.abi.datatypes.Uint
import net.vite.wallet.abi.datatypes.Utf8String
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.walletconnect.taskdetail.DataDecodeResult
import net.vite.wallet.balance.walletconnect.taskdetail.ParseException
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmItemInfo
import net.vite.wallet.constants.WcLangItem
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.constants.WcStyle
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.toLocalReadableTextWithThouands
import org.walletconnect.Session
import java.math.BigDecimal
import java.math.BigInteger

class MintageTokenIssuance : Decoder {
    override fun decode(
        rawSendTransaction: Session.MethodCall.SendTransaction,
        abiDataParseResult: List<DataDecodeResult>,
        normalTokenInfo: NormalTokenInfo?
    ): WCConfirmInfo {
        return with(rawSendTransaction) {
            val fee = block.fee?.toBigIntegerOrNull() ?: throw ParseException.feeError("Mintage fee error ${block.fee}")

            val items = ArrayList<WCConfirmItemInfo>()

            items.add(
                WCConfirmItemInfo.create(
                    item = WcNameItem(WcLangItem(base = "Issuance Address", zh = "铸币地址")),
                    text = AccountCenter.currentViteAddress()!!
                )
            )


            val tokenNameValue = if (abiDataParseResult[1].value is Utf8String) {
                abiDataParseResult[1].value.value.toString()
            } else {
                throw ParseException.dataError("Mintage data 1 is not String")
            }
            items.add(
                WCConfirmItemInfo.create(
                    item = WcNameItem(WcLangItem(base = "Token Name", zh = "代币全称")),
                    text = tokenNameValue
                )
            )

            val tokenSymbolValue = if (abiDataParseResult[2].value is Utf8String) {
                abiDataParseResult[2].value.value.toString()
            } else {
                throw ParseException.dataError("Mintage data 2 is not String")
            }
            items.add(
                WCConfirmItemInfo.create(
                    item = WcNameItem(WcLangItem(base = "Token Symbol", zh = "代币简称")),
                    text = tokenSymbolValue
                )
            )

            val decimalsValue = if (abiDataParseResult[4].value is Uint) {
                abiDataParseResult[4].value.value as BigInteger
            } else {
                throw ParseException.dataError("Mintage data 4 is not Uint")
            }

            val totalSupplyValue = if (abiDataParseResult[3].value is Uint) {
                abiDataParseResult[3].value.value as BigInteger
            } else {
                throw ParseException.dataError("Mintage data 3 is not Uint")
            }
            val totalSupplyStr = totalSupplyValue.toBigDecimal()
                .divide(BigDecimal.TEN.pow(decimalsValue.toInt())).toLocalReadableTextWithThouands(0)

            items.add(
                WCConfirmItemInfo.create(
                    item = WcNameItem(WcLangItem(base = "Total Supply", zh = "总发行量")),
                    text = totalSupplyStr
                )
            )

            items.add(
                WCConfirmItemInfo.create(
                    item = WcNameItem(WcLangItem(base = "Decimals", zh = "价格精度")),
                    text = decimalsValue.toString()
                )
            )


            val isReIssuableValue = if (abiDataParseResult[0].value is Bool) {
                abiDataParseResult[0].value.value as Boolean
            } else {
                throw ParseException.dataError("Mintage data 0 is not Bool")
            }
            val canIssue = if (isReIssuableValue) {
                ViteConfig.get().context.getString(R.string.detail_can_issue)
            } else {
                ViteConfig.get().context.getString(R.string.detail_can_not_issue)
            }

            items.add(
                WCConfirmItemInfo.create(
                    item = WcNameItem(WcLangItem(base = "Re-issuable", zh = "是否可增发")),
                    text = canIssue
                )
            )

            if (isReIssuableValue) {
                val maxSupplyValue = if (abiDataParseResult[5].value is Uint) {
                    abiDataParseResult[5].value.value as BigInteger
                } else {
                    throw ParseException.dataError("Mintage data 5 is not Uint")
                }
                val maxSupplyValueStr = maxSupplyValue.toBigDecimal()
                    .divide(BigDecimal.TEN.pow(decimalsValue.toInt())).toLocalReadableTextWithThouands(0)
                items.add(
                    WCConfirmItemInfo.create(
                        item = WcNameItem(WcLangItem(base = "Max Supply", zh = "最大发行量")),
                        text = maxSupplyValueStr
                    )
                )
            }

            val ownerBurnOnlyValue = if (abiDataParseResult[6].value is Bool) {
                abiDataParseResult[6].value.value as Boolean
            } else {
                throw ParseException.dataError("Mintage data 6 is not Bool")
            }


            val feeStr = normalTokenInfo!!.amountTextWithSymbol(fee, 8)
            items.add(
                WCConfirmItemInfo.create(
                    item = WcNameItem(
                        WcLangItem(base = "Issuance Fee", zh = "铸币费"),
                        style = WcStyle(textColor = "5BC500", backgroundColor = "007AFF0F")
                    ),
                    text = feeStr
                )
            )


            WCConfirmInfo(
                title = WcLangItem(
                    base = "Token Issuance",
                    zh = "铸币"
                ).getTitle(),
                listItems = items
            )

        }
    }
}