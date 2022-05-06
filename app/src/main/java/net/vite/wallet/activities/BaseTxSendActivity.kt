package net.vite.wallet.activities

import android.os.Bundle
import net.vite.wallet.BaseTxSendFlow
import net.vite.wallet.PowProfile
import net.vite.wallet.TxEndStatus
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.ProgressDialog
import net.vite.wallet.network.rpc.NormalTxParams


abstract class BaseTxSendActivity : UnchangableAccountAwareActivity() {

    lateinit var cycleProgressBar: ProgressDialog

    var lastSendParams: NormalTxParams? = null
    protected val baseTxSendFlow = BaseTxSendFlow(this, {
        lastSendParams
    }, ::onTxEnd)

    fun verifyIdentity(params: BigDialog.Params?, onVerifyIdSuccess: () -> Unit, onClose: () -> Unit) {
        baseTxSendFlow.identityVerify.verifyIdentity(params, onVerifyIdSuccess, onClose)
    }

    fun verifyIdentityDirectlyFinger(title: String, onVerifyIdSuccess: () -> Unit, onClose: () -> Unit) {
        baseTxSendFlow.identityVerify.verifyIdentityDirectlyFinger(title, onVerifyIdSuccess, onClose)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseTxSendFlow.onCreate()
        cycleProgressBar = baseTxSendFlow.cycleProgressBar
    }

    fun showPowProfileDialog(powProfile: PowProfile, onDialogDismiss: (() -> Unit)? = null) {
        baseTxSendFlow.showPowProfileDialog(powProfile, onDialogDismiss)
    }

    open fun onTxEnd(status: TxEndStatus) {
        baseTxSendFlow.handleTxEndStatus(status)
    }


    fun sendTx(p: NormalTxParams, requestCode: Int? = 0) {
        baseTxSendFlow.txSendViewModel.sendTx(p, requestCode)
    }

}
