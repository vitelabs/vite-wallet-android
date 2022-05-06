package net.vite.wallet.balance.walletconnect.session.fsm

import net.vite.wallet.balance.walletconnect.session.VCFsm
import org.walletconnect.Session

class StateWaitingUserApproval : VCState() {
    var request: Session.MethodCall.SessionRequest? = null
        private set

    override fun enter(fsm: VCFsm, vcEvent: VCEvent) {
        if (vcEvent !is EventApproveRequestArrived) {
            throw VcFsmInvalidOpException(
                vcEvent, this
            )
        }
        request = vcEvent.request
    }
}
