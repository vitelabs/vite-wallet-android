package net.vite.wallet.balance.vote

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.rpc.AsyncNet
import net.vite.wallet.network.rpc.CandidateItem
import net.vite.wallet.network.rpc.VoteInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


object CandidateListPoll {
    private var disposable: Disposable? = null
    var lastCandidateList: List<CandidateItem> = ArrayList()
        set(value) {
            value.forEachIndexed { index, _ ->
                value[index].rank = index + 1
            }
            field = value
        }


    private var id = 0
    private val liveDataSet = HashMap<Int, MutableLiveData<List<CandidateItem>>>()

    fun register(liveData: MutableLiveData<List<CandidateItem>>): Int {
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

    @SuppressLint("CheckResult")
    fun getCandidateList(refreshCandidateListState: MutableLiveData<NetworkState>): LiveData<List<CandidateItem>> {
        val ld = MutableLiveData<List<CandidateItem>>()
        refreshCandidateListState.postValue(NetworkState.LOADING)
        AsyncNet.getCandidateList().subscribeOn(Schedulers.io()).subscribe(
            {
                it.throwable?.let { throwable ->
                    refreshCandidateListState.postValue(NetworkState.error(throwable))
                    return@subscribe
                }

                it.resp?.let { list ->
                    lastCandidateList = list
                    ld.postValue(lastCandidateList)
                    refreshCandidateListState.postValue(NetworkState.LOADED)
                    return@subscribe
                }

                refreshCandidateListState.postValue(NetworkState.LOADED)
            }, {
                refreshCandidateListState.postValue(NetworkState.error(it))
            }
        )
        return ld
    }

    private fun start() {
        disposable = AsyncNet.getCandidateList()
            .subscribeOn(Schedulers.io())
            .repeatWhen {
                it.delay(30, TimeUnit.SECONDS)
            }
            .retryWhen { it.delay(15, TimeUnit.SECONDS) }
            .subscribe({
                if (it.success()) {
                    it.resp?.let { list ->
                        lastCandidateList = list
                        liveDataSet.forEach {
                            it.value.postValue(lastCandidateList)
                        }
                    }
                }
            }, {
            })

    }

    private fun stop() {
        disposable?.dispose()
    }
}


object VoteInfoPoll {
    private var disposable: Disposable? = null
    var lastVoteInfo = ConcurrentHashMap<String, VoteInfo>()

    private var id = 0
    private val liveDataSet = HashMap<Int, MutableLiveData<VoteInfo>>()

    fun register(liveData: MutableLiveData<VoteInfo>): Int {
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

    private fun start() {
        disposable = AsyncNet.getVoteInfo(AccountCenter.currentViteAddress()).subscribeOn(Schedulers.io())
            .repeatWhen {
                it.delay(5, TimeUnit.SECONDS)
            }
            .retryWhen { it.delay(5, TimeUnit.SECONDS) }
            .subscribe({ resp ->
                if (resp.success()) {
                    AccountCenter.currentViteAddress()?.let { addr ->
                        resp?.resp?.let { voteInfo ->
                            lastVoteInfo[addr] = voteInfo
                        }
                    }
                    liveDataSet.forEach {
                        it.value.postValue(resp.resp)
                    }
                }
            }, {
            })

    }

    private fun stop() {
        disposable?.dispose()
    }
}