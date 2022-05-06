package net.vite.wallet.balance.walletconnect.taskdetail

import android.annotation.SuppressLint
import android.util.Base64
import com.google.gson.Gson
import io.reactivex.Observable
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.ViteConfig
import net.vite.wallet.abi.AbiJsonParse
import net.vite.wallet.abi.FunctionReturnDecoder
import net.vite.wallet.abi.datatypes.Bytes
import net.vite.wallet.abi.datatypes.Type
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.constants.*
import net.vite.wallet.logt
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.rpc.NormalTxParams
import net.vite.wallet.utils.toHex
import org.vitelabs.mobile.Mobile
import org.walletconnect.Session

fun Session.MethodCall.ViteBlock.toNormalTxParams(accountAddress: String) = NormalTxParams(
    accountAddr = accountAddress,
    toAddr = toAddress,
    amountInSu = amount.toBigInteger(),
    tokenId = tokenId,
    data = data?.let {
        Base64.encodeToString(it, Base64.NO_WRAP)
    },
    fee = fee?.toBigIntegerOrNull(),
    blockType = blockType
)

fun Session.MethodCall.SendTransaction.parse(): Observable<WCConfirmInfo> {
    return TokenInfoCenter.queryViteToken(block.tokenId).flatMap { normaltokenInfo ->
        try {
            Observable.just(
                if (Mobile.isContactAddress(block.toAddress)) {
                    parseAsContact(normaltokenInfo)
                } else {
                    parseAsTransfer(normaltokenInfo)
                }
            )
        } catch (e: Exception) {
            Observable.error<WCConfirmInfo>(e)
        }
    }
}


fun Session.MethodCall.SignMessage.parse(): Observable<WCConfirmInfo> {


    return Observable.just(
        WCConfirmInfo(
            ViteConfig.get().context.getString(R.string.create_api), listOf(
                WCConfirmItemInfo.create(
                    WcNameItem(WcLangItem(base = "Current Address", zh = "当前地址")),
                    AccountCenter.currentViteAddress()!!
                ),
                WCConfirmItemInfo.create(
                    WcNameItem(WcLangItem(base = "Permission Type", zh = "权限类型")),
                    WcNameItem(WcLangItem(base = "Trade", zh = "交易权限"))
                ),

                WCConfirmItemInfo.create(
                    WcNameItem(WcLangItem(base = "Signature Content", zh = "签名内容")),
                    message
                )
            )
        )
    )
}


class DataDecodeResult(
    val value: Type<*>,
    val paramName: String?
)

fun Session.MethodCall.SendTransaction.parseContract(abi: String): List<DataDecodeResult> {
    val parseResult = AbiJsonParse.parse(abi)
    return if (parseResult.paramNameTypePair.isNullOrEmpty()) {
        emptyList<DataDecodeResult>()
    } else {
        FunctionReturnDecoder.decode(
            block.data!!.toHex().substring(8),
            parseResult.getParamsList()!!
        ).foldIndexed(ArrayList<DataDecodeResult>()) { index, acc, item ->
            val paramName = parseResult.paramNameTypePair[index].first
            acc.add(DataDecodeResult(item, paramName))
            acc
        }
    }.also {
        it.forEach {
            logt("parseBuildInContract ${it.paramName} ${it.value.typeAsString} ${it.value.value}")
        }
    }
}

private fun Session.MethodCall.SendTransaction.parseOtherContract(): List<DataDecodeResult> {
    if (abi == null) {
        throw IllegalArgumentException("third contract must contain abi")
    }
    return parseContract(abi!!)
}


private fun Session.MethodCall.SendTransaction.parseOtherContractToConfirmInfoWithAbi(): WCConfirmInfo {
    val abiDataParseResult = parseOtherContract()

    val wcDesc = if (description != null) {
        val gson = Gson()
        gson.fromJson<WcDesc>(gson.toJson(description), WcDesc::class.java)
    } else {
        null
    }

    val list = ArrayList<WCConfirmItemInfo>()
    abiDataParseResult.forEachIndexed { index, result ->
        val text = if (result.value is Bytes) {
            result.value.value.toHex()
        } else {
            result.value.value.toString()
        }

        list.add(
            WCConfirmItemInfo.create(
                wcDesc?.inputs?.get(index) ?: WcNameItem(
                    WcLangItem(
                        result.paramName
                    )
                ), text
            )
        )
    }

    return WCConfirmInfo(wcDesc?.title() ?: "", list)
}

fun parseOtherContractToConfirmInfoOnlyUserViteBlock(
    blockTokenIdTokenInfo: NormalTokenInfo,
    block: Session.MethodCall.ViteBlock
): WCConfirmInfo {
    val items = mutableListOf(
        WCConfirmItemInfo.create(
            WcNameItem(WcLangItem(base = "Contract Address", zh = "合约地址")),
            block.toAddress
        ),
        WCConfirmItemInfo.create(
            WcNameItem(WcLangItem(base = "Token Symbol", zh = "币种")),
            blockTokenIdTokenInfo.symbol ?: ""
        ),
        WCConfirmItemInfo.create(
            WcNameItem(WcLangItem(base = "Amount", zh = "金额")),
            blockTokenIdTokenInfo.amountText(block.amount.toBigInteger(), 8)
        )
    )
    block.data?.let {
        items.add(
            WCConfirmItemInfo.create(
                WcNameItem(WcLangItem(base = "Data", zh = "Data")),
                it.toHex()
            )
        )
    }

    return WCConfirmInfo(
        WcLangItem(base = "Contract Invoke", zh = "合约调用").getTitle(),
        items
    )
}

@SuppressLint("CheckResult")
fun Session.MethodCall.SendTransaction.parseOtherContractToConfirmInfo(blockTokenIdTokenInfo: NormalTokenInfo): WCConfirmInfo {
    return try {
        parseOtherContractToConfirmInfoWithAbi()
    } catch (e: Exception) {
        parseOtherContractToConfirmInfoOnlyUserViteBlock(blockTokenIdTokenInfo, block)
    }
}

private fun Session.MethodCall.SendTransaction.parseAsContact(blockTokenIdTokenInfo: NormalTokenInfo): WCConfirmInfo {
    val data = block.data
    if (data != null && data.size >= 4) {
        val typeHex = data.sliceArray(0 until 4).toHex()
        val type = DataPrefixToBlockType[typeHex] ?: return parseOtherContractToConfirmInfo(
            blockTokenIdTokenInfo
        )
        val expectAddr = BlockTypeToContactAddress[type]
        if (expectAddr == block.toAddress) {
            return parseInnerContractToConfirmInfo(
                parseContract(BuildInContactAbiMap[type]!!),
                blockTokenIdTokenInfo,
                type
            )
        }
    }
    return parseOtherContractToConfirmInfo(blockTokenIdTokenInfo)
}

private fun Session.MethodCall.SendTransaction.parseAsTransfer(normalTokenInfo: NormalTokenInfo): WCConfirmInfo {
    if (extend != null) {
        if (extend.type != Session.MethodCall.SendTransactionExtension.CrossChainTransfer) {
            throw ParseException.extendError("CrossChainTransfer extend type")
        }
        if (block.dataType() != 0x0bc3.toShort()) {
            throw ParseException.dataError("CrossChainTransfer prefix")
        }
        return crossChainTransfer(normalTokenInfo)
    }
    return normalTransfer(normalTokenInfo)
}
