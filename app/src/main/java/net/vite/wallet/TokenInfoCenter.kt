package net.vite.wallet

import android.annotation.SuppressLint
import android.util.ArrayMap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.exchange.TickerStatisticsCenter
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.http.vitex.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

typealias onNewTokenInfosArrived = (infos: List<NormalTokenInfo>) -> Unit

object TokenInfoCenter {

    lateinit var ViteViteTokenCodes: PinnedTokenInfo
    lateinit var EthViteTokenCodes: PinnedTokenInfo
    lateinit var EthEthTokenCodes: PinnedTokenInfo
    lateinit var ViteEthTokenCodes: PinnedTokenInfo
    lateinit var ViteUSDTTokenCodes: PinnedTokenInfo
    lateinit var GrinTokenCodes: PinnedTokenInfo

    private val queryViteTokenCache = ConcurrentHashMap<String, NormalTokenInfo>()

    @Volatile
    private var defaultTokenInfoList = ArrayList<NormalTokenInfo>()
    fun getDefaultTokenInfoList(): ArrayList<NormalTokenInfo> {
        return defaultTokenInfoList
    }

    @SuppressLint("CheckResult")
    private fun storeNewDefaultCache() {
        Observable.fromCallable {
            GlobalKVCache.store("lastDefaultTokenInfos" to Gson().toJson(defaultTokenInfoList))
        }.subscribeOn(Schedulers.single()).subscribe({}, {})
    }

    @SuppressLint("CheckResult")
    private fun storeNewUserAddCache() {
        Observable.fromCallable {
            GlobalKVCache.store("lastUserAddTokenInfos" to Gson().toJson(userAddTokenInfoList))
        }.subscribeOn(Schedulers.single()).subscribe({}, {})
    }

    @Volatile
    private var dexTokens = ArrayList<NormalTokenInfo>()

    fun getDexTokens() = dexTokens

    @SuppressLint("CheckResult")
    private fun storeDexTokensCache() {
        Observable.fromCallable {
            GlobalKVCache.store("storeVitexTokensCache" to Gson().toJson(dexTokens))
        }.subscribeOn(Schedulers.single()).subscribe({}, {})
    }


    @Volatile
    private var userAddTokenInfoList = ArrayList<NormalTokenInfo>()
    fun getUserAddTokenInfoCodes(): ArrayList<String> {
        return userAddTokenInfoList.fold(ArrayList()) { acc, normalTokenInfo ->
            acc.add(normalTokenInfo.tokenCode ?: "")
            acc
        }
    }

    fun getAllOrderedTokenInfos(): ArrayList<NormalTokenInfo> {
        val tokenPlatformMap = ArrayMap<Int, ArrayList<NormalTokenInfo>>()
        val f: (NormalTokenInfo) -> Unit = { info ->
            val l = tokenPlatformMap[info.family()]
            if (l != null) {
                if (l.find { it.tokenCode == info.tokenCode } == null) {
                    l.add(info)
                }
            } else {
                tokenPlatformMap[info.family()] = arrayListOf(info)
            }
        }

        defaultTokenInfoList.forEach { f.invoke(it) }
        userAddTokenInfoList.forEach { f.invoke(it) }

        val al = ArrayList<NormalTokenInfo>()
        tokenPlatformMap.values.forEach {
            al.addAll(it)
        }
        return al
    }


    fun getEthEthTokenInfo() = findTokenInfo { it.tokenCode == EthEthTokenCodes.tokenCode }

    fun findTokenInfo(predicate: (NormalTokenInfo) -> Boolean): NormalTokenInfo? {
        for (i in 0 until defaultTokenInfoList.size) {
            val token = defaultTokenInfoList[i]
            if (predicate.invoke(token)) {
                return token
            }
            val mappedToken = token.gatewayInfo?.mappedToken ?: continue
            if (predicate.invoke(mappedToken)) {
                return mappedToken
            }
        }

        for (i in 0 until userAddTokenInfoList.size) {
            val token = userAddTokenInfoList[i]
            if (predicate.invoke(token)) {
                return token
            }
            val mappedToken = token.gatewayInfo?.mappedToken ?: continue
            if (predicate.invoke(mappedToken)) {
                return mappedToken
            }
        }

        for (i in 0 until dexTokens.size) {
            val token = dexTokens[i]
            if (predicate.invoke(token)) {
                return token
            }
            val mappedToken = token.gatewayInfo?.mappedToken ?: continue
            if (predicate.invoke(mappedToken)) {
                return mappedToken
            }
        }

        return null
    }

    fun getTokenInfoInCache(tokenCode: String) = findTokenInfo { it.tokenCode == tokenCode }

    fun getTokenInfoIncacheByTokenAddr(tokenAddress: String): NormalTokenInfo? {
        return findTokenInfo { it.tokenAddressEqual(tokenAddress) }
    }

    fun loadDefaultAndUserAddTokenInfoCache(): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            try {
                loadDefaultTokenInfoCache()
                loadUserAddTokenInfoCache()
                loadDexTokensCache()
                loadViteTokenCache()
            } catch (e: Exception) {
            }
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    private fun loadDexTokensCache() {
        val a = ArrayList<NormalTokenInfo>()
        GlobalKVCache.read("storeVitexTokensCache")?.let {
            try {
                val type = object : TypeToken<List<NormalTokenInfo>>() {}.type
                val userAddList = Gson().fromJson<List<NormalTokenInfo>>(it, type)
                a.addAll(userAddList)
            } catch (e: Exception) {
                GlobalKVCache.store("storeVitexTokensCache" to "")
            }
        }
        dexTokens = a
    }

    private fun loadUserAddTokenInfoCache() {
        val a = ArrayList<NormalTokenInfo>()
        GlobalKVCache.read("lastUserAddTokenInfos")?.let {
            try {
                val type = object : TypeToken<List<NormalTokenInfo>>() {}.type
                val userAddList = Gson().fromJson<List<NormalTokenInfo>>(it, type)
                a.addAll(userAddList)
            } catch (e: Exception) {
                GlobalKVCache.store("lastUserAddTokenInfos" to "")
            }
        }
        userAddTokenInfoList = a
    }

    private fun loadDefaultTokenInfoCache() {
        val a = ArrayList<NormalTokenInfo>()
        GlobalKVCache.read("lastDefaultTokenInfos")?.let { cache ->
            try {
                val type = object : TypeToken<List<NormalTokenInfo>>() {}.type
                val defaultInfo = Gson().fromJson<List<NormalTokenInfo>>(cache, type)
                a.addAll(defaultInfo)
            } catch (e: Exception) {
                GlobalKVCache.store("lastDefaultTokenInfos" to "")
            }
        }
        defaultTokenInfoList = a
    }

    private fun loadViteTokenCache() {
        GlobalKVCache.read("TokenCenterLastUserViteTokenCache")?.let { cache ->
            try {
                val type = object : TypeToken<Map<String, NormalTokenInfo>>() {}.type
                val defaultInfo = Gson().fromJson<Map<String, NormalTokenInfo>>(cache, type)
                queryViteTokenCache.putAll(defaultInfo)
            } catch (e: Exception) {
                GlobalKVCache.store("TokenCenterLastUserViteTokenCache" to "")
            }
        }
    }


    val pollViteXTokensListeners = ArrayList<onNewTokenInfosArrived>()

    var pollViteXTokensDisposable: Disposable? = null
    fun pollViteXTokens() {
        if (pollViteXTokensDisposable != null) {
            return
        }
        pollViteXTokensDisposable =
            VitexApi.getDexTokens()
                .subscribeOn(Schedulers.io())
                .repeatWhen { it.delay(10, TimeUnit.SECONDS) }
                .retryWhen { it.delay(5, TimeUnit.SECONDS) }
                .subscribe({ tokens ->
                    dexTokens = ArrayList<NormalTokenInfo>().apply { addAll(tokens) }
                    dexTokens.forEach {
                        queryViteTokenCache[it.tokenAddress!!] = it
                    }
                    pollViteXTokensListeners.forEach { it.invoke(tokens) }
                    storeViteTokenCache()
                    TickerStatisticsCenter.dexTokensCache = tokens
                    storeDexTokensCache()
                }, {
                    logt("xirtam VitexApi.getDexTokens error ${it.message}")
                    loge(it)
                })
    }

    fun refreshDefaultTokenInfo(): Observable<List<NormalTokenInfo>> {
        return VitexApi.getApi().getDefaultTokenInfos()
            .subscribeOn(Schedulers.io()).flatMap {
                it.data?.let { map ->
                    val list = ArrayList<NormalTokenInfo>()
                    map.values.forEach { u ->
                        list.addAll(u)
                    }
                    defaultTokenInfoList.clear()
                    defaultTokenInfoList.addAll(list)
                    storeNewDefaultCache()

                    val cleanUserAddList = ArrayList<NormalTokenInfo>()
                    userAddTokenInfoList.forEach { userAddInfo ->
                        if (defaultTokenInfoList.find { it.tokenCode == userAddInfo.tokenCode } == null) {
                            cleanUserAddList.add(userAddInfo)
                        }
                    }
                    userAddTokenInfoList.clear()
                    userAddTokenInfoList.addAll(cleanUserAddList)
                    storeNewUserAddCache()

                    Observable.just(list)

                } ?: run {
                    Observable.error<List<NormalTokenInfo>>(it.throwable())
                }
            }
    }

    fun queryViteToken(tti: String): Observable<NormalTokenInfo> {
        val cacheTokenInfo = if (queryViteTokenCache[tti] != null) {
            queryViteTokenCache[tti]
        } else {
            getTokenInfoIncacheByTokenAddr(tti)
        }
        if (cacheTokenInfo != null) {
            return Observable.just(cacheTokenInfo)
        }
        return VitexApi.getApi().queryPlatformToken(QueryPlatformTokensReq.create(tti)).subscribeOn(
            Schedulers.io()
        ).flatMap {
            if (it.data?.size == 1) {
                queryViteTokenCache[tti] = it.data[0]
                storeViteTokenCache()
                Observable.just(it.data[0])
            } else {
                Observable.error(Exception("not found"))
            }
        }
    }

    fun queryTokenDetail(tokenCode: String): Observable<NormalTokenInfo> {
        return if (getTokenInfoInCache(tokenCode) != null) {
            Observable.just(getTokenInfoInCache(tokenCode))
        } else {
            VitexApi.getApi().batchQueryTokenDetail(listOf(tokenCode)).subscribeOn(Schedulers.io())
                .flatMap {
                    if (it.data?.size == 1) {
                        Observable.just(it.data.get(0))
                    } else {
                        Observable.error(Exception("not found tokenCode"))
                    }

                }
        }
    }

    fun batchQueryTokenDetail(tokenCodes: List<String>): Observable<List<NormalTokenInfo>> {
        return VitexApi.getApi().batchQueryTokenDetail(tokenCodes).subscribeOn(
            Schedulers.io()
        ).flatMap {
            it.data?.let { list ->
                try {
                    list.forEach { now ->
                        if (now.platform == "VITE") {
                            queryViteTokenCache[now.tokenAddress!!] = now
                        }
                        val defaultIndex =
                            defaultTokenInfoList.indexOfFirst { it.tokenCode == now.tokenCode }
                        if (defaultIndex != -1) {
                            defaultTokenInfoList[defaultIndex] = now
                            return@forEach
                        }

                        val userAddIndex =
                            userAddTokenInfoList.indexOfLast { it.tokenCode == now.tokenCode }
                        if (userAddIndex == -1) {
                            userAddTokenInfoList.add(now)
                        } else {
                            logt("batchQueryTokenDetail")
                            userAddTokenInfoList[userAddIndex] = now
                        }

                        val dexIndex =
                            dexTokens.indexOfLast { it.tokenCode == now.tokenCode }
                        if (dexIndex != -1) {
                            dexTokens[dexIndex] = now
                        }
                    }
                    storeNewDefaultCache()
                    storeNewUserAddCache()
                    storeViteTokenCache()
                } catch (e: Exception) {
                }

                Observable.just(list)
            }
        }
    }

    private fun storeViteTokenCache() {
        try {
            GlobalKVCache.store(
                "TokenCenterLastUserViteTokenCache" to Gson().toJson(
                    queryViteTokenCache
                )
            )
        } catch (e: Exception) {
        }
    }

    fun addUserSearchToken(normalToken: NormalTokenInfo) {
        if (userAddTokenInfoList.find { it.tokenCode == normalToken.tokenCode } != null) {
            return
        }
        userAddTokenInfoList.add(normalToken)
        storeNewUserAddCache()
    }

    fun rmUserSearchToken(tokenCode: String) {
        val index = userAddTokenInfoList.indexOfFirst { it.tokenCode == tokenCode }
        if (index != -1) {
            userAddTokenInfoList.removeAt(index)
        }
    }

    val permanentPinnedTokenInfos = ArrayList<PinnedTokenInfo>()
    fun isInPermanentTokenCode(tokenCode: String?): Boolean {
        if (tokenCode == null) {
            return false
        }
        val findInPermanent = permanentPinnedTokenInfos.find { it.tokenCode == tokenCode }
        return findInPermanent != null
    }

    fun init() {
        ViteViteTokenCodes = PinnedTokenInfo(
            tokenCode = "1171",
            family = TokenFamily.VITE,
            name = "Vite Token",
            symbol = "VITE"
        )
        EthViteTokenCodes = PinnedTokenInfo(
            tokenCode = "41",
            family = TokenFamily.ETH,
            name = "ViteToken",
            symbol = "VITE"
        )
        EthEthTokenCodes = PinnedTokenInfo(
            tokenCode = "1",
            family = TokenFamily.ETH,
            name = "Ether",
            symbol = "ETH"
        )
        ViteEthTokenCodes = PinnedTokenInfo(
            tokenCode = "1352",
            family = TokenFamily.VITE,
            name = "ETH Token",
            symbol = "ETH"
        )

        ViteUSDTTokenCodes = PinnedTokenInfo(
            tokenCode = "1353",
            family = TokenFamily.VITE,
            name = "Tether USD",
            symbol = "USDT"
        )

        GrinTokenCodes = PinnedTokenInfo(
            tokenCode = "1174",
            family = TokenFamily.GRIN,
            name = "Grin",
            symbol = "GRIN"
        )

        permanentPinnedTokenInfos.clear()

        permanentPinnedTokenInfos.addAll(
            arrayListOf(
                ViteViteTokenCodes,
                ViteEthTokenCodes,
                ViteUSDTTokenCodes,
                EthEthTokenCodes,
                EthViteTokenCodes
            )
        )
    }

}