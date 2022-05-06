package net.vite.wallet.network.rpc

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

@Keep
data class RpcException(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val rpcmessage: String
) : Exception() {
    override fun toString(): String {
        return "code:$code message:$rpcmessage"
    }

    companion object {
        fun tryMake(e: Throwable): Throwable {
            return try {
                Gson().fromJson(e.message, RpcException::class.java)
            } catch (ne: Throwable) {
                e
            }
        }
    }

    fun isBalanceNotEnough() = code == -35001
    fun isQuotaNotEnough() = code == -35002
    fun isVmIdCollision() = code == -35003
    fun isVmInvaildBlockData() = code == -35004
    fun isVmCalPoWTwice() = code == -35005
    fun isVmMethodNotFound() = code == -35006

    fun isVerifyAccountAddr() = code == -36001
    fun isVerifyHash() = code == -36002
    fun isVerifySignature() = code == -36003
    fun isVerifyNonce() = code == -36004
    fun isVerifySnapshotOfReferredBlock() = code == -36005
}