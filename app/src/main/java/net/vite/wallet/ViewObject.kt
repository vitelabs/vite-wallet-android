package net.vite.wallet

import android.app.Dialog
import android.content.Context
import android.view.View
import net.vite.wallet.network.NetworkState

class ViewObject<T> private constructor(
    var networkState: NetworkState,
    var resp: T? = null,
    var requestCode: Int? = null,
    var metaData: Any? = null
) {

    companion object {
        fun <T> Init(requestCode: Int? = null, metaData: Any? = null) =
            ViewObject<T>(NetworkState.INIT, null, requestCode, metaData)

        fun <T> Loading(requestCode: Int? = null, metaData: Any? = null) =
            ViewObject<T>(NetworkState.LOADING, null, requestCode, metaData)

        fun <T> Loaded(resp: T?, requestCode: Int? = null, metaData: Any? = null) =
            ViewObject(NetworkState.LOADED, resp, requestCode, metaData)

        fun <T> Error(throwable: Throwable?, requestCode: Int? = null, metaData: Any? = null) =
            ViewObject<T>(NetworkState.error(throwable), null, requestCode, metaData)
    }

    val progressVisible: Int
        get() = if (networkState.status == 4001) {
            View.VISIBLE
        } else {
            View.GONE
        }

    fun error(): Throwable? = networkState.throwable
    fun isError() = networkState.status == 4002
    fun isLoading() = networkState.status == 4001
    fun isInit() = networkState == NetworkState.INIT
    fun isSuccess() = networkState == NetworkState.LOADED

    fun handleDialogShowOrDismiss(dialog: Dialog) {
        if (isLoading()) {
            if (!dialog.isShowing) {
                dialog.show()
            }
        } else {
            dialog.dismiss()
        }
    }

    fun tryGetErrorMsg(context: Context? = null): String {
        return networkState.tryGetErrorMsg(context)
    }
}