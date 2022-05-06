package net.vite.wallet.balance.walletconnect.session.fsm

import net.vite.wallet.balance.walletconnect.session.VCFsm
import java.lang.ref.WeakReference

class StateWaitingApprovalRequest : VCState() {
    private var timer: MyCountDownTimer? = null

    override fun enter(fsm: VCFsm, vcEvent: VCEvent) {
        val myFsm = WeakReference<VCFsm>(fsm)
        timer?.cancel()
        timer = MyCountDownTimer(10 * 1000) {
            myFsm.get()?.sendEvent(EventWaitApproveRequestTimeout())
        }
        timer?.start()
    }

    override fun leave(fsm: VCFsm, vcEvent: VCEvent) {
        timer?.cancel()
        timer = null
    }
}
