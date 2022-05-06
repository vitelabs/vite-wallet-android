package net.vite.wallet.vitebridge

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import net.vite.wallet.ENV_PRODUCT
import net.vite.wallet.ENV_TEST
import net.vite.wallet.ViteConfig


@Keep
data class JsEncryptResult(
    val base64: String
)

@Keep
data class JsBridgeResp<T>(
    val data: T?,
    val code: Int = 0,
    val msg: String = ""
) {
    companion object {

        val SuccessResp = JsBridgeResp<Any>(null, 0, "")
        val UnknownError = JsBridgeResp<Any>(null, 1, "Unknown error")
        val InvalidParam = JsBridgeResp<Any>(null, 2, "Invalid param")
        val NetWorkError = JsBridgeResp<Any>(null, 3, "Network error")
        val LoginError = JsBridgeResp<Any>(null, 4, "Login error")
        val AddressNotMatch = JsBridgeResp<Any>(null, 5, "Address not match error")

        fun createSystemPermissionError(permissionName: String): JsBridgeResp<Any> {
            return JsBridgeResp(null, 6, "system permission not granted $permissionName")
        }

        fun createAppPermissionError(permissionName: String): JsBridgeResp<Any> {
            return JsBridgeResp(null, 7, "app permission not granted $permissionName")
        }
    }


    override fun toString(): String {
        return Gson().toJson(this)
    }
}

@Keep
data class JsBridgeVersion(
    val versionName: String,
    val versionCode: Int
) {
    companion object {
        val NowVersion = JsBridgeVersion("1.3.0", 5)
    }
}

@Keep
data class AppInfo(
    val platform: String,
    val versionName: String,
    val versionCode: Int,
    val env: String,
    val uuid: String
) {
    companion object {
        fun nowAppInfo(context: Context) = AppInfo(
            platform = "android",
            versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName,
            versionCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode,
            env = when (ViteConfig.get().currentEnv) {
                ENV_TEST -> "test"
                ENV_PRODUCT -> "production"
                else -> "unknown"
            },
            uuid = ""
        )
    }
}