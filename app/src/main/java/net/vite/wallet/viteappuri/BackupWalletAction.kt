package net.vite.wallet.viteappuri

import android.net.Uri
import net.vite.wallet.utils.hexToBytes

data class BackupWalletAction(
    val name: String,
    val entropy: String,
    val language: String,
    val password: String
) {
    fun toViteAppUriString(): String {
        return "viteapp://backup-wallet?name=${Uri.encode(name)}&entropy=$entropy&language=$language&password=$password"
    }

    companion object {
        fun tryCreate(uri: Uri): BackupWalletAction? {
            if (uri.host != "backup-wallet") {
                return null
            }
            val name = uri.getQueryParameter("name").takeIf { !it.isNullOrEmpty() } ?: return null

            val entropy = uri.getQueryParameter("entropy").takeUnless {
                it.isNullOrEmpty() || kotlin.runCatching { it.hexToBytes() }.isFailure
            } ?: return null

            val language =
                uri.getQueryParameter("language").takeUnless { it != "zh-Hans" && it != "en" }
                    ?: return null

            val password =
                uri.getQueryParameter("password").takeUnless { it.isNullOrEmpty() } ?: return null

            return BackupWalletAction(
                name = name,
                entropy = entropy,
                language = language,
                password = password
            )
        }
    }

}