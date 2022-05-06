package net.vite.wallet.balance.walletconnect.session

import net.vite.wallet.balance.walletconnect.session.fsm.*
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import org.walletconnect.Session

object VCFsmHolder {
    @Volatile
    private var vcFsm: VCFsm? = null

    @Volatile
    var signManager: SignManager? = null
        private set

    fun tryEstablishSession(uri: String) {
        vcFsm?.close()
        vcFsm = VCFsm()
        vcFsm?.addVCFsmStateEnterCallback(::vcSessionStatusAwareFun)
        vcFsm?.addNewRequestArrivedTrigger(::newTaskArrived)
        vcFsm?.connect(uri)
    }

    fun getTask(index: Int) = signManager?.getTask(index)

    fun initSignManager(address: String) {
        signManager = SignManager(address)
    }

    fun close() {
        vcFsm?.close()
    }

    fun approve(accounts: List<String>) {
        vcFsm?.approve(accounts)
    }

    fun hasExistSession(): Boolean {
        return vcFsm != null && vcFsm?.currentVCState != vcFsm?.stateUnconnected
    }

    private val vCFsmStateEnterCallbacks = HashMap<Int, VCFsmStateEnterCallback>()
    private var vCFsmStateEnterCallbackId = 0
    fun addVCFsmStateEnterCallback(cb: VCFsmStateEnterCallback): Int {
        vCFsmStateEnterCallbackId++
        vCFsmStateEnterCallbacks.put(vCFsmStateEnterCallbackId, cb)
        return vCFsmStateEnterCallbackId
    }


    fun sendEvent(event: VCEvent) {
        vcFsm?.sendEvent(event)
    }

    fun rmVCFsmStateEnterCallback(id: Int) {
        vCFsmStateEnterCallbacks.remove(id)
    }

    fun forceRetry() {
        vcFsm?.forceRetry()
    }

    private fun newTaskArrived(tx: Session.MethodCall.VCTask, confirmInfo: WCConfirmInfo) {
        signManager?.addTask(VcRequestTask(tx, confirmInfo))
    }

    fun vcSessionStatusAwareFun(state: VCState) {
        vCFsmStateEnterCallbacks.values.forEach {
            kotlin.runCatching { it.invoke(state) }
        }

        when (state) {
            is StateConnecting -> {
            }

            is StateConnectionLost -> {
                signManager?.suspend()
            }

            is StateReconnected -> {

            }

            is StateSessionEstablished -> {
                signManager?.resume()
            }

            is StateUnconnected -> {
                vcFsm?.rmvCFsmStateEnterCallback(::vcSessionStatusAwareFun)
                vcFsm?.rmNewRequestArrivedTrigger(::newTaskArrived)
                vcFsm = null
                signManager?.stop()
                signManager = null
            }

            is StateUserApprovalProcess -> {

            }

            is StateWaitingApprovalRequest -> {

            }
            is StateWaitingUserApproval -> {

            }
        }
    }

    fun closeAutoSign() {
        signManager?.isAutoSignEnabled = false
    }

    fun startAutoSign() {
        signManager?.isAutoSignEnabled = true
    }

}