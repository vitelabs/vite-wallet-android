package net.vite.wallet.balance.walletconnect.session

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.squareup.moshi.Moshi
import net.vite.wallet.balance.walletconnect.session.fsm.*
import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.parse
import net.vite.wallet.logt
import okhttp3.OkHttpClient
import org.walletconnect.Session
import org.walletconnect.impls.WCSession
import java.util.concurrent.TimeUnit


typealias VCFsmStateEnterCallback = (state: VCState) -> Unit
typealias NewRequestArrivedTrigger = (tx: Session.MethodCall.VCTask, confirmInfo: WCConfirmInfo) -> Unit

class VCFsm {

    data class VCFsmEdge(
        val start: VCState,
        val event: Class<out VCEvent>,
        val end: VCState
    )

    val stateUnconnected = StateUnconnected()
    val stateConnecting = StateConnecting()
    val stateWaitingApprovalRequest = StateWaitingApprovalRequest()
    val stateWaitingUserApproval = StateWaitingUserApproval()
    val stateUserApprovalProcess = StateUserApprovalProcess()
    val stateSessionEstablished = StateSessionEstablished()
    val stateConnectionLost = StateConnectionLost()
    val stateReconnecting = StateReconnecting()
    val stateReconnected = StateReconnected()

    val FSM = listOf(
        VCFsmEdge(stateUnconnected, EventConnect::class.java, stateConnecting),

        VCFsmEdge(stateConnecting, EventConnectFailed::class.java, stateUnconnected),
        VCFsmEdge(stateConnecting, EventConnectionClosed::class.java, stateUnconnected),
        VCFsmEdge(stateConnecting, EventConnectSuccess::class.java, stateWaitingApprovalRequest),

        VCFsmEdge(
            stateWaitingApprovalRequest,
            EventWaitApproveRequestTimeout::class.java,
            stateUnconnected
        ),
        VCFsmEdge(stateWaitingApprovalRequest, EventConnectInterrupt::class.java, stateUnconnected),
        VCFsmEdge(stateWaitingApprovalRequest, EventConnectionClosed::class.java, stateUnconnected),
        VCFsmEdge(
            stateWaitingApprovalRequest,
            EventApproveRequestArrived::class.java,
            stateWaitingUserApproval
        ),

        VCFsmEdge(stateWaitingUserApproval, EventConnectionClosed::class.java, stateUnconnected),
        VCFsmEdge(stateWaitingUserApproval, EventApproved::class.java, stateUserApprovalProcess),

        VCFsmEdge(stateUserApprovalProcess, EventApprovedSent::class.java, stateSessionEstablished),
        VCFsmEdge(stateUserApprovalProcess, EventConnectionClosed::class.java, stateUnconnected),

        VCFsmEdge(stateSessionEstablished, EventConnectionClosed::class.java, stateUnconnected),
        VCFsmEdge(stateSessionEstablished, EventConnectInterrupt::class.java, stateConnectionLost),
        VCFsmEdge(stateSessionEstablished, EventTxSendSuccess::class.java, stateSessionEstablished),
        VCFsmEdge(stateSessionEstablished, EventTxSendCancel::class.java, stateSessionEstablished),


        VCFsmEdge(stateConnectionLost, EventReconnect::class.java, stateReconnecting),
        VCFsmEdge(stateConnectionLost, EventConnectionClosed::class.java, stateUnconnected),


        VCFsmEdge(stateReconnecting, EventConnectionClosed::class.java, stateUnconnected),
        VCFsmEdge(stateReconnecting, EventConnectFailed::class.java, stateConnectionLost),
        VCFsmEdge(stateReconnecting, EventConnectSuccess::class.java, stateReconnected),

        VCFsmEdge(stateReconnected, EventPongTimeout::class.java, stateUnconnected),
        VCFsmEdge(stateReconnected, EventConnectionClosed::class.java, stateUnconnected),
        VCFsmEdge(stateReconnected, EventConnectInterrupt::class.java, stateConnectionLost),
        VCFsmEdge(stateReconnected, EventPongArrived::class.java, stateSessionEstablished)
    )


    private val workThread = HandlerThread("VCFsm Thread")
    val moshi = Moshi.Builder().build()
    val client = OkHttpClient.Builder().pingInterval(1000, TimeUnit.MILLISECONDS).build()

    private val vCFsmStateEnterCallbacks = ArrayList<VCFsmStateEnterCallback>()
    fun addVCFsmStateEnterCallback(cb: VCFsmStateEnterCallback) {
        vCFsmStateEnterCallbacks.add(cb)
    }

    fun rmvCFsmStateEnterCallback(cb: VCFsmStateEnterCallback) {
        vCFsmStateEnterCallbacks.remove(cb)
    }

    private val newRequestArrivedTriggers = ArrayList<NewRequestArrivedTrigger>()
    fun addNewRequestArrivedTrigger(cb: NewRequestArrivedTrigger) {
        newRequestArrivedTriggers.add(cb)
    }

    fun rmNewRequestArrivedTrigger(cb: NewRequestArrivedTrigger) {
        newRequestArrivedTriggers.remove(cb)
    }


    var currentVCState: VCState = stateUnconnected
        private set
    @Volatile
    var isDisposed = false
        private set

    @Volatile
    var session: WCSession? = null
    @Volatile
    private var sessionCb: Session.Callback? = null


    var retryCount = 0

    fun dispose() {
        isDisposed = true
        kotlin.runCatching { workThread.quitSafely() }
    }

    private val fsmThreadHandler: Handler

    init {
        workThread.start()
        fsmThreadHandler = Handler(workThread.looper) { msg ->
            if (isDisposed) {
                return@Handler true
            }
            val event = msg.obj as VCEvent

            val edger = FSM.find { vcFsmEdge ->
                vcFsmEdge.start == currentVCState && event.javaClass == vcFsmEdge.event
            } ?: kotlin.run {
                logt("not found any op current:$currentVCState event:$event")
                return@Handler true
            }

            logt("viteFsm $currentVCState  ${event.javaClass.simpleName} ${edger.end}")

            edger.start.leave(this, event)

            edger.end.enter(this, event)
            logt("viteFsm enter end")

            currentVCState = edger.end
            if (currentVCState == stateConnectionLost) {
                triggerReconnect()
            }
            if (currentVCState == stateReconnected) {
                retryCount = 0
            }

            vCFsmStateEnterCallbacks.forEach {
                kotlin.runCatching { it.invoke(currentVCState) }
            }
            true
        }

    }


    private var wcUri = ""
    private var approvedAccounts = ArrayList<String>()


    fun connect(wcUri: String) {
        this.wcUri = wcUri
        this.sessionCb = SessionCallback()
        sendEvent(EventConnect(wcUri, sessionCb!!))
    }

    fun close() {
        sendEvent(EventConnectionClosed())
    }

    fun approve(accounts: List<String>) {
        approvedAccounts.clear()
        approvedAccounts.addAll(accounts)
        sendEvent(EventApproved(accounts))
    }

    fun sendEvent(event: VCEvent) {
        fsmThreadHandler.sendMessage(Message.obtain().apply {
            obj = event
        })
    }

    fun connected(vcSession: WCSession) {
        session = vcSession
    }

    fun clearOldSession() {
        sessionCb?.let { cb ->
            session?.removeCallback(cb)
        }
        session = null
    }

    fun forceRetry() {
        if (isDisposed || currentVCState != stateConnectionLost) {
            return
        }
        fsmThreadHandler.sendMessage(
            Message.obtain().apply {
                obj = EventConnect(wcUri, sessionCb!!)
            }
        )
    }

    private fun triggerReconnect() {
        if (isDisposed || currentVCState != stateConnectionLost || session == null) {
            return
        }
        val delayedTime = when (retryCount) {
            in 0..10 -> 2
            in 10..20 -> 10
            else -> {
                30
            }
        } * 1000
        if (sessionCb == null) {
            sessionCb = SessionCallback()
        }
        fsmThreadHandler.sendMessageDelayed(
            Message.obtain().apply {
                obj = EventReconnect(session!!)
            }, delayedTime.toLong()
        )
        retryCount++
    }


    private inner class SessionCallback : Session.Callback {
        override fun handleMethodCall(call: Session.MethodCall) {
            when (call) {
                is Session.MethodCall.SessionRequest -> {
                    sendEvent(EventApproveRequestArrived(call))
                }

                is Session.MethodCall.Ping -> {
                    sendEvent(EventPongArrived())
                }

                is Session.MethodCall.SignMessage -> {
                    call.parse().blockingSubscribe({ confirmInfo ->
                        newRequestArrivedTriggers.forEach {
                            val task =
                                Session.MethodCall.VCTask(Session.MethodCall.VCTask.TYPE_SIGN_MESSAGE)
                            task.signMessage = call
                            kotlin.runCatching {
                                it(task, confirmInfo)
                            }
                        }
                    }, {
                        session?.rejectRequest(call.id, "parse error")
                    })
                }
                is Session.MethodCall.SendTransaction -> {
                    call.parse().blockingSubscribe({ confirmInfo ->
                        newRequestArrivedTriggers.forEach {
                            kotlin.runCatching {
                                val task =
                                    Session.MethodCall.VCTask(Session.MethodCall.VCTask.TYPE_SEND_TRANSACTION)
                                task.sendTransaction = call
                                it(task, confirmInfo)
                            }
                        }
                    }, {
                        session?.rejectRequest(call.id, "parse error")
                    })
                }
            }
        }

        override fun sessionApproved() {
            sendEvent(EventApprovedSent())
        }

        override fun sessionClosed() {
            sendEvent(EventConnectionClosed())
        }

        override fun sessionInterrupt() {
            sendEvent(EventConnectInterrupt())
        }
    }


}