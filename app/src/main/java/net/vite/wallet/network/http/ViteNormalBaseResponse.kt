package net.vite.wallet.network.http

import androidx.annotation.Keep
import io.reactivex.Observable
import net.vite.wallet.network.http.vitex.ViteXApiException

@Keep
data class ViteNormalBaseResponse<T>(
    val code: Int?,
    val `data`: T?,
    val msg: String?
) {
    fun throwable(): ViteHttpException {
        return ViteHttpException(msg, code)
    }
}

class ViteHttpException(msg: String?, code: Int?) : Exception("msg: $msg code: $code")

fun <T> Observable<ViteNormalBaseResponse<T>>.processViteBase() = flatMap {
    when {
        it.code != 0 -> Observable.error(ViteXApiException(it.code, it.msg))
        it.data == null -> Observable.error(ViteXApiException())
        else -> Observable.just(it.data)
    }
}

fun <T> Observable<ViteNormalBaseResponse<T>>.processViteBaseWithoutData() = flatMap {
    when {
        it.code != 0 -> Observable.error(ViteXApiException(it.code, it.msg))
        else -> Observable.just(Unit)
    }
}
