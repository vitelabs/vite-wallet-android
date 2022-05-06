package net.vite.wallet.balance.walletconnect.session.fsm

import net.vite.wallet.balance.walletconnect.session.VCFsm

class StateUnconnected : VCState() {
    var fromEvent: VCEvent? = null
        private set

    override fun enter(fsm: VCFsm, vcEvent: VCEvent) {
        fromEvent = vcEvent
        when (vcEvent) {
            is EventConnectInterrupt -> {

            }
            is EventConnectionClosed -> {

            }
            is EventPongTimeout -> {

            }
            else -> {

            }
        }

        fsm.session?.kill()
        fsm.clearOldSession()
        fsm.dispose()
    }
}