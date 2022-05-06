package net.vite.wallet

import PledgeQuotaOrPowDialog
import android.os.Handler
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.vite.wallet.account.AccountAwareFragment
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.quota.PledgeQuotaActivity
import net.vite.wallet.balance.quota.PowCount
import net.vite.wallet.constants.BlockDetailTypePledge
import net.vite.wallet.constants.BlockTypeToContactAddress
import net.vite.wallet.dialog.*
import net.vite.wallet.network.rpc.*
import net.vite.wallet.security.IdentityVerify
import net.vite.wallet.utils.showToast


open class TxEndStatus(val requestCode: Int = 0)
class TxSendSuccess(
    val accountBlock: AccountBlock,
    val powProfile: PowProfile? = null,
    requestCode: Int
) : TxEndStatus(requestCode)

class TxSendFailed(val whereFailed: Int, val throwable: Throwable, requestCode: Int) :
    TxEndStatus(requestCode) {
    companion object {
        const val POW = 100
        const val TxSend = 101
    }
}

class TxSendCancel(val where: Int, requestCode: Int) : TxEndStatus(requestCode) {
    companion object {
        const val Pow = 1
        const val NetworkCongestion = 3
        const val IdVerifyCancel = 5
    }
}

typealias TxSendFlowStatusListener = (status: TxEndStatus) -> Unit


class PowProfile(
    var powStartTime: Long?,
    var powEndTime: Long?,
    var calcPowDifficultyResp: CalcPowDifficultyResp?
) {


    companion object {
        fun start(calcPowDifficultyResp: CalcPowDifficultyResp): PowProfile {
            return PowProfile(
                powStartTime = System.currentTimeMillis(),
                calcPowDifficultyResp = calcPowDifficultyResp,
                powEndTime = null
            )
        }
    }

    fun end() {
        powEndTime = System.currentTimeMillis()
    }

    fun timeCost(): Long {
        return (powEndTime ?: 0L) - (powStartTime ?: 0L)
    }

}

class BaseTxSendFlow {

    var powProfile: PowProfile? = null
    lateinit var txSendViewModel: TxSendViewModel

    val lifecycleOwner: LifecycleOwner
    val activity: FragmentActivity
    val fragment: Fragment?

    lateinit var cycleProgressBar: ProgressDialog
    private var powProgressDialog: PowProgressDialog? = null
    var identityVerify: IdentityVerify

    var txSendFlowListener: TxSendFlowStatusListener?
    val getSendTx: () -> NormalTxParams?

    constructor(
        fragment: AccountAwareFragment,
        getSendTx: () -> NormalTxParams?,
        txSendFlowListener: TxSendFlowStatusListener?
    ) {
        this.fragment = null
        this.activity = fragment.requireActivity()
        this.getSendTx = getSendTx
        lifecycleOwner = activity
        this.identityVerify = IdentityVerify(fragment)
        this.txSendFlowListener = txSendFlowListener
    }


    constructor(
        activity: UnchangableAccountAwareActivity,
        getSendTx: () -> NormalTxParams?,
        txSendFlowListener: TxSendFlowStatusListener?
    ) {
        this.fragment = null
        this.activity = activity
        this.getSendTx = getSendTx
        lifecycleOwner = activity
        this.identityVerify = IdentityVerify(activity)
        this.txSendFlowListener = txSendFlowListener
    }

    fun onCreate() {
        cycleProgressBar = ProgressDialog(activity)
        if (fragment != null) {
            txSendViewModel = ViewModelProviders.of(fragment)[TxSendViewModel::class.java]
        } else {
            txSendViewModel = ViewModelProviders.of(activity)[TxSendViewModel::class.java]
        }

        txSendViewModel.txSendLiveData.observe(lifecycleOwner, Observer {
            val requestCode = it.requestCode ?: 0
            it.handleDialogShowOrDismiss(cycleProgressBar)
            if (it.isLoading()) {
                return@Observer
            }
            it.resp?.let { ab ->
                onTxEnd(TxSendSuccess(ab, powProfile, requestCode))
                powProfile = null
                return@Observer
            }
            val throwable = it.error() ?: return@Observer
            if (throwable is CalcPowDifficultyRespException) {
                if (throwable.calcPowDifficultyResp.isCongestion == true) {
                    if (throwable.calcPowDifficultyResp.difficulty.isNullOrEmpty()) {
                        onNetworkCongestion(true, requestCode)
                    } else {
                        onNetworkCongestion(false, requestCode)
                    }
                    return@Observer
                }

                if (!throwable.calcPowDifficultyResp.difficulty.isNullOrEmpty()) {
                    if (throwable.calcPowDifficultyReq.toAddr == BlockTypeToContactAddress[BlockDetailTypePledge]) {
                        showPowDialog(
                            throwable.calcPowDifficultyResp.utRequired ?: "--",
                            requestCode
                        )
                        txSendViewModel.getPowNonce(
                            GetPowNonceReq(
                                difficulty = throwable.calcPowDifficultyResp.difficulty,
                                preHash = throwable.calcPowDifficultyReq.prevHash,
                                addr = throwable.calcPowDifficultyReq.selfAddr
                            ),
                            it.requestCode
                        )
                        return@Observer
                    }

                    if (PowCount.isExceedPow(AccountCenter.currentViteAddress()!!)) {
                        PledgeQuotaOrPowDialog(activity).showAsPowExceedLimit {
                            PledgeQuotaActivity.show(
                                activity,
                                AccountCenter.currentViteAddress(),
                                "1000"
                            )
                        }
                        return@Observer
                    }

                    PowCount.usePow(AccountCenter.currentViteAddress()!!)
                    showPowOrPledgeDialog {
                        showPowDialog(
                            throwable.calcPowDifficultyResp.utRequired ?: "--",
                            requestCode
                        )
                        txSendViewModel.getPowNonce(
                            GetPowNonceReq(
                                difficulty = throwable.calcPowDifficultyResp.difficulty,
                                preHash = throwable.calcPowDifficultyReq.prevHash,
                                addr = throwable.calcPowDifficultyReq.selfAddr
                            ),
                            it.requestCode
                        )
                    }
                    return@Observer
                }
            }
            onTxEnd(TxSendFailed(TxSendFailed.TxSend, throwable, requestCode))
        })

        txSendViewModel.powNonceLd.observe(lifecycleOwner, Observer {
            if (it.isLoading()) {
                if (powProgressDialog?.isShowing != true) {
                    showPowDialog("--", it.requestCode ?: 0)
                }
            } else {
                powProgressDialog?.dismiss()
                powProgressDialog = null
            }

            if (it.isLoading()) {
                return@Observer
            }

            it.error()?.let { throwable ->
                powProfile = null
                onTxEnd(TxSendFailed(TxSendFailed.POW, throwable, it.requestCode ?: 0))
                return@Observer
            }
            getSendTx()?.let { last ->
                powProfile?.end()
                last.nonce = it.resp?.nonce
                last.difficulty = it.resp?.req?.difficulty
                txSendViewModel.sendTx(last, it.requestCode ?: 0)
            }
        })
    }

    private fun showPowDialog(utCost: String, requestCode: Int) {
        powProgressDialog = PowProgressDialog(activity).apply {
            onCancelPowCb = {
                txSendViewModel.cancelPow()
                dismiss()
                onTxEnd(TxSendCancel(TxSendCancel.Pow, requestCode))
            }
            show(utCost)
        }
    }

    private fun showPowOrPledgeDialog(runPow: () -> Unit) {
        PledgeQuotaOrPowDialog(activity).show(PowCount.powCount != 0, {
            PledgeQuotaActivity.show(activity, AccountCenter.currentViteAddress(), "1000")
        }, runPow)
    }

    fun onTxEnd(status: TxEndStatus) {
        txSendFlowListener?.invoke(status)
    }

    private fun onNetworkCongestion(quotaEnough: Boolean, requestCode: Int) {
        with(TextTitleNotifyDialog(activity)) {
            setTitle(R.string.notice)
            setMessage(R.string.quota_is_congestion)
            if (quotaEnough) {
                setBottomLeft(R.string.tx_congestion) { dialog ->
                    getSendTx()?.let { last ->
                        last.forceSend = true
                        txSendViewModel.sendTx(last, requestCode)
                    }
                    dialog.dismiss()
                }
            }

            setBottom(R.string.tx_later) { dialog ->
                dialog.dismiss()
                onTxEnd(TxSendCancel(TxSendCancel.NetworkCongestion, requestCode))
            }
            show()
        }
    }


    fun getString(@StringRes id: Int): String {
        return activity.getString(id)
    }

    fun handleTxEndStatus(status: TxEndStatus, onDialogDismiss: (() -> Unit)? = null) {
        when (status) {
            is TxSendSuccess -> {
                status.powProfile?.let {
                    showPowProfileDialog(it, onDialogDismiss)
                } ?: kotlin.run {
                    showBaseTxSendSuccessDialog(onDialogDismiss)
                }
            }
            is TxSendFailed -> status.throwable.showToast(activity)
        }
    }

    fun showPowProfileDialog(powProfile: PowProfile, onDialogDismiss: (() -> Unit)? = null) =
        PowProfileDialog(activity).apply {
            setPowProfile(
                (powProfile.timeCost() / 1000).toString(),
                powProfile.calcPowDifficultyResp?.utRequired ?: "--"
            )
            setDismissListener {
                onDialogDismiss?.invoke()
            }
            show()
        }


    fun showBaseTxSendSuccessDialog(onDialogDismiss: (() -> Unit)? = null) =
        LogoTitleNotifyDialog(activity).apply {
            enableTopImage(true)
            setMessage(getString(R.string.tx_send_success))
            setCancelable(false)
            Handler().postDelayed({
                dismiss()
                onDialogDismiss?.invoke()
            }, 1000)
            show()
        }
}

