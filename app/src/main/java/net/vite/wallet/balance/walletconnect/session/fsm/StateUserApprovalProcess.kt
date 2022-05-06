package net.vite.wallet.balance.walletconnect.session.fsm

import net.vite.wallet.balance.walletconnect.session.VCFsm


class StateUserApprovalProcess : VCState() {
    override fun enter(fsm: VCFsm, vcEvent: VCEvent) {
        if (vcEvent !is EventApproved) {
            throw VcFsmInvalidOpException(
                vcEvent, this
            )
        }
        fsm.session?.approveSessionEstablish(vcEvent.accounts, 1)
    }

}
