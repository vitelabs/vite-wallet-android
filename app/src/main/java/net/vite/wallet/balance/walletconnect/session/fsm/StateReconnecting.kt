package net.vite.wallet.balance.walletconnect.session.fsm

import net.vite.wallet.balance.walletconnect.session.VCFsm
import java.lang.ref.WeakReference

class StateReconnecting : VCState() {
    private var connectTimer: MyCountDownTimer? = null

    override fun enter(fsm: VCFsm, vcEvent: VCEvent) {
        if (vcEvent !is EventReconnect) {
            throw VcFsmInvalidOpException(vcEvent, this)
        }

        val myFsm = WeakReference<VCFsm>(fsm)
        connectTimer?.cancel()
        connectTimer = MyCountDownTimer(10 * 1000) {
            myFsm.get()?.sendEvent(EventConnectFailed(true))
        }

        connectTimer?.start()
        if (vcEvent.oldSession.reconnect()) {
            fsm.sendEvent(EventConnectSuccess())
        } else {
            fsm.sendEvent(EventConnectFailed(false))
        }
    }


    override fun leave(fsm: VCFsm, vcEvent: VCEvent) {
        connectTimer?.cancel()
        connectTimer = null
    }
}