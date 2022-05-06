package net.vite.wallet.network.rpc

import android.util.Base64
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.constants.*
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.utils.hexToBytes
import net.vite.wallet.utils.toHex
import net.vite.wallet.utils.toLeftPadByteArray
import org.vitelabs.mobile.Address
import org.vitelabs.mobile.Mobile
import org.vitelabs.mobile.TokenTypeId
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.min

class ABFiledLackException(msg: String = "") : Exception("ABFiledLackException $msg")

@Keep
data class AccountBlock(
    @SerializedName("blockType") var blockType: Byte? = null, // Byte
    @SerializedName("hash") var hash: String? = null,  // hex string
    @SerializedName("prevHash") var prevHash: String? = null,  // hex string
    @SerializedName("height") var height: String? = null,
    @SerializedName("accountAddress") var accountAddress: String? = null,
    @SerializedName("publicKey") var publicKey: String? = null, // base64 string
    @SerializedName("fromAddress") var fromAddress: String? = null,
    @SerializedName("toAddress") var toAddress: String? = null,
    @SerializedName("amount") var amount: String? = null,
    @SerializedName("tokenId") var tokenId: String? = null,
    @SerializedName("fromBlockHash") var fromBlockHash: String? = null,
    @SerializedName("receiveBlockHash") var receiveBlockHash: String? = null,
    @SerializedName("firstSnapshotHeight") var firstSnapshotHeight: String? = null,
//    @SerializedName("snapshotHash") var snapshotHash: String? = null,
    @SerializedName("data") var data: String? = null, // base64
    @SerializedName("quota") var quota: String? = null,
    @SerializedName("fee") var fee: String? = null,
    @SerializedName("logHash") var logHash: String? = null,
    @SerializedName("difficulty") var difficulty: String? = null, // 10  bigint
    @SerializedName("timestamp") var timestamp: Long? = null, //  unit : second
    @SerializedName("nonce") var nonce: String? = null, // base64
    @SerializedName("signature") var signature: String? = null, // base64
    @SerializedName("confirmedTimes") var confirmedTimes: String? = null,
    @SerializedName("tokenInfo") var viteChainTokenInfo: ViteChainTokenInfo? = null
) {
    companion object {
        const val BlockTypeSendCreate = 1.toByte()
        const val BlockTypeSendCall = 2.toByte()
        const val BlockTypeSendReward = 3.toByte()
        const val BlockTypeReceive = 4.toByte()
        const val BlockTypeReceiveError = 5.toByte()
        const val BlockTypeSendRefund = 6.toByte()
        const val BlockTypeGenenis = 7.toByte()
    }

    fun blockDetailType(): Int {
        return blockType?.let { rawType ->
            when (rawType) {
                BlockTypeSendCreate, BlockTypeSendReward -> BlockDetailTypeSend
                BlockTypeReceiveError -> BlockDetailTypeReceive
                BlockTypeSendCall -> {
                    data?.let { nd ->
                        if (Mobile.isContactAddress(toAddress)) {
                            val dataHex = Base64.decode(nd, Base64.NO_WRAP).toHex()
                            if (dataHex.length >= 8) {
                                val pre8Hex = dataHex.substring(0, 8)

                                val datatype = DataPrefixToBlockType[pre8Hex]
                                if (BlockTypeToContactAddress[datatype] == toAddress) {
                                    datatype
                                } else {
                                    BlockDetailTypeContractCall
                                }
                            } else {
                                BlockDetailTypeContractCall
                            }
                        } else {
                            BlockDetailTypeSend
                        }

                    } ?: BlockDetailTypeSend
                }
                BlockTypeReceive -> BlockDetailTypeReceive
                else -> BlockDetailTypeReceive
            }
        } ?: BlockDetailTypeReceive

    }

    fun isSendBlock(): Boolean? {
        return blockType?.let {
            it == BlockTypeSendCreate || it == BlockTypeSendCall
                    || it == BlockTypeSendReward || it == BlockTypeSendRefund
        }
    }

    fun isReceiveBlock(): Boolean? {
        return blockType?.let {
            it == BlockTypeReceive || it == BlockTypeReceiveError
        }
    }

    fun getMsTimestamp(): Long? {
        return timestamp?.let { 1000 * it }
    }

    fun getAmountReadableText(scale: Int): String {
        return amount?.toBigDecimal()?.divide(
            BigDecimal.TEN.pow(viteChainTokenInfo?.decimals ?: 0),
            min(scale, viteChainTokenInfo?.decimals ?: 0),
            RoundingMode.DOWN
        )?.toLocalReadableText(scale) ?: "0"
    }

    fun getRawPreBytes(): ArrayList<Byte> {
        val preBytes = ArrayList<Byte>()
        preBytes.apply {
            blockType?.let {
                add(it)
            } ?: throw ABFiledLackException("blockType")

            prevHash?.let {
                addAll(it.hexToBytes().toList())
            } ?: throw ABFiledLackException("prevHash")

            height?.let {
                // haha java is big endian by default -.- happy about this
                val b = ByteBuffer.allocate(8)
                b.putLong(it.toLong())
                addAll(b.array().toList())
            } ?: throw ABFiledLackException("height")

            accountAddress?.let {
                addAll(Address(it).bytes.toList())
            } ?: throw ABFiledLackException("accountAddress")

            isSendBlock()?.let {
                if (it) {
                    // is sendblock
                    toAddress?.let {
                        addAll(Address(it).bytes.toList())
                    } ?: throw ABFiledLackException("toAddress")
                    amount?.let {
                        addAll(it.toBigInteger().toLeftPadByteArray(32).toMutableList())
                    } ?: throw ABFiledLackException("amount")
                    tokenId?.let {
                        addAll(TokenTypeId(it).bytes.toList())
                    } ?: throw ABFiledLackException("tokenId")
                } else {
                    // is receive block
                    fromBlockHash?.let {
                        addAll(it.hexToBytes().toList())
                    } ?: throw ABFiledLackException("fromBlockHash")
                }
            } ?: throw ABFiledLackException("isSendBlock")

            data?.let {
                addAll(Mobile.hash256(Base64.decode(it, Base64.NO_WRAP)).toList())
            }


            fee?.let {
                addAll(it.toBigInteger().toLeftPadByteArray(32).toMutableList())
            } ?: run {
                addAll(ByteArray(32).toList())
            }

            logHash?.let {
                addAll(it.hexToBytes().toList())
            }

            nonce?.let {
                val nonce = Base64.decode(it, Base64.NO_WRAP)
                addAll(BigInteger(nonce).toLeftPadByteArray(8).toList())
            } ?: run {
                addAll(ByteArray(8).toList())
            }
        }

        preBytes.trimToSize()
        return preBytes
    }

    fun computeHash() {
        hash = Mobile.hash256(getRawPreBytes().toByteArray()).toHex()
    }

    fun sign() {
        hash?.let {
            val signDataResult = AccountCenter.getCurrentAccount()?.signVite(it)
            signDataResult?.let { sd ->
                publicKey = Base64.encodeToString(sd.publicKey, Base64.NO_WRAP)
                signature = Base64.encodeToString(sd.signature, Base64.NO_WRAP)
            }
        }
    }

}