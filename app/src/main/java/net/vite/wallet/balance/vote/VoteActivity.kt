package net.vite.wallet.balance.vote

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_vote.*
import net.vite.wallet.PowProfile
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.constants.BlockDetailTypeVote
import net.vite.wallet.constants.BlockTypeToContactAddress
import net.vite.wallet.dialog.*
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.rpc.*
import net.vite.wallet.security.IdentityVerify
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.showToast
import java.math.BigInteger
import java.util.*

class VoteActivity : UnchangableAccountAwareActivity() {

    var powProfile: PowProfile? = null

    companion object {
        // vote fsm status
        const val VoteStatusNoVote = 1
        const val VoteStatusVoting = 2
        const val VoteStatusVoteReqSuccess = 3
        const val VoteStatusHasVoted = 4
        const val VoteStatusCancelVoting = 5
        const val VoteStatusCancelVoteReqSuccess = 6


        const val VoteReqCode = 10000
        const val CancelVoteReqCode = 10001
    }


    private var nowVoteStatus = 0
        set(value) {
            when (value) {
                VoteStatusNoVote -> {
                    emptyVoteImg.visibility = View.VISIBLE
                    emptyVoteTxt.visibility = View.VISIBLE
                    groupHasVote.visibility = View.GONE
                    info4SbpStatus.visibility = View.GONE
                }
                VoteStatusVoting -> {

                }
                VoteStatusVoteReqSuccess -> {
                    voteProgress.text = getString(R.string.voteing)
                    voteProgress.setBackgroundResource(R.drawable.voting)
                    recallBtn.setBackgroundResource(R.drawable.grey_rect_h22_w57)
                    recallBtn.isClickable = false
                    emptyVoteImg.visibility = View.GONE
                    emptyVoteTxt.visibility = View.GONE
                    groupHasVote.visibility = View.VISIBLE
                    info4SbpStatus.visibility = View.GONE
                    myVoteAmount.text = ViteAccountInfoPoll.myLatestViteAccountInfo()?.getBalanceReadableText(
                        4,
                        ViteTokenInfo.tokenId!!
                    ) ?: "0.0"
                    sbpStatus.text = getString(R.string.candidate_valid)
                    sbpName.text = voteName

                    powProfile?.let {
                        showPowProfileDialog(it)
                    } ?: run {
                        LogoTitleNotifyDialog(this).apply {
                            enableTopImage(true)
                            setMessage(getString(R.string.vote_req_send_success))
                            setCancelable(false)
                            setBottom(getString(R.string.confirm))
                            show()
                        }
                    }
                }
                VoteStatusHasVoted -> {
                    updateVoteInfo(lastVoteInfo)
                }
                VoteStatusCancelVoting -> {
                }
                VoteStatusCancelVoteReqSuccess -> {
                    voteProgress.text = getString(R.string.recalling_vote)
                    voteProgress.setBackgroundResource(R.drawable.voting)

                    recallBtn.setBackgroundResource(R.drawable.grey_rect_h22_w57)
                    recallBtn.isClickable = false
                    powProfile?.let {
                        showPowProfileDialog(it)
                    } ?: run {
                        LogoTitleNotifyDialog(this).apply {
                            enableTopImage(true)
                            setMessage(getString(R.string.recall_vote_req_send_success))
                            setCancelable(false)
                            setBottom(getString(R.string.confirm))
                            show()
                        }
                    }

                }
                else -> {

                }
            }
            field = value
        }

    private lateinit var vm: VoteVM

    private var filterPattern = ""
    private val adapter = CandidateListAdapter(emptyList())
    private var lastCandidateItems: List<CandidateItem> = ArrayList()
    private var lastVoteInfo: VoteInfo? = null
    private var voteTxParams: NormalTxParams? = null
    private var voteName = ""
    private var cancelVoteTxParams: NormalTxParams? = null


    private var powProgressDialog: PowProgressDialog? = null

    private fun filterList(list: List<CandidateItem>): List<CandidateItem> {
        if (filterPattern == "") {
            return list
        }
        return list.filterIndexed { _, candidateItem ->
            candidateItem.name.contains(filterPattern, true) ||
                    candidateItem.nodeAddr.contains(filterPattern, true)
        }
    }

    fun showPowProfileDialog(powProfile: PowProfile) {
        with(PowProfileDialog(this)) {
            setPowProfile(
                (powProfile.timeCost() / 1000).toString(),
                powProfile.calcPowDifficultyResp?.utRequired ?: "--"
            )
            show()
        }
    }

    fun showPowDialog(quotaCostTxt: String) {
        powProgressDialog = PowProgressDialog(this).apply {
            onCancelPowCb = {
                vm.cancelPow()
                dismiss()
            }
            show(quotaCostTxt)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vote)

        info4SbpStatus.setOnClickListener {
            startActivity(createBrowserIntent("https://app.vite.net/voteLoser/?localize=en}"))
        }
        voteInfo.setOnClickListener {
            startActivity(createBrowserIntent("https://app.vite.net/vote/?localize=en}"))
        }

        vm = ViewModelProviders.of(this)[VoteVM::class.java]



        searchBoxBg.setOnClickListener {
            filterEditText.requestFocus()
        }

        filterEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPattern = s?.toString() ?: ""
                adapter.candidateList = filterList(lastCandidateItems)
                adapter.notifyDataSetChanged()
            }
        })

        swipeRefresh.setOnRefreshListener {
            vm.refreshCandidateList()
        }

        recallBtn.setOnClickListener {
            showRecallVotePasswordDialog(lastVoteInfo?.name ?: "")
        }

        candidateSbpList.adapter = adapter

        adapter.voteFunc = { name ->
            lastVoteInfo?.let { lastVoteInfo ->
                if (lastVoteInfo.name == name) {
                    showToast(R.string.vote_to_same_sbp)
                    return@let
                }
                if (lastVoteInfo.name != name) {
                    BigDialog(this).apply {
                        setBigTitle(getString(R.string.vote))
                        setSecondValue(getString(R.string.make_sure_replace_vote_node, lastVoteInfo.name))
                        setCancelable(false)
                        setFirstButton(getString(R.string.confirm)) {
                            dismiss()
                            showVotePasswordDialog(name)
                        }
                        show()
                    }
                }
            } ?: run {
                showVotePasswordDialog(name)
            }
        }

        vm.refreshCandidateListState.observe(this, Observer {
            swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })

        vm.txSendLiveData.observe(this, Observer {
            swipeRefresh.isRefreshing = it.isLoading()
            if (it.isLoading()) {
                return@Observer
            }
            if (it.isSuccess()) {
                nowVoteStatus = if (it.requestCode == VoteReqCode) {
                    VoteStatusVoteReqSuccess
                } else {
                    VoteStatusCancelVoteReqSuccess
                }
                return@Observer
            }
            val throwable = it.error()

            if (throwable is CalcPowDifficultyRespException) {
                if (throwable.calcPowDifficultyResp.isCongestion == true) {
                    with(TextTitleNotifyDialog(this)) {
                        setTitle(R.string.notice)
                        setMessage(R.string.quota_is_congestion)
                        if (throwable.calcPowDifficultyResp.difficulty.isNullOrEmpty()) {
                            setBottomLeft(R.string.tx_congestion) { dialog ->
                                if (it.requestCode == VoteReqCode) {
                                    voteTxParams?.let { voteTxParams ->
                                        voteTxParams.forceSend = true
                                        vm.sendTx(voteTxParams, it.requestCode!!)
                                    }
                                } else if (it.requestCode == CancelVoteReqCode) {
                                    cancelVoteTxParams?.let { cancelVoteTxParams ->
                                        cancelVoteTxParams.forceSend = true
                                        vm.sendTx(cancelVoteTxParams, it.requestCode!!)
                                    }
                                }
                                dialog.dismiss()
                            }
                        }
                        setBottom(R.string.tx_later) { dialog ->
                            dialog.dismiss()
                        }
                        show()
                    }
                    return@Observer
                }
                if (!throwable.calcPowDifficultyResp.difficulty.isNullOrEmpty()) {
                    powProfile = PowProfile.start(throwable.calcPowDifficultyResp)
                    showPowDialog(throwable.calcPowDifficultyResp.utRequired ?: "--")
                    vm.getPowNonce(
                        GetPowNonceReq(
                            difficulty = throwable.calcPowDifficultyResp.difficulty,
                            preHash = throwable.calcPowDifficultyReq.prevHash,
                            addr = throwable.calcPowDifficultyReq.selfAddr
                        ),
                        it.requestCode ?: 0
                    )
                    return@Observer
                }
            }

            if (throwable is RpcException && throwable.code == -36001) {
                showToast(R.string.need_reveive_a_tx_before_vote)
                return@Observer
            }

            throwable?.showToast(this)

        })

        vm.powNonceLd.observe(this, Observer {
            if (it.isLoading()) {
                if (powProgressDialog?.isShowing != true) {
                    showPowDialog("--")
                }
                return@Observer
            } else {
                powProgressDialog?.dismiss()
                powProgressDialog = null
            }


            it.error()?.let { throwable ->
                powProfile = null
                throwable.showToast(this)
                return@Observer
            }

            powProfile?.end()
            if (it.requestCode == VoteReqCode) {
                voteTxParams?.let { voteTxParams ->
                    voteTxParams.nonce = it.resp?.nonce!!
                    voteTxParams.difficulty = it.resp?.req?.difficulty!!
                    vm.sendTx(voteTxParams, it.requestCode!!)
                }
            } else if (it.requestCode == CancelVoteReqCode) {
                cancelVoteTxParams?.let { cancelVoteTxParams ->
                    cancelVoteTxParams.nonce = it.resp?.nonce!!
                    cancelVoteTxParams.difficulty = it.resp?.req?.difficulty!!
                    vm.sendTx(cancelVoteTxParams, it.requestCode!!)
                }
            }
        })



        vm.refreshCandidateListResult.observe(this, Observer {
            lastCandidateItems = it
            adapter.candidateList = filterList(it)
            adapter.notifyDataSetChanged()
        })

        vm.candidateListPollResult.observe(this, Observer {
            lastCandidateItems = it
            adapter.candidateList = filterList(it)
            adapter.notifyDataSetChanged()
        })

        vm.voteInfoPollLiveData.observe(this, Observer {
            lastVoteInfo = it
            if (it != null) {
                if (it.isInvalid()) {
                    return@Observer
                }
                if (nowVoteStatus == VoteStatusCancelVoteReqSuccess) {
                    // do nothing because this is the voted to no vote
                    return@Observer
                }

                if (nowVoteStatus == VoteStatusVoteReqSuccess && it.name != voteName) {
                    // represent the transition state of vote A to vote B
                    return@Observer
                }

                nowVoteStatus = VoteStatusHasVoted
            } else {
                if (nowVoteStatus == VoteStatusVoteReqSuccess) {
                    // // represent the transition state of no vote to voted
                } else {
                    nowVoteStatus = VoteStatusNoVote
                }
            }
        })
    }


    private fun updateVoteInfo(voteInfo: VoteInfo?) {
        voteInfo?.let { voteInfo ->
            voteProgress.text = getString(R.string.vote_success)
            voteProgress.setBackgroundResource(R.drawable.vote_success)
            recallBtn.setBackgroundResource(R.drawable.deep_blue_rect_h22_w57)
            recallBtn.isClickable = true

            emptyVoteImg.visibility = View.GONE
            emptyVoteTxt.visibility = View.GONE
            groupHasVote.visibility = View.VISIBLE
            myVoteAmount.text = ViteTokenInfo.amountText(voteInfo.balance, 4)
            sbpStatus.text = getString(
                when (voteInfo.nodeStatus) {
                    1 -> R.string.candidate_valid.also {
                        info4SbpStatus.visibility = View.GONE
                    }
                    2 -> R.string.candidate_invalid.also {
                        info4SbpStatus.visibility = View.VISIBLE

                        voteProgress.text = getString(R.string.vote_invalid)
                        voteProgress.setBackgroundResource(R.drawable.vote_invalid)

                        recallBtn.setBackgroundResource(R.drawable.deep_blue_rect_h22_w57)
                        recallBtn.isEnabled = true
                    }
                    else -> {
                        R.string.unknown
                    }
                }
            )
            sbpName.text = voteInfo.name
        }
    }

    private fun showVotePasswordDialog(name: String) {
        IdentityVerify(this).verifyIdentity(
            BigDialog.Params(
                bigTitle = getString(R.string.vote),
                secondTitle = getString(R.string.node_name_tag),
                secondValue = name,
                firstButtonTxt = getString(R.string.confirm),
                quotaCost = "4"
            ),
            {
                voteTxParams = NormalTxParams(
                    accountAddr = currentAccount.nowViteAddress()!!,
                    toAddr = BlockTypeToContactAddress[BlockDetailTypeVote]!!,
                    tokenId = ViteTokenInfo.tokenId!!,
                    amountInSu = BigInteger.ZERO,
                    data = BuildInContractEncoder.encodeVote(name)
                )
                voteName = name
                vm.sendTx(voteTxParams!!, VoteReqCode)
            },
            {}
        )
    }


    private fun showRecallVotePasswordDialog(name: String) {
        val dialogParams = BigDialog.Params(
            bigTitle = getString(R.string.recall_vote),
            secondTitle = getString(R.string.node_name_tag),
            secondValue = name,
            firstButtonTxt = getString(R.string.confirm),
            quotaCost = "2.5"
        )
        IdentityVerify(this).verifyIdentity(
            dialogParams, {
                cancelVoteTxParams = NormalTxParams(
                    accountAddr = currentAccount.nowViteAddress()!!,
                    toAddr = BlockTypeToContactAddress[BlockDetailTypeVote]!!,
                    tokenId = ViteTokenInfo.tokenId!!,
                    amountInSu = BigInteger.ZERO,
                    data = BuildInContractEncoder.encodeCancelVote()
                )
                vm.sendTx(cancelVoteTxParams!!, CancelVoteReqCode)
            },
            {})
    }

}