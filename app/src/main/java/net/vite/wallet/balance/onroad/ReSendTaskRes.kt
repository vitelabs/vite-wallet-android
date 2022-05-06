package net.vite.wallet.balance.onroad

import androidx.annotation.Keep

@Keep
data class ReSendTaskRes(
    var isSuccess: Boolean? = null,
    var msg: String? = null
) {
    companion object {
        val REQ_INVALI = "request is invalid!"
        val SERVER_ERROR = "internal error!"
        val SIGNATURE_ERROR = "signature error!"
        val NOT_ALLOW = "you can not resend this time!"
        val RESNED = "is resending!"
    }
}