package net.vite.wallet.vitebridge

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.ViteTokenBalanceDetailActivity
import net.vite.wallet.utils.WhiteHostManager
import okhttp3.HttpUrl

object UrlOpenHelper {


    fun open(activity: Activity, url: String): Disposable? {
        val httpUrl = HttpUrl.parse(url) ?: return null
        if (httpUrl.encodedPath() == "/native_app/token_balance_info") {
            val tokenCode = httpUrl.queryParameter("token_code") ?: return null
            val address = httpUrl.queryParameter("address") ?: return null
            if (address != AccountCenter.currentViteAddress()) {
                return null
            }

            return TokenInfoCenter.queryTokenDetail(tokenCode)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    ViteTokenBalanceDetailActivity.show(activity, tokenCode)
                }
        }

        if (WhiteHostManager.isWhiteHost(url)) {
            H5WebActivity.show(activity, url)
        } else {
            AlertDialog.Builder(activity)
                .setMessage(R.string.warning_of_jump_to_third)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    H5WebActivity.show(activity, url)
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .create().show()
        }
        return null
    }

}