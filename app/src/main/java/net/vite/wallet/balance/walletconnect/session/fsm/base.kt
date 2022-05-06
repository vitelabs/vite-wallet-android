package net.vite.wallet.balance.walletconnect.session.fsm


import net.vite.wallet.balance.walletconnect.session.VCFsm

abstract class VCState {

    open fun enter(fsm: VCFsm, vcEvent: VCEvent) {}
    open fun leave(fsm: VCFsm, vcEvent: VCEvent) {}
    override fun toString(): String {
        return this.javaClass.simpleName
    }

}

abstract class VCEvent {
    override fun toString(): String {
        return this.javaClass.simpleName
    }
}

class VcFsmInvalidOpException(val vcEvent: VCEvent, val vcState: VCState) :
    Exception("vcEvent: $vcEvent,vcState: $vcState")





