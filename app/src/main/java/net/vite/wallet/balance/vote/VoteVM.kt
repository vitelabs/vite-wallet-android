package net.vite.wallet.balance.vote

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import net.vite.wallet.TxSendViewModel
import net.vite.wallet.balance.vote.CandidateListPoll.getCandidateList
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.rpc.CandidateItem
import net.vite.wallet.network.rpc.VoteInfo

class VoteVM : TxSendViewModel() {

    private val refreshCandidateList = MutableLiveData<Int>()

    val refreshCandidateListState = MutableLiveData<NetworkState>()
    val voteInfoPollLiveData = VoteInfoPollLiveData()


    val refreshCandidateListResult = Transformations.switchMap(refreshCandidateList) {
        getCandidateList(refreshCandidateListState)
    }

    val candidateListPollResult = CandidateListPollLiveData()

    fun refreshCandidateList() {
        refreshCandidateList.value = 1
    }


}

class CandidateListPollLiveData : MutableLiveData<List<CandidateItem>>() {
    var id: Int? = null
    override fun onActive() {
        id = CandidateListPoll.register(this)
    }

    override fun onInactive() {
        id?.let {
            CandidateListPoll.unregister(it)
        }
    }
}


class VoteInfoPollLiveData : MutableLiveData<VoteInfo>() {
    var id: Int? = null
    override fun onActive() {
        id = VoteInfoPoll.register(this)
    }

    override fun onInactive() {
        id?.let {
            VoteInfoPoll.unregister(it)
        }
    }
}