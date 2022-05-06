package net.vite.wallet.balance.walletconnect.taskdetail

import com.google.gson.Gson
import net.vite.wallet.constants.BuildInTransferDesc
import net.vite.wallet.constants.WcDesc
import net.vite.wallet.constants.WcNameItem
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.utils.toHex
import org.walletconnect.Session
import java.nio.charset.Charset


fun Session.MethodCall.SendTransaction.crossChainTransfer(tokenInfo: NormalTokenInfo): WCConfirmInfo {
    val data = block.data ?: throw ParseException.dataError("CrossChainTransfer date empty")
    if (data.size <= 3) {
        throw ParseException.dataError("CrossChainTransfer date length")
    }
    val type = data[2]
    var label: String? = null
    val address = when (type) {
        0x00.toByte() -> {
            String(data, 3, data.size - 3)
        }
        0x01.toByte() -> {
            try {
                val addressSize = data[3].toInt()
                val addressOffset = 3 + 1
                val address = String(data, addressOffset, addressSize)
                val labelSize = data[addressOffset + addressSize].toInt()
                val labelOffset = addressOffset + addressSize + 1
                if (data.size != labelOffset + labelSize) {
                    throw ParseException.dataError("CrossChainTransfer size error")
                }
                label = String(data, labelOffset, labelSize)

                "$address"
            } catch (e: Exception) {
                throw ParseException.dataError(e.message ?: "CrossChainTransfer data ")
            }
        }
        else -> {
            throw ParseException.dataError("CrossChainTransfer data type error")
        }
    }
    val desc = WcDesc.create(BuildInTransferDesc["CrossChainTransfer"]!!)

    val amountInSu = block.amount.toBigIntegerOrNull()
        ?: throw ParseException.extendError("amount is not bigInt")
    val feeInSu =
        extend?.fee?.toBigIntegerOrNull() ?: throw ParseException.extendError("fee is not bigInt")

    val feeInBuStr = tokenInfo.amountTextWithSymbol(feeInSu, 8)
    val amountWithoutFeeBuStr = tokenInfo.amountTextWithSymbol(amountInSu.minus(feeInSu), 8)
    val labelItem = label?.let {
        if (extend?.labelTitle != null) {
            val gson = Gson()
            val labelTitleNameItem =
                gson.fromJson<WcNameItem>(gson.toJson(extend?.labelTitle), WcNameItem::class.java)
            WCConfirmItemInfo.create(
                labelTitleNameItem, label
            )
        } else {
            null
        }
    }

    val wcItems = mutableListOf(
        WCConfirmItemInfo.create(
            desc.inputs[0],
            amountWithoutFeeBuStr
        ),
        WCConfirmItemInfo.create(
            desc.inputs[1], feeInBuStr
        ),
        WCConfirmItemInfo.create(
            desc.inputs[2], address
        )
    )
    if (labelItem != null) {
        wcItems.add(labelItem)
    }

    return WCConfirmInfo(
        desc.title(),
        wcItems
    )
}


fun Session.MethodCall.SendTransaction.normalTransfer(tokenInfo: NormalTokenInfo): WCConfirmInfo {
    return normalTransferWcConfirmInfo(tokenInfo, block)
}

fun normalTransferWcConfirmInfo(
    tokenInfo: NormalTokenInfo,
    block: Session.MethodCall.ViteBlock
): WCConfirmInfo {
    var isUtf8 = false
    val mark = if (block.data == null) {
        null
    } else {
        runCatching {
            isUtf8 = true
            String(block.data!!, Charset.forName("UTF-8"))
        }.getOrNull() ?: run {
            isUtf8 = false
            block.data?.toHex()
        }
    }

    val desc = WcDesc.create(BuildInTransferDesc["Transfer"]!!)

    return WCConfirmInfo(
        desc.title(),
        listOf(
            WCConfirmItemInfo.create(
                desc.inputs[0], block.toAddress
            ),
            WCConfirmItemInfo.create(
                desc.inputs[1],
                tokenInfo.amountTextWithSymbol(amount = block.amount.toBigInteger(), scale = 8)
            ),
            if (isUtf8 || mark == null) {
                WCConfirmItemInfo.create(
                    desc.inputs[2],
                    mark ?: ""
                )
            } else {
                WCConfirmItemInfo.create(
                    "Data",
                    mark ?: ""
                )
            }

        )
    )
}


