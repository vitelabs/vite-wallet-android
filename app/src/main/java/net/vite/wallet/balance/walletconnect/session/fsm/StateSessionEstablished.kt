package net.vite.wallet.balance.walletconnect.session.fsm

import net.vite.wallet.balance.walletconnect.session.VCFsm


class StateSessionEstablished : VCState() {
    var fromEvent: VCEvent? = null
        private set

    override fun enter(fsm: VCFsm, vcEvent: VCEvent) {
        when (vcEvent) {
            is EventTxSendCancel -> {
                fsm.session?.sendTxCancel(vcEvent.id, vcEvent.errorMessage)
            }
            is EventTxSendSuccess -> {
                fsm.session?.approveRequest(vcEvent.id, vcEvent.accountBlock)
            }
        }
        fromEvent = vcEvent
    }
}