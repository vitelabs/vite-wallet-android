package net.vite.wallet.network.rpc

import android.util.Base64
import net.vite.wallet.abi.FunctionEncoder
import net.vite.wallet.abi.datatypes.*
import net.vite.wallet.abi.datatypes.Function
import net.vite.wallet.abi.datatypes.generated.Bytes32
import net.vite.wallet.abi.datatypes.generated.Uint256
import net.vite.wallet.abi.datatypes.generated.Uint32
import net.vite.wallet.abi.datatypes.generated.Uint8
import net.vite.wallet.utils.hexToBytes
import org.vitelabs.mobile.Mobile
import java.math.BigInteger

object BuildInContractEncoder {
    fun encodeCancelPledge(address: String, amountInMini: BigInteger): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "CancelPledge",
                listOf(Address(Mobile.newAddressFromString(address)), Uint256(amountInMini)),
                emptyList()
            )
        )


        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun encodeCancelPledgeNew(id: Bytes32): String {
        val hexData = FunctionEncoder.encode(
            Function("CancelQuotaStaking", listOf(id), emptyList())
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun encodePledge(address: String): String {// old pledge
        val hexData = FunctionEncoder.encode(
            Function(
                "Pledge",
                listOf(Address(Mobile.newAddressFromString(address))),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun encodePledgeNew(address: String): String {// old pledge
        val hexData = FunctionEncoder.encode(
            Function(
                "StakeForQuota",
                listOf(Address(Mobile.newAddressFromString(address))),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun encodeCancelVote(gid: String = DefaultGid): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "CancelSBPVoting",
                listOf(),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun encodeVote(voteName: String, gid: String = DefaultGid): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "VoteForSBP",
                listOf(Utf8String(voteName)),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }


    fun dexBindInviteCode(code: BigInteger): String {

        val hexData = FunctionEncoder.encode(
            Function(
                "BindInviteCode",
                listOf(Uint32(code)),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }


    fun dexPost(): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "Deposit",
                emptyList(),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun dexPlaceOrder(
        tradeToken: String,
        quoteToken: String,
        side: Boolean,
        orderType: Long,
        price: String,
        quantity: BigInteger
    ): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "PlaceOrder",
                listOf(
                    TokenId(Mobile.newTokenTypeIdFromString(tradeToken)),
                    TokenId(Mobile.newTokenTypeIdFromString(quoteToken)),
                    Bool(side),
                    Uint8(orderType),
                    Utf8String(price),
                    Uint256(quantity)
                ),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun stakeForVIP(actionType: Long): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "StakeForVIP",
                listOf(Uint8(actionType)),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun cancelStakeById(id: ByteArray): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "CancelStakeById",
                listOf(Bytes32(id)),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun dexCancelOrder(orderId: ByteArray): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "CancelOrder",
                listOf(DynamicBytes(orderId)),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun dexCancelOrderByTransactionHash(sendHash: ByteArray): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "CancelOrderByTransactionHash",
                listOf(Bytes32(sendHash)),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

    fun dexDexwithdraw(tokenId: String, amountInMini: BigInteger): String {
        val hexData = FunctionEncoder.encode(
            Function(
                "Withdraw",
                listOf(TokenId(Mobile.newTokenTypeIdFromString(tokenId)), Uint256(amountInMini)),
                emptyList()
            )
        )
        return Base64.encodeToString(hexData.hexToBytes(), Base64.NO_WRAP)
    }

}