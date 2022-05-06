package net.vite.wallet

import org.vitelabs.mobile.Mobile


object Wallet {
    val instance = Mobile.newWallet(ViteConfig.get().viteRootWalletPath, 10, true)!!

    init {
        instance.start()
    }

    fun listAllEntropyFiles(): List<String> {
        logt("listAllEntropyFiles" + instance.listAllEntropyFiles())
        return instance.listAllEntropyFiles().split("\n")
    }

    fun checkPassphrase(ep: String, passphrase: String): Boolean {
        return try {
            instance.unlock(ep, passphrase)
            true
        } catch (e: Exception) {
            false
        }
    }
}