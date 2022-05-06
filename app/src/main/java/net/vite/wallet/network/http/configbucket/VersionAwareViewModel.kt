package net.vite.wallet.network.http.configbucket

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.*

@Keep
data class LocalizedStr(
    val base: String = "",
    val localized: Map<String, String> = HashMap()
)

@Keep
data class VersionUpdate(
    val isForce: Boolean = false,
    val forceUrl: String = "",
    @SerializedName("build")
    val code: Int = 0,
    val versionName: String = "",
    val forced: Int = 0,
    val url: String = "",
    val message: LocalizedStr?,
    val title: LocalizedStr?,
    val okTitle: LocalizedStr?,
    val cancelTitle: LocalizedStr?,
    val dontShowInSplash: Boolean?
)






