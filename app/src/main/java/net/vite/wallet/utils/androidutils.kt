package net.vite.wallet.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.fingerprint.FingerprintManager
import android.net.ConnectivityManager
import android.net.Uri
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.graphics.toColorInt
import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.dialog.LogoTitleNotifyDialog
import net.vite.wallet.network.rpc.RpcException
import net.vite.wallet.setup.LanguageUtils
import java.text.SimpleDateFormat
import java.util.*

object ToastFactory {
    var toast: Toast? = null
}

fun Context.showToast(s: String) {
    ToastFactory.toast?.cancel()
    ToastFactory.toast = Toast.makeText(this, s, Toast.LENGTH_SHORT)
    ToastFactory.toast?.show()
}

fun Context.showToast(@StringRes s: Int) {
    ToastFactory.toast?.cancel()
    ToastFactory.toast = Toast.makeText(this, s, Toast.LENGTH_SHORT)
    ToastFactory.toast?.show()
}


fun Context.currentLocale(): Locale {
    return Locale(
        this.resources.configuration.locale.language,
        this.resources.configuration.locale.country
    )
}

fun Context.isChinese(): Boolean {
    val langStored = LanguageUtils.currentLang(this)
    if (langStored != "" && langStored == "zh") {
        return true
    }
    if (langStored != "" && langStored == "en") {
        return false
    }
    return this.currentLocale() == Locale.SIMPLIFIED_CHINESE || this.currentLocale() == Locale.TRADITIONAL_CHINESE
}

fun Context.hasNetWork(): Boolean {
    val netinfo =
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
            ?: return false
    return netinfo.isConnected
}

fun getGoogleGms(context: Context) = getAppInfoString("com.google.android.gms", context)
fun getGooglePlayStore(context: Context) = getAppInfoString("com.android.vending", context)
fun getGsf(context: Context) = getAppInfoString("com.google.android.gsf", context)

fun Context.canUseFingerprint(): Boolean {
    val f = this.getSystemService(FingerprintManager::class.java)
    return ViteConfig.get().kvstore.getBoolean(
        "USEFingerprint",
        false
    ) && f.isHardwareDetected && f.hasEnrolledFingerprints()
}

fun getAppInfoString(packageName: String, context: Context): String {
    val dataFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")

    val info = try {
        context.packageManager.getPackageInfo(packageName, 0)
    } catch (e: Exception) {
        null
    }
    return info?.let {
        val s = StringBuilder(packageName)
        try {
            s.append("\nvn:").append(it.versionName)
            s.append("\nftime:").append(dataFormat.format(it.firstInstallTime))
            s.append("\nltime:").append(dataFormat.format(it.lastUpdateTime))
        } catch (e: Exception) {
            s
        }
        s.append("\nend\n")
    }?.toString() ?: ""
}

fun Throwable.shownMessage(activity: Activity) =
    (if (this is RpcException) {
        when {
            isBalanceNotEnough() -> activity.getString(R.string.balance_not_enough)
            isQuotaNotEnough() -> activity.getString(R.string.quota_not_enough)
            isVmIdCollision() -> activity.getString(R.string.vm_id_collsion)
            isVmMethodNotFound() -> activity.getString(R.string.vm_method_not_found) + "($code)"
            isVmInvaildBlockData() -> activity.getString(R.string.vm_method_not_found) + "($code)"
            isVmCalPoWTwice() -> activity.getString(R.string.tx_send_too_frequent)


            code == -36001 -> activity.getString(R.string.need_reveive_a_tx_before_send)
            code == -36005 -> activity.getString(R.string.invalid_sb_height)
            else -> {
                activity.getString(R.string.verify_failed_by) + "($code)"
            }
        }
    } else {
        activity.getString(R.string.net_work_error)
    })!!

fun Throwable.showToast(activity: Activity) {
    Toast.makeText(activity, shownMessage(activity), Toast.LENGTH_SHORT).show()
}

fun Throwable.showDialogMsg(activity: Activity, bottomTxt: String = "") {
    val message = shownMessage(activity)
    var bt = bottomTxt
    if (bt == "") {
        bt = activity.getString(R.string.confirm)
    }
    LogoTitleNotifyDialog(activity).apply {
        enableTopImage(false)
        setMessage(message)
        setBottom(bt)
        show()
    }
}

fun String.symbolRemoveIndex(string: String?): String {
    if (string == null)
        return "--"
    var index = string.lastIndexOf("-")
    index = if (index == -1) string.length else index
    return string?.substring(0, index)
}


fun createBrowserIntent(url: String): Intent {
    return Intent().apply {
        data = Uri.parse(url)
        action = Intent.ACTION_VIEW
    }
}


@SuppressLint("UseCompatLoadingForDrawables")
fun TextView.blueWhiteGreyDarkSelect(isSelect: Boolean) {
    setPadding(
        17.0F.dp2px().toInt(),
        5.0F.dp2px().toInt(),
        17.0F.dp2px().toInt(),
        5.0F.dp2px().toInt()
    )
    textSize = 12.0F
    if (isSelect) {
        setBackgroundResource(R.drawable.trader_orders_filter_panel_item_select_bg)
        setTextColor(Color.WHITE)
    } else {
        setBackgroundResource(R.drawable.trader_orders_filter_panel_item_unselect_bg)
        setTextColor("#3E4A59".toColorInt())
    }
}