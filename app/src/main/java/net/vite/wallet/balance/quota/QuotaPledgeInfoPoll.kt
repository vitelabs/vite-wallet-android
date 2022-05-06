package net.vite.wallet.balance.quota

import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.loge
import net.vite.wallet.network.rpc.PledgeInfoResp
import net.vite.wallet.network.rpc.SyncNet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object QuotaPledgeInfoPoll {
    var disposable: Disposable? = null
    val latestData = ConcurrentHashMap<String, PledgeInfoResp>()
    fun getLatestData(viteAddress: String): PledgeInfoResp? {
        return latestData[viteAddress]
    }

    private var id = 0
    private val liveDataSet = HashMap<Int, MutableLiveData<PledgeInfoResp>>()

    fun register(liveData: MutableLiveData<PledgeInfoResp>): Int {
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
            SyncNet.getPledgeList(AccountCenter.currentViteAddress()!!, 0, 0)
        }.repeatWhen { it.delay(5, TimeUnit.SECONDS) }
            .retryWhen { it.delay(5, TimeUnit.SECONDS) }
            .subscribeOn(Schedulers.io())
            .subscribe({ resp ->
                if (resp.success()) {
                    resp.resp?.let {
                        latestData[AccountCenter.currentViteAddress() ?: ""] = it
                    }
                    liveDataSet.values.forEach {
                        it.postValue(resp.resp)
                    }
                } else {
                    resp.throwable?.printStackTrace()
                }
            }, { err ->
                loge(err)
            })
    }

    fun stop() {
        disposable?.dispose()
    }
}