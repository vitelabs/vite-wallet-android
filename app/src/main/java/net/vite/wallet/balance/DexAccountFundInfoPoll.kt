package net.vite.wallet.balance

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.rpc.GetAccountFundInfoResp
import net.vite.wallet.network.rpc.SyncNet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DexAccountFundInfoPoll {


    private val gson = Gson()
    var disposable: Disposable? = null
    private val latestDexAccountFundInfo =
        ConcurrentHashMap<String, Map<String, GetAccountFundInfoResp>>()

    fun myLatestViteAccountFundInfo(): Map<String, GetAccountFundInfoResp>? {
        return AccountCenter.currentViteAddress()?.let {
            latestDexAccountFundInfo[it]
        }
    }

    private var id = 0
    private val liveDataSet = HashMap<Int, MutableLiveData<Map<String, GetAccountFundInfoResp>>>()

    fun register(liveData: MutableLiveData<Map<String, GetAccountFundInfoResp>>): Int {
        id++
        liveDataSet[id] = liveData
        if (liveDataSet.size == 1) {
            start()
        }
        return id
    }

    fun unregister(id: Int) {
        liveDataSet.remove(id)
        if (liveDataSet.size == 0) {
            stop()
        }
    }

    fun start() {
        disposable?.dispose()
        disposable = Observable.fromCallable {
            try {
                val viteAddr =
                    AccountCenter.currentViteAddress() ?: throw Exception("none vite addr")

                val cacheDexAccountFundInfo = try {
                    gson.fromJson<Map<String, GetAccountFundInfoResp>>(
                        GlobalKVCache.read("${viteAddr}DexAccountFundInfo"),
                        object : TypeToken<Map<String, GetAccountFundInfoResp>>() {}.type
                    )
                } catch (e: Exception) {
                    null
                }

                cacheDexAccountFundInfo?.let {
                    latestDexAccountFundInfo[viteAddr] = it
                    liveDataSet.values.forEach { livedata ->
                        livedata.postValue(it)
                    }
                }

                val accountFundInfo =
                    SyncNet.getAccountFundInfo(viteAddr, "").resp ?: return@fromCallable

                latestDexAccountFundInfo[viteAddr] = accountFundInfo
                GlobalKVCache.store("${viteAddr}DexAccountFundInfo" to gson.toJson(accountFundInfo))

                liveDataSet.values.forEach { livedata ->
                    livedata.postValue(accountFundInfo)
                }

            } catch (e: Exception) {

            }
        }.subscribeOn(Schedulers.io())
            .repeatWhen { it.delay(5, TimeUnit.SECONDS) }
            .retryWhen { it.delay(5, TimeUnit.SECONDS) }
            .subscribe({}, {})
    }

    fun stop() {
        disposable?.dispose()
        disposable = null
    }
}