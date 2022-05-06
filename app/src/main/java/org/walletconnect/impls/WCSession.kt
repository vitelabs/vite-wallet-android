package org.walletconnect.impls

import net.vite.wallet.logt
import net.vite.wallet.network.rpc.AccountBlock
import org.walletconnect.Session
import org.walletconnect.nullOnThrow
import org.walletconnect.types.intoMap
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap


class WCSession(
    private val config: Session.Config,
    private val payloadAdapter: Session.PayloadAdapter,
    transportBuilder: OkHttpTransport.Builder,
    clientMeta: Session.PeerMeta
) {

    val offset = HashMap<String, Int>()

    private val keyLock = Any()

    private var approvedAccounts: List<String>? = null

    private var handshakeId: Long? = null
    private var peerId: String? = null
    private var peerMeta: Session.PeerMeta? = null

    private val clientData: Session.PeerData =
        Session.PeerData(UUID.randomUUID().toString(), clientMeta)

    private val transport = transportBuilder.build(config.bridge, ::handleStatus, ::handleMessage)

    private val sessionCallbacks: MutableSet<Session.Callback> =
        Collections.newSetFromMap(ConcurrentHashMap<Session.Callback, Boolean>())

    fun addCallback(cb: Session.Callback) {
        sessionCallbacks.add(cb)
    }

    fun removeCallback(cb: Session.Callback) {
        sessionCallbacks.remove(cb)
    }

    fun ping() {
        logt("viteFsm ping start")
        send(Session.MethodCall.Ping(createCallId()))
        logt("viteFsm ping end")
    }


    fun sendTxCancel(id: Long, errorMsg: String) {
        send(
            Session.MethodCall.Response(
                id,
                result = null,
                error = Session.Error.requestCancel(errorMsg)
            )
        )
    }

    fun approveRequest(id: Long, response: AccountBlock) {
        send(Session.MethodCall.Response(id, response))
    }

    fun rejectRequest(id: Long, errorMsg: String) {
        send(
            Session.MethodCall.Response(
                id,
                result = null,
                error = Session.Error.rejectRequestError(errorMsg)
            )
        )
    }


    fun reconnect(): Boolean {
        peerId ?: return false
        if (transport.connect()) {
            logt("viteFsm reconnect sub start $offset")
            transport.send(
                OkHttpTransport.Message(
                    topic = peerId!!, type = "sub", payload = "", offset = offset[peerId!!]
                )
            )
            logt("viteFsm reconnect sub end")
            return true
        }
        return false
    }


    fun connect(): Boolean {
        if (transport.connect()) {
            logt("viteFsm sub start $offset")
            transport.send(
                OkHttpTransport.Message(
                    topic = config.handshakeTopic,
                    type = "sub",
                    payload = "",
                    offset = offset[config.handshakeTopic]
                )
            )
            logt("viteFsm sub end")
            return true
        }
        return false
    }

    fun approveSessionEstablish(accounts: List<String>, chainId: Long) {
        val handshakeId = handshakeId ?: return
        approvedAccounts = accounts
        val params = Session.SessionParams(true, chainId, accounts, clientData).intoMap()
        send(Session.MethodCall.Response(handshakeId, params))
        sessionCallbacks.forEach { nullOnThrow { it.sessionApproved() } }
    }

    fun kill() {
        rejectSessionEstablish()
    }

    fun rejectSessionEstablish() {
        handshakeId?.let {
            val params = Session.SessionParams(false, null, null, null).intoMap()
            send(Session.MethodCall.Response(it, params))
        }
        endSession()
    }

    private fun endSession() {
        approvedAccounts = null
        internalClose()
        sessionCallbacks.forEach { nullOnThrow { it.sessionClosed() } }
    }


    private fun handleStatus(status: OkHttpTransport.Status) {
        when (status) {
            OkHttpTransport.Status.CONNECTED -> {
                logt("viteFsm handleStatus CONNECTED ${offset[clientData.id]}")
                transport.send(
                    OkHttpTransport.Message(
                        clientData.id, "sub", "", offset[clientData.id]
                    )
                )
            }


            OkHttpTransport.Status.DISCONNECTED -> {
                sessionCallbacks.forEach {
                    nullOnThrow {
                        it.sessionInterrupt()
                    }
                }
            }
            OkHttpTransport.Status.INTERRUPT -> {
                sessionCallbacks.forEach {
                    nullOnThrow {
                        it.sessionInterrupt()
                    }
                }
            }
        }
    }

    private fun handleMessage(message: OkHttpTransport.Message) {
        if (message.type != "pub") return
        logt("viteFsm handleMessage ${message.offset} $message")
        message.offset?.let {
            offset[message.topic] = it
        }
        val data: Session.MethodCall
        synchronized(keyLock) {
            try {
                data = payloadAdapter.parse(message.payload, config.key)
            } catch (e: Exception) {
                (e as? Session.MethodCallException)?.let {
                    rejectRequest(it.id, it.message ?: "Unknown error")
                }
                return
            }
        }
        when (data) {
            is Session.MethodCall.SessionRequest -> {
                handshakeId = data.id
                peerId = data.peer.id
                peerMeta = data.peer.meta
            }
            is Session.MethodCall.SessionUpdate -> {
                if (!data.params.approved) {
                    endSession()
                }
            }
        }

        sessionCallbacks.forEach {
            nullOnThrow {
                it.handleMethodCall(data)
            }
        }
    }

    private fun send(
        msg: Session.MethodCall,
        topic: String? = peerId
    ): Boolean {
        topic ?: return false

        val payload: String
        synchronized(keyLock) {
            payload = payloadAdapter.prepare(msg, config.key)
        }
        transport.send(OkHttpTransport.Message(topic, "pub", payload))
        return true
    }

    private fun createCallId() = System.currentTimeMillis() * 1000 + Random().nextInt(999)

    private fun internalClose() {
        transport.close()
    }
}