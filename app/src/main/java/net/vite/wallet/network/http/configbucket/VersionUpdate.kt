package net.vite.wallet.network.http.configbucket

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.network.NetworkState
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.isChinese


class VersionAwareViewModel : ViewModel() {
    val versionAwareLd = MutableLiveData<VersionUpdate>()
    val networkState = MutableLiveData<NetworkState>()

    @SuppressLint("CheckResult")
    fun checkVersion() {
        networkState.postValue(NetworkState.LOADING)
        ConfigBucketApi.checkVersion()
            .subscribe({
                versionAwareLd.postValue(it)
                networkState.postValue(NetworkState.LOADED)
            }, {
                networkState.postValue(NetworkState.error(it))
            })
    }
}


fun prepareVersionDialog(
    context: Context,
    newVersion: VersionUpdate,
    currentCode: Int,
    cancelLis: () -> Unit = {}
): AlertDialog {
    var ok = newVersion.okTitle?.base ?: context.getString(R.string.update)
    var cancel = newVersion.cancelTitle?.base ?: context.getString(R.string.cancel)
    var title = newVersion.title?.base ?: ""
    var message = newVersion.message?.base ?: ""

    if (context.isChinese()) {
        title = newVersion.title?.localized?.get("zh-Hans") ?: title
        message = newVersion.message?.localized?.get("zh-Hans") ?: message
        ok = newVersion.okTitle?.localized?.get("zh-Hans") ?: ok
        cancel = newVersion.cancelTitle?.localized?.get("zh-Hans") ?: cancel
    }

    return AlertDialog.Builder(context)
        .setMessage(message)
        .setTitle(title)
        .setPositiveButton(ok) { _, _ ->
            try {
                try {
                    if (newVersion.forceUrl.isNotBlank()) {
                        Uri.parse(newVersion.forceUrl)
                        context.startActivity(createBrowserIntent(newVersion.forceUrl))
                        return@setPositiveButton
                    }
                } catch (e: Exception) {
                }

                if (ViteConfig.get().channel == "google") {
                    context.startActivity(createBrowserIntent("https://play.google.com/store/apps/details?id=net.vite.wallet"))
                } else {
                    Uri.parse(newVersion.url)
                    context.startActivity(createBrowserIntent(newVersion.url))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        .setCancelable(false)
        .apply {
            if (!newVersion.isForce && currentCode >= newVersion.forced) {
                setNegativeButton(cancel) { dialog, _ ->
                    dialog.dismiss()
                    cancelLis()
                }
            }
        }.create()
}
