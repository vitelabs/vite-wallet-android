package net.vite.wallet.balance.walletconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import net.vite.wallet.R
import net.vite.wallet.TxEndStatus
import net.vite.wallet.TxSendSuccess
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.balance.walletconnect.record.VCSignListActivity
import net.vite.wallet.balance.walletconnect.session.VCFsmHolder
import net.vite.wallet.balance.walletconnect.session.VcRequestTask
import net.vite.wallet.balance.walletconnect.session.fsm.EventTxSendCancel
import net.vite.wallet.balance.walletconnect.session.fsm.EventTxSendSuccess
import net.vite.wallet.balance.walletconnect.taskdetail.ConfirmInfoAdapter
import net.vite.wallet.balance.walletconnect.taskdetail.toNormalTxParams
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.network.rpc.AccountBlock
import net.vite.wallet.utils.hexToBytes
import net.vite.wallet.utils.showToast
import net.vite.wallet.utils.toHex
import org.vitelabs.mobile.Mobile
import org.walletconnect.Session
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class ViteConnectActivity : BaseTxSendActivity() {

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, ViteConnectActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_connect)
        connectedAddress.text = currentAccount.nowViteAddress()
        backIcon.setOnClickListener {
            onBackPressed()

        }


        wcKillSession.setOnClickListener {

            with(TextTitleNotifyDialog(this)) {
                setTitle(R.string.warm_notice)
                setMessage(R.string.wc_kill_confirm_msg)
                setBottom(R.string.confirm) { dialog ->

                    VCFsmHolder.close()
                    dialog.dismiss()
                    finish()
                }
                show()
            }
        }

        cancelButton.setOnClickListener {
            currentProcessingTask?.let {
                it.currentState = VcRequestTask.Cancel
                it.completeLatch.countDown()
                VCFsmHolder.sendEvent(
                    EventTxSendCancel(
                        it.tx.sendTransaction?.id ?: it.tx.signMessage?.id!!, "user cancel"
                    )
                )
            }
            currentProcessingTask = null
            viewEmptyTask()
        }

        confirmBtn.setOnClickListener {
            val nowTask = currentProcessingTask ?: return@setOnClickListener

            if (nowTask.tx.type == Session.MethodCall.VCTask.TYPE_SEND_TRANSACTION) {
                verifyIdentityDirectlyFinger(nowTask.confirmInfo.title, {

                    val sendParams =
                        nowTask.tx.sendTransaction!!.block.toNormalTxParams(currentAccount.nowViteAddress()!!)
                    val currentBalance =
                        ViteAccountInfoPoll.myLatestViteAccountInfo()?.balance?.tokenBalanceInfoMap?.get(
                            sendParams.tokenId
                        )?.totalAmount?.toBigIntegerOrNull() ?: BigInteger.ZERO

                    if (sendParams.amountInSu > currentBalance) {
                        showToast(R.string.balance_not_enough)
                        return@verifyIdentityDirectlyFinger
                    }

                    lastSendParams =
                        nowTask.tx.sendTransaction!!.block.toNormalTxParams(currentAccount.nowViteAddress()!!)

                    sendTx(lastSendParams!!)
                }, {})

            } else { //SIGN MESSAGE
                val signMessage = nowTask.tx.getObject() as Session.MethodCall.SignMessage

                val hash =
                    Mobile.hash256("Vite Signed Message:\n".toByteArray() + signMessage.message.hexToBytes())
                        .toHex()
                val signDataResult = AccountCenter.getCurrentAccount()?.signVite(hash)
                signDataResult?.let { sd ->
                    val publicKey = Base64.encodeToString(sd.publicKey, Base64.NO_WRAP)
                    val signature = Base64.encodeToString(sd.signature, Base64.NO_WRAP)
                    val ab = AccountBlock(publicKey = publicKey, signature = signature)

                    onTxEnd(TxSendSuccess(ab, null, 0))
                }


            }
        }

        signTxNums.setOnClickListener {
            VCSignListActivity.show(this)
        }

        autoSignTransactionSwitch.setOnCheckedChangeListener { compoundButton: CompoundButton, checked: Boolean ->
            if (checked) {
                val isAutoStarted = VCFsmHolder.signManager?.isAutoSignEnabled ?: false
                if (!isAutoStarted) {
                    verifyIdentityDirectlyFinger(getString(R.string.password_confirm), {
                        VCFsmHolder.startAutoSign()
                    }, {
                        autoSignTransactionSwitch.isChecked = false
                    })
                }
            } else {
                VCFsmHolder.closeAutoSign()
            }
        }
    }

    private var currentProcessingTask: VcRequestTask? = null
    override fun postTaskToUserCheck() {
        runOnUiThread { checkHasNewTask() }

    }

    private fun checkHasNewTask() {
        if (currentProcessingTask == null) {
            val currentTask = VCFsmHolder.signManager?.getCurrentTask()
            if (currentTask?.currentState == VcRequestTask.WaitUserProcessing) {
                currentProcessingTask = currentTask
            }
        }
        refreshView()
    }

    private fun refreshView() {
        currentProcessingTask?.let {
            recyclerView.adapter = ConfirmInfoAdapter(it.confirmInfo)
            viewHasTask()
        } ?: kotlin.run {
            viewEmptyTask()
        }
    }

    override fun onTxEnd(status: TxEndStatus) {
        if (status is TxSendSuccess) {
            status.powProfile?.let {
                showPowProfileDialog(it)
            } ?: kotlin.run {
                showToast(R.string.tx_send_success)
            }

            currentProcessingTask?.let {
                it.currentState = VcRequestTask.Success
                it.completeLatch.countDown()
                VCFsmHolder.sendEvent(
                    EventTxSendSuccess(
                        it.tx.sendTransaction?.id ?: it.tx.signMessage?.id!!,
                        status.accountBlock
                    )
                )
            }

            currentProcessingTask = null
            viewEmptyTask()
        }
    }


    var disposable: Disposable? = null

    override fun onStart() {
        super.onStart()
        if (!VCFsmHolder.hasExistSession()) {
            finish()
        }

        autoSignTransactionSwitch.isChecked = VCFsmHolder.signManager?.isAutoSignEnabled ?: false

        disposable = Observable.fromCallable {
            VCFsmHolder.signManager?.requestTaskArray ?: ArrayList()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .repeatWhen { it.delay(2, TimeUnit.SECONDS) }
            .subscribe {
                signTxNums.text = it.size.toString()
            }


        checkHasNewTask()

    }

    override fun onStop() {
        super.onStop()
        disposable?.dispose()
        disposable = null
    }

    private fun viewEmptyTask() {
        connectingStub.visibility = View.VISIBLE
        signTxProfileLayout.visibility = View.VISIBLE
        connectedGroup.visibility = View.GONE

    }

    private fun viewHasTask() {
        connectingStub.visibility = View.GONE
        signTxProfileLayout.visibility = View.GONE
        connectedGroup.visibility = View.VISIBLE
    }


}