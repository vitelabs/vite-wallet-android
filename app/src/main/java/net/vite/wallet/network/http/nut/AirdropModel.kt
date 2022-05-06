package net.vite.wallet.network.http.nut

import androidx.annotation.Keep
import io.reactivex.Observable

class GrowthApiException : Exception {
    val code: Int?
    val msg: String?
    val isNullResp: Boolean?

    constructor(code: Int?, msg: String?) : super("code $code msg:$msg") {
        this.code = code
        this.msg = msg
        isNullResp = null
    }

    constructor() : super("null resp") {
        this.code = null
        this.msg = null
        isNullResp = true
    }

    fun toReadableString(): String {
        return msg ?: code?.toString() ?: "null resp"
    }

    fun isNullResp() = isNullResp == true
}

@Keep
data class GrowthBaseResponse<T>(
    val code: Int?,
    val `data`: T?,
    val msg: String?
)


fun <T> Observable<GrowthBaseResponse<T>>.processGrowthBase() = flatMap {
    when {
        it.code != 0 -> Observable.error(GrowthApiException(it.code, it.msg))
        it.data == null -> Observable.error(GrowthApiException())
        else -> Observable.just(it.data)
    }
}
