package net.vite.wallet.balance.poll

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.http.MyRPC
import net.vite.wallet.network.http.vitex.VitexApi
import net.vite.wallet.network.rpc.AsyncNet
import net.vite.wallet.network.rpc.SyncNet
import net.vite.wallet.network.rpc.ViteAccountInfo
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ViteAccountInfoPoll {

    var disposable: Disposable? = null
    val latestViteAccountInfo = ConcurrentHashMap<String, ViteAccountInfo>()

    fun myLatestViteAccountInfo(): ViteAccountInfo? {
        return AccountCenter.currentViteAddress()?.let {
            latestViteAccountInfo[it]
        }
    }

    fun updateViteAccountInfo(addr: String, update: ViteAccountInfo.() -> ViteAccountInfo) {
        latestViteAccountInfo[addr]?.let { origin ->
            origin.update()
        }?.let { new ->
            storeCache(addr, new)
        }
    }

    @SuppressLint("CheckResult")
    private fun storeCache(addr: String, info: ViteAccountInfo) {
        latestViteAccountInfo[addr] = info
        Observable.fromCallable {
            kotlin.runCatching {
                GlobalKVCache.store("${addr}AccountInfo" to Gson().toJson(info))
            }
        }.subscribeOn(Schedulers.single()).subscribe { }

    }

    private var id = 0
    private val liveDataSet = HashMap<Int, MutableLiveData<ViteAccountInfo>>()

    fun register(liveData: MutableLiveData<ViteAccountInfo>): Int {
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
                val viteAddr =
                    AccountCenter.currentViteAddress() ?: throw Exception("none vite addr")
                val gson = Gson()
                val cacheViteInfo = try {
                    gson.fromJson(
                        GlobalKVCache.read("${viteAddr}AccountInfo") ?: "",
                        ViteAccountInfo::class.java
                    )
                } catch (e: Exception) {
                    null
                }

                cacheViteInfo?.let {
                    latestViteAccountInfo[viteAddr] = it
                    liveDataSet.values.forEach { livedata ->
                        livedata.postValue(it)
                    }
                }

                val holdViteVite = SyncNet.getAccountByAccAddr(viteAddr)
                val onroadVite = SyncNet.getOnroadInfoByAddress(viteAddr)
                if (holdViteVite.resp == null && onroadVite.resp == null) {
                    return@fromCallable
                }

                val vnet = cacheViteInfo?.copy(
                    balance = holdViteVite.resp,
                    onroadBalance = onroadVite.resp
                ) ?: ViteAccountInfo(holdViteVite.resp, onroadVite.resp)

                storeCache(viteAddr, vnet)

                liveDataSet.values.forEach { livedata ->
                    livedata.postValue(vnet)
                }
            } catch (e: Exception) {
            }
        }.subscribeOn(Schedulers.single())
            .repeatWhen { it.delay(5, TimeUnit.SECONDS) }
            .retryWhen { it.delay(5, TimeUnit.SECONDS) }
            .subscribe({}, {})
    }

    fun stop() {
        disposable?.dispose()
    }


    fun getStakeForFullNode(addr: String) =
        VitexApi.getApi().rewardPledgeFullStat(addr).applyIoScheduler().map {
            val pAmount = it.data?.pledgeAmount
            if (pAmount != null) {
                updateViteAccountInfo(addr) {
                    copy(stateToFullNodeAll = pAmount.toBigIntegerOrNull() ?: BigInteger.ZERO)
                }
            }
            it
        }

    fun getStakeForSBP(addr: String) =
        MyRPC.contract_getSBPStakeAll(addr).map {
            if (it.success() && it.resp != null) {
                updateViteAccountInfo(addr) {
                    copy(stakeToSBPAll = it.resp)
                }
            }
            it
        }

    fun dexGetCurrentMiningStakingAmountByAddress(addr: String) =
        MyRPC.dex_getCurrentMiningStakingAmountByAddress(addr).map {
            if (it.success() && it.resp != null) {
                updateViteAccountInfo(addr) {
                    copy(stakeForDexMining = it.resp)
                }
            }
            it
        }

    fun dexGetVIPStakeInfoList(
        address: String,
        pageIndex: Long,
        pageSize: Long
    ) = AsyncNet.dexGetVIPStakeInfoList(address, pageIndex, pageSize).map {
        if (it.success()) {
            updateViteAccountInfo(address) {
                copy(
                    stakeForDexVip = it.resp?.totalStakeAmount?.toBigIntegerOrNull()
                        ?: BigInteger.ZERO
                )
            }
        }
        it
    }.applyIoScheduler()


}