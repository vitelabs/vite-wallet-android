package net.vite.wallet.balance.tokenselect

import android.annotation.SuppressLint
import android.util.LruCache
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.ViewObject
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.VitexApi

@SuppressLint("CheckResult")
class TokenSearchVM : ViewModel() {

    companion object {
        var lastRefreshAllInfoTime = 0L
    }

    val tokenIdQueryLd = MutableLiveData<ViewObject<NormalTokenInfo>>()
    fun queryByTokenId(tokenId: String, requestCode: Int = 0) {
        tokenIdQueryLd.postValue(ViewObject.Loading(requestCode))
        TokenInfoCenter.queryViteToken(tokenId).subscribe({
            tokenIdQueryLd.postValue(ViewObject.Loaded(it, requestCode))
        }, {
            tokenIdQueryLd.postValue(ViewObject.Error(it, requestCode))
        })
    }

    val fuzzyQueryLd = MutableLiveData<ViewObject<List<NormalTokenInfo>>>()
    fun fuzzyQuery(s: String, requestCode: Int = 0) {
        fuzzyQueryLd.postValue(ViewObject.Loading(requestCode))
        TokenSearchRepo.fuzzyQuery(s).subscribe({
            fuzzyQueryLd.postValue(ViewObject.Loaded(it, requestCode))
        }, {
            fuzzyQueryLd.postValue(ViewObject.Error(it, requestCode))
        })
    }

    val refreshCompleteLd = MutableLiveData<ViewObject<List<NormalTokenInfo>>>()
    fun refreshAllInfo() {
        if (System.currentTimeMillis() - lastRefreshAllInfoTime > 5 * 60 * 1000) {
            refreshCompleteLd.postValue(ViewObject.Loading())
            Observable.zip(TokenInfoCenter.refreshDefaultTokenInfo(),
                TokenInfoCenter.batchQueryTokenDetail(TokenInfoCenter.getUserAddTokenInfoCodes()),
                { t1, t2 ->
                    TokenInfoCenter.getAllOrderedTokenInfos()
                }
            ).subscribe({
                lastRefreshAllInfoTime = System.currentTimeMillis()
                refreshCompleteLd.postValue(ViewObject.Loaded(it))
            }, {
                refreshCompleteLd.postValue(ViewObject.Error(it))
            })
        }
    }
}

object TokenSearchRepo {

    private val memoryCache = object : LruCache<String, List<NormalTokenInfo>>(400) {
        override fun sizeOf(key: String?, value: List<NormalTokenInfo>?): Int {
            return value?.size ?: 0
        }
    }

    fun fuzzyQuery(s: String): Observable<List<NormalTokenInfo>> {
        val cache = memoryCache.get(s)
        return if (cache != null) {
            Observable.just(cache)
        } else {
            VitexApi.getApi().fuzzyQuery(s).subscribeOn(Schedulers.io())
                .flatMap {
                    val list = ArrayList<NormalTokenInfo>()
                    it.data?.let { map ->
                        map.values.forEach { u ->
                            list.addAll(u)
                        }
                        memoryCache.put(s, list)
                    }
                    Observable.just(list)
                }
        }
    }
}