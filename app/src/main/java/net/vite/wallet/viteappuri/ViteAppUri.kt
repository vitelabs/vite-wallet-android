package net.vite.wallet.viteappuri

import android.app.Activity
import android.content.Context
import android.net.Uri
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.uritx.UriTxActivity
import net.vite.wallet.constants.DexCancelContractAddress
import net.vite.wallet.constants.DexContractAddress
import net.vite.wallet.vep.decodeViteTransferUrl
import net.vite.wallet.vitebridge.UrlOpenHelper

class ViteAppUri private constructor(val uri: Uri) {
    companion object {
        fun createOrNull(uriString: String): ViteAppUri? {
            return kotlin.runCatching {
                val uri = Uri.parse(uriString)
                createOrNull(uri)
            }.getOrNull()

        }

        fun createOrNull(uri: Uri): ViteAppUri? {
            return kotlin.runCatching {
                if (isSupportUri(uri)) {
                    ViteAppUri(uri)
                } else {
                    null
                }
            }.getOrNull()
        }

        fun isSupportUri(uriString: String): Boolean {
            return createOrNull(uriString) != null
        }

        fun isSupportUri(uri: Uri): Boolean {
            if (uri.scheme != "viteapp") {
                return false
            }
            if (BackupWalletAction.tryCreate(uri) != null) {
                return true
            }
            if (uri.host == "vote" || uri.host == "open") {
                return true
            }
            if (uri.host == "send-tx") {
                val vepurl = uri.getQueryParameter("uri") ?: return false
                val viteTransferUrl =
                    kotlin.runCatching { decodeViteTransferUrl(vepurl) }.getOrNull() ?: return false
                return viteTransferUrl.toAddr != DexContractAddress && viteTransferUrl.toAddr != DexCancelContractAddress
            }
            return false
        }
    }

    fun openUriTxActivity(context: Context, uri: Uri) {
        if (!AccountCenter.isLogin()) {
            return
        }
        UriTxActivity.show(context, uri)
    }

    fun action(activity: Activity) {
        if (!isSupportUri(uri)) {
            return
        }
        if (uri.host == "open") {
            val url = uri.getQueryParameter("url") ?: return
            UrlOpenHelper.open(activity, url)
            return
        }

        openUriTxActivity(activity, uri)
    }

}