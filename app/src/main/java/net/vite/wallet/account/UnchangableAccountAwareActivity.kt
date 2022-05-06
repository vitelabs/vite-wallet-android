package net.vite.wallet.account

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import net.vite.wallet.R
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.balance.walletconnect.ViteConnectActivity
import net.vite.wallet.balance.walletconnect.record.VCSignListActivity
import net.vite.wallet.balance.walletconnect.session.VCFsmHolder
import net.vite.wallet.balance.walletconnect.session.VcRequestTask
import net.vite.wallet.balance.walletconnect.session.fsm.*
import net.vite.wallet.dialog.ProgressDialog
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.utils.showToast
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.vep.ViteBridgeUrlTransferParams
import org.walletconnect.Session

@SuppressLint("Registered")
open class UnchangableAccountAwareActivity : BaseActivity() {

    lateinit var currentAccount: AccountProfile
    private lateinit var cycleProgressBar: ProgressDialog
    private var vCFsmStateEnterCallbackId: Int? = null
    private var postTaskToUserCheckId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentAccount = AccountCenter.getCurrentAccount() ?: run {
            finish()
            return
        }
        cycleProgressBar = ProgressDialog(this)
        init()
    }

    open fun init() {

    }

    override fun onStart() {
        super.onStart()
        vCFsmStateEnterCallbackId = VCFsmHolder.addVCFsmStateEnterCallback(::wcSessionStatusAwareFun)
        postTaskToUserCheckId = VCFsmHolder.signManager?.addPostTaskToUserCheck(::postTaskToUserCheck)
        if (VCFsmHolder.hasExistSession()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onStop() {
        super.onStop()
        vCFsmStateEnterCallbackId?.let {
            VCFsmHolder.rmVCFsmStateEnterCallback(it)
        }
        postTaskToUserCheckId?.let {
            VCFsmHolder.signManager?.rmPostTaskToUserCheck(it)
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    open fun wcSessionStatusAwareFun(state: VCState) {
        runOnUiThread {
            when (state) {
                is StateConnecting -> {
                }
                is StateConnectionLost -> {
                }
                is StateReconnected -> {
                }
                is StateSessionEstablished -> {
                    cycleProgressBar.dismiss()
                    val fromEvent = state.fromEvent ?: return@runOnUiThread
                    if (fromEvent is EventApprovedSent) {
                        VCFsmHolder.initSignManager(currentAccount.nowViteAddress()!!)
                        ViteConnectActivity.show(this)
                    }
                }
                is StateUnconnected -> {
                    cycleProgressBar.dismiss()
                    val fromEvent = state.fromEvent ?: return@runOnUiThread
                    if (fromEvent is EventConnectFailed && fromEvent.isTimeout) {
                        showToast(R.string.net_work_timeout)
                    }
                    if (this is ViteConnectActivity || this is VCSignListActivity) {
                        this.finish()
                    }
                }
                is StateUserApprovalProcess -> {
                }
                is StateWaitingApprovalRequest -> {
                }
                is StateWaitingUserApproval -> {
                    state.request?.let {
                        showApproveDialog(it)
                    }
                }
            }
        }
    }

    open fun postTaskToUserCheck() {
        runOnUiThread {
            if (VCFsmHolder.signManager?.getCurrentTask()?.currentState == VcRequestTask.WaitUserProcessing) {
                ViteConnectActivity.show(this)
            }
        }
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        if (qrcodeParseResult.result is ViteBridgeUrlTransferParams) {
            cycleProgressBar.show()
            VCFsmHolder.tryEstablishSession(qrcodeParseResult.rawData)
        } else {
            super.onScanResult(qrcodeParseResult)
        }
    }

    fun showApproveDialog(req: Session.MethodCall.SessionRequest) {

        val currentViteAddr = this.currentAccount.nowViteAddress() ?: return

        runOnUiThread {
            val message =
                if (req.peer.meta?.lastAccount != null && currentViteAddr != req.peer.meta.lastAccount) {
                    getString(R.string.vitebifrost_connected_address_not_same)
                } else {
                    getString(
                        R.string.vitebifrost_connected_qr_notice_content, req.peer.meta?.url ?: ""
                    )
                }
            with(TextTitleNotifyDialog(this)) {
                setTitle(R.string.warm_notice)
                setCancelable(false)
                setMessage(message)
                setBottomLeft(R.string.cancel) {
                    it.dismiss()
                    VCFsmHolder.close()
                }
                setBottom(R.string.confirm) {
                    it.dismiss()
                    VCFsmHolder.approve(listOf(currentViteAddr))
                    cycleProgressBar.show()
                }
                show()
            }
        }
    }
}