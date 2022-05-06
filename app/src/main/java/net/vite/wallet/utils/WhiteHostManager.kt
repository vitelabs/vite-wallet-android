package net.vite.wallet.utils

import net.vite.wallet.BuildConfig
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

object WhiteHostManager {
    val viteWhiteHostList = CopyOnWriteArrayList<String>()

    init {
        viteWhiteHostList.apply {
            add("vite.org")
            add("vite.net")
            add("vite.store")
            add("vite.wiki")
            add("vite.blog")
            if (BuildConfig.DEBUG) {
                add("vite-wallet-test2.netlify.com")
            }
        }
    }

    fun isWhiteHost(s: String): Boolean {
        try {
            if (s in viteWhiteHostList) {
                return true
            }
            val url = URL(s)
            if (url.host in viteWhiteHostList) {
                return true
            }

            for (wh in viteWhiteHostList) {
                if (url.host.endsWith(".$wh")) {
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    fun addWhiteUrl(s: String) {
        try {
            val url = URL(s)
            if (viteWhiteHostList.indexOf(url.host) == -1) {
                viteWhiteHostList.add(url.host)
            }
        } catch (e: Exception) {
        }
    }
}