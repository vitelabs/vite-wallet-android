package net.vite.wallet

object InMemoryCache {

    val cacheMap = HashMap<String, Any>()

    fun put(p: Pair<String, Any>) {
        cacheMap[p.first] = p.second
    }

    fun get(key: String): Any? {
        return cacheMap[key]
    }

}