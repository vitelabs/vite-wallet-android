package net.vite.wallet

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.http.vitex.TokenPrice
import net.vite.wallet.network.http.vitex.VitexApi
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ExternalPriceCenter {

    private val tokenPriceMap = ConcurrentHashMap<String, TokenPrice>()

    fun getTokenBtcEquivalentPrice(tokenCode: String): BigDecimal {
        return tokenPriceMap[tokenCode]?.btc ?: BigDecimal.ZERO
    }

    fun getPrice(tokenCode: String): BigDecimal {
        val currency = ViteConfig.get().currentCurrency()
        return tokenPriceMap[tokenCode]?.let {
            when (currency) {
                CURRENCY_RUB -> it.rub
                CURRENCY_KRW -> it.krw
                CURRENCY_TRY -> it.`try`
                CURRENCY_VND -> it.vnd
                CURRENCY_USD -> it.usd
                CURRENCY_CNY -> it.cny
                CURRENCY_EUR -> it.eur
                CURRENCY_GBP -> it.gbp
                else -> BigDecimal.ZERO
            }
        } ?: BigDecimal.ZERO
    }

    fun loadTokenPriceCache(): Observable<List<TokenPrice>> {
        return GlobalKVCache.readAsync("lastQueryPrice").flatMap {
            val list = try {
                val type = object : TypeToken<List<TokenPrice>>() {}.type
                Gson().fromJson<List<TokenPrice>>(it, type)
            } catch (e: Throwable) {
                ArrayList()
            }
            list.forEach { tokenPrice ->
                tokenPrice.tokenCode?.let { tokenCode ->
                    tokenPriceMap[tokenCode] = tokenPrice
                }
            }
            Observable.just(list)
        }
    }

    fun refreshQueryTokenPrice(tokenCodes: List<String>): Observable<List<TokenPrice>> {
        return VitexApi.getApi().batchQueryTokenPrice(tokenCodes).flatMap {
            if (it.data != null) {
                GlobalKVCache.store("lastQueryPrice" to Gson().toJson(it.data))
                it.data.forEach { tokenPrice ->
                    tokenPrice.tokenCode?.let { tokenCode ->
                        tokenPriceMap[tokenCode] = tokenPrice
                    }
                }
                Observable.just(it.data)
            } else {
                Observable.error<List<TokenPrice>>(it.throwable())
            }
        }.subscribeOn(Schedulers.io())
    }

    fun pollQueryTokensPrice(): Observable<List<TokenPrice>> {
        return VitexApi.getApi()
            .batchQueryTokenPrice(TokenInfoCenter.getDexTokens().mapNotNull { it.tokenCode })
            .flatMap {
                if (it.data != null) {
                    GlobalKVCache.store("lastQueryPrice" to Gson().toJson(it.data))
                    it.data.forEach { tokenPrice ->
                        tokenPrice.tokenCode?.let { tokenCode ->
                            tokenPriceMap[tokenCode] = tokenPrice
                        }
                    }
                    Observable.just(it.data)
                } else {
                    Observable.error(it.throwable())
                }
            }.repeatWhen { it.delay(5, TimeUnit.SECONDS) }
            .retryWhen { it.delay(5, TimeUnit.SECONDS) }
            .subscribeOn(Schedulers.io())
    }
}