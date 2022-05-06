package net.vite.wallet.balance.walletconnect.session.fsm

import net.vite.wallet.balance.walletconnect.session.VCFsm

class StateReconnected : VCState() {

    var timer: MyCountDownTimer? = null
    override fun enter(fsm: VCFsm, vcEvent: VCEvent) {
        timer?.cancel()
        timer = MyCountDownTimer(10 * 1000) {
            fsm.sendEvent(EventPongTimeout())
        }
        timer?.start()
        fsm.session?.ping()
    }

    override fun leave(fsm: VCFsm, vcEvent: VCEvent) {
        timer?.cancel()
        timer = null
    }

}