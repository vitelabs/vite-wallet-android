package net.vite.wallet.balance.walletconnect.session.fsm

import net.vite.wallet.balance.walletconnect.session.VCFsm
import org.walletconnect.Session
import org.walletconnect.impls.MoshiPayloadAdapter
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCSession
import java.lang.ref.WeakReference

class StateConnecting : VCState() {
    private var connectTimer: MyCountDownTimer? = null

    override fun enter(fsm: VCFsm, vcEvent: VCEvent) {
        if (vcEvent !is EventConnect) {
            throw VcFsmInvalidOpException(
                vcEvent, this
            )
        }

        val preConnectSession = WCSession(
            Session.Config.fromWCUri(vcEvent.vcUri),
            MoshiPayloadAdapter(fsm.moshi),
            OkHttpTransport.Builder(fsm.client),
            Session.PeerMeta(name = "android")
        )
        preConnectSession.addCallback(vcEvent.cb)

        val myFsm = WeakReference<VCFsm>(fsm)

        connectTimer?.cancel()
        connectTimer = MyCountDownTimer(10 * 1000) {
            myFsm.get()?.sendEvent(EventConnectFailed(true))
        }

        connectTimer?.start()
        if (preConnectSession.connect()) {
            fsm.connected(preConnectSession)
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