package net.vite.wallet.network

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.ViteConfig
import java.io.File


class KVCache(val dirname: String) {
    private val absolutePath: String = if (!dirname.isEmpty()) {
        ViteConfig.get().viteCacheDir + "/" + dirname
    } else {
        ViteConfig.get().viteCacheDir
    }

    fun getPath(): String {
        return absolutePath
    }

    fun store(cacheItem: Pair<String, String>) {
        try {
            val f = File(absolutePath)
            f.mkdirs()
            val cacheFile = File("$absolutePath/${cacheItem.first}")

            cacheFile.writeText(cacheItem.second)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun read(cacheKey: String): String? {
        val f = File("$absolutePath/$cacheKey")
        if (f.isFile) {
            try {
                return f.readText()
            } catch (e: Exception) {
            }
        }
        return null
    }


    fun readAsync(cacheKey: String): Observable<String> {
        return Observable.create<String> {
            it.onNext(read(cacheKey) ?: "")
        }.subscribeOn(Schedulers.io())

    }
}

object GlobalKVCache {

    val kvCache by lazy { KVCache("") }
    fun store(cacheItem: Pair<String, String>) {
        kvCache.store(cacheItem)
    }

    fun read(cacheKey: String): String? {
        return kvCache.read(cacheKey)
    }

    fun readAsync(cacheKey: String): Observable<String> {
        return kvCache.readAsync(cacheKey)
    }

    fun getPath(): String {
        return kvCache.getPath()
    }

}