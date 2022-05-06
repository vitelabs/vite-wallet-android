package net.vite.wallet.balance.poll

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.eth.EthNet
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.rpc.EthAccountInfo
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object EthAccountInfoPoll {

    var disposable: Disposable? = null

    val latestEthAccountInfo = ConcurrentHashMap<String, EthAccountInfo>()

    fun myLatestEthAccountInfo(): EthAccountInfo? {
        return AccountCenter.currentEthAddress()?.let {
            latestEthAccountInfo[it]
        }
    }

    fun myEthBalanceInWei(): BigDecimal {
        return myLatestEthAccountInfo()?.ethTokenBalance?.get(TokenInfoCenter.EthEthTokenCodes.tokenCode)?.toBigDecimal()
            ?: BigDecimal.ZERO
    }

    private var id = 0
    private val liveDataSet = HashMap<Int, MutableLiveData<EthAccountInfo>>()

    fun register(liveData: MutableLiveData<EthAccountInfo>): Int {
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
        disposable = Observable.fromCallable {
            try {
                val gson = Gson()
                val ethAddr = AccountCenter.currentEthAddress() ?: throw Exception("non eth addr")

                val ecache = try {
                    val cacheText = GlobalKVCache.read("${ethAddr}AccountInfo") ?: ""
                    gson.fromJson<EthAccountInfo>(
                        cacheText,
                        EthAccountInfo::class.java
                    )
                } catch (e: Exception) {
                    null
                }


                ecache?.let {
                    latestEthAccountInfo[ethAddr] = it
                    liveDataSet.values.forEach { livedata ->
                        livedata.postValue(it)
                    }
                }


                val e = ecache ?: EthAccountInfo()
                val tokenAddrSet = HashMap<String, String>()
                AccountCenter.getCurrentAccountTokenManager()?.pinnedTokenInfoList?.forEach {
                    TokenInfoCenter.getTokenInfoInCache(it.tokenCode)?.let { info ->
                        if (info.family() == TokenFamily.ETH) {
                            tokenAddrSet[info.tokenCode ?: ""] = info.tokenAddress ?: ""
                        }
                    }
                }

                tokenAddrSet.entries.forEach {
                    EthNet.getTokenBalance(ethAddr, it.value)?.let { value ->
                        e.ethTokenBalance[it.key] = value
                    }
                }

                if (e.ethTokenBalance.isNotEmpty()) {
                    latestEthAccountInfo[ethAddr] = e
                    GlobalKVCache.store("${ethAddr}AccountInfo" to gson.toJson(e))
                }

                liveDataSet.values.forEach { livedata ->
                    livedata.postValue(e)
                }

            } catch (e: Exception) {
            }
        }.subscribeOn(Schedulers.io())
            .repeatWhen { it.delay(30, TimeUnit.SECONDS) }
            .retryWhen { it.delay(30, TimeUnit.SECONDS) }
            .subscribe({}, {})
    }

    fun stop() {
        disposable?.dispose()
    }
}