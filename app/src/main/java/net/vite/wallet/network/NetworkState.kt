package net.vite.wallet.network

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.View
import androidx.annotation.Keep
import net.vite.wallet.R
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.network.http.gw.OverChainApiException
import retrofit2.HttpException
import java.net.SocketTimeoutException

@Keep
data class NetworkState private constructor(val status: Int, val throwable: Throwable? = null) {
    val progressVisible: Int
        get() = if (status == 4001) {
            View.VISIBLE
        } else {
            View.GONE
        }


    fun isError() = status == 4002

    fun showBaseErrorDialog(activity: Activity, f: (dialog: Dialog) -> Unit = { dialog -> dialog.dismiss() }) {
        TextTitleNotifyDialog(activity).apply {
            setTitle(R.string.failed)
            setMessage(tryGetErrorMsg(activity))
            setBottom(R.string.confirm) {
                f.invoke(it)
            }
        }.show()
    }

    fun tryGetErrorMsg(context: Context? = null): String {
        val error = throwable
        return when (error) {
            is OverChainApiException -> context?.getString(R.string.request_server_error, error.message ?: "")
                ?: "request failed (${error.message ?: ""}) "
            is HttpException -> context?.getString(R.string.http_service_error, error.code().toString())
                ?: "Service invalid (${error.code()})"
//            is ConnectException -> context?.getString(R.string.net_work_error)
//                ?: "Connect service failed"
            is SocketTimeoutException -> context?.getString(R.string.net_work_timeout)
                ?: "Connect service timeout"
            else -> context?.getString(R.string.net_work_error)
                ?: "Connect service failed"
        }
    }

    companion object {
        val LOADED = NetworkState(4000)
        val LOADING = NetworkState(4001)
        val INIT = NetworkState(4003)
        fun error(throwable: Throwable?) = NetworkState(4002, throwable)
    }
}
