package org.walletconnect.impls

import com.google.gson.Gson
import net.vite.wallet.logt
import okhttp3.*
import java.util.concurrent.CountDownLatch

class OkHttpTransport(
    val client: OkHttpClient,
    val serverUrl: String,
    val statusHandler: (Status) -> Unit,
    val messageHandler: (Message) -> Unit
) : WebSocketListener() {
    enum class Status {
        CONNECTED,
        DISCONNECTED,
        INTERRUPT,
    }

    data class Message(
        val topic: String,
        val type: String,
        val payload: String,
        val offset: Int? = null,
        val bridgeVersion: String? = "2"
    )

    private val gson = Gson()
    private val socketLock = Any()
    private var socket: WebSocket? = null
    private var connected: Boolean = false

    fun status(): Status =
        if (connected) Status.CONNECTED else Status.DISCONNECTED


    private var connectFinishLatch: CountDownLatch? = null
    fun connect(): Boolean {
        synchronized(socketLock) {
            if (connected) return true
            connectFinishLatch = CountDownLatch(1)
            val bridgeWS = serverUrl.replace("https://", "wss://").replace("http://", "ws://")
            socket = client.newWebSocket(Request.Builder().url(bridgeWS).build(), this)
        }

        connectFinishLatch?.await()
        connectFinishLatch = null
        return connected
    }

    fun send(message: Message) {
        if (!connected) return
        socket?.let { s ->
            val json = gson.toJson(message)
            logt("viteFsm real $json")
            s.send(json)
        }
    }

    fun close() {
        socket?.close(1000, null)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        connected = true
        connectFinishLatch?.countDown()
        statusHandler(Status.CONNECTED)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        kotlin.runCatching { gson.fromJson<Message>(text, Message::class.java) }.getOrNull()?.let { messageHandler(it) }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        socket = null
        connected = false
        connectFinishLatch?.countDown()
        statusHandler(Status.INTERRUPT)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        socket = null
        connected = false
        connectFinishLatch?.countDown()

        statusHandler(Status.DISCONNECTED)
    }

    class Builder(val client: OkHttpClient) {
        fun build(
            url: String,
            statusHandler: (Status) -> Unit,
            messageHandler: (Message) -> Unit
        ) = OkHttpTransport(client, url, statusHandler, messageHandler)
    }

}
