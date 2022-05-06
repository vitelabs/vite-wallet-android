package net.vite.wallet.balance.walletconnect.session.fsm

import com.google.gson.Gson
import net.vite.wallet.network.rpc.AccountBlock
import org.walletconnect.Session
import org.walletconnect.impls.WCSession


class EventConnect(val vcUri: String, val cb: Session.Callback) : VCEvent() {
    override fun toString(): String {
        return "EventConnect $vcUri cb $cb"
    }
}


class EventReconnect(val oldSession: WCSession) : VCEvent() {
    override fun toString(): String {
        return "EventReconnect"
    }
}

class EventConnectFailed(val isTimeout: Boolean) : VCEvent() {
    override fun toString(): String {
        return "EventConnectFailed isTimeout $isTimeout"
    }
}

class EventWaitApproveRequestTimeout : VCEvent()
class EventConnectSuccess : VCEvent()
class EventApproveRequestArrived(val request: Session.MethodCall.SessionRequest) : VCEvent() {
    override fun toString(): String {
        return "EventApproveRequestArrived isTimeout ${Gson().toJson(request)}"
    }
}

class EventConnectInterrupt : VCEvent()
class EventConnectionClosed : VCEvent()
class EventApproved(val accounts: List<String>) : VCEvent() {
    override fun toString(): String {
        return kotlin.runCatching { "EventApproved accounts ${accounts[0]}" }.getOrDefault("EventApproved accounts 0")
    }
}

class EventApprovedSent : VCEvent()
class EventPongArrived : VCEvent()
class EventPongTimeout : VCEvent()

class EventTxSendSuccess(val id: Long, val accountBlock: AccountBlock) : VCEvent()
class EventTxSendCancel(val id: Long, val errorMessage: String = "") : VCEvent()

