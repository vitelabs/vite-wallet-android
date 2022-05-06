package net.vite.wallet.exchange.net.ws

import net.vite.wallet.loge
import net.vite.wallet.logi
import net.vite.wallet.logt
import net.vite.wallet.network.http.vitex.*
import okhttp3.*
import okio.ByteString
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ExchangeWs(
    val url: String,
    val statusHandler: (Status) -> Unit,
    val messageHandler: (TickerStatistics) -> Unit,
    val tradeListHandler: (TradeList) -> Unit,
    val depthListHandler: (DepthList, String) -> Unit,
    val klineHandler: (Kline, String) -> Unit,
    val orderHandler: (Order) -> Unit
) : WebSocketListener() {
    enum class Status {
        CONNECTED,
        DISCONNECTED,
        INTERRUPT,
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    val clientId = UUID.randomUUID().toString()

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    var connected: Boolean = false
        private set

    @Volatile
    var isConnecting: Boolean = false

    var connectLatch: CountDownLatch? = null

    fun connect() {
        logt("MarketLog WS connect ")
        if (connected || isConnecting) {
            return
        }
        mustConnect()
    }

    fun mustConnect() {
        close()
        isConnecting = true
        connectLatch = CountDownLatch(1)
        socket = client.newWebSocket(
            Request.Builder().url(url).build(),
            this
        )
        connectLatch?.await()
        isConnecting = false
        connectLatch = null
        ping()
        sub("market.quoteTokenCategory.VITE.tickers")
        sub("market.quoteTokenCategory.ETH.tickers")
        sub("market.quoteTokenCategory.USDT.tickers")
        sub("market.quoteTokenCategory.BTC.tickers")

        logt("MarketLog WS connect $connected")
    }


    fun close() {
        socket?.close(1001, "by manual")
    }

    private fun send(dp: DexProto.DexProtocol) {
        if (!connected) return
        socket?.send(
            ByteString.of(
                ByteBuffer.wrap(dp.toByteArray())
            )
        )
    }

    private fun ping() {
        val dp = DexProto.DexProtocol.newBuilder()
            .setClientId(clientId)
            .setOpType("ping")
            .setErrorCode(0)
            .setTopics("").build()
        send(dp)
    }

    fun sub(topics: String) {
        val dp = DexProto.DexProtocol.newBuilder()
            .setClientId(clientId)
            .setOpType("sub")
            .setErrorCode(0)
            .setTopics(topics).build()
        send(dp)
        logi("ExchangeWs sub $topics")
    }

    fun unsub(topics: String) {
        val dp = DexProto.DexProtocol.newBuilder()
            .setClientId(clientId)
            .setOpType("un_sub")
            .setErrorCode(0)
            .setTopics(topics).build()
        send(dp)
        logi("ExchangeWs un_sub $topics")
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        connected = true
        connectLatch?.countDown()
        statusHandler.invoke(Status.CONNECTED)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        connected = false
        connectLatch?.countDown()
        statusHandler.invoke(Status.INTERRUPT)
        loge(t)
    }


    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        connected = false
        connectLatch?.countDown()
        statusHandler.invoke(Status.DISCONNECTED)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        try {
            val dexProtocol =
                DexProto.DexProtocol.parseFrom(bytes.toByteArray())
            if (dexProtocol.opType == "pong") {
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        ping()
                    }
                }, 20 * 1000L)
            }
            if (dexProtocol.opType != "push" || dexProtocol.clientId != clientId) {
                return
            }

            if (dexProtocol.topics.matches(Regex("market.*trade"))) {
                val tp =
                    DexPushMessage.TradeListProto.newBuilder().mergeFrom(dexProtocol.message)
                        .build()
                val fromPb = TradeList.fromPb(tp)
                tradeListHandler.invoke(fromPb)
            } else if (dexProtocol.topics.matches(Regex("market\\..*\\.kline\\..*"))) {
                val tp =
                    DexPushMessage.KlineProto.newBuilder().mergeFrom(dexProtocol.message)
                        .build()
                val kline = Kline.fromPb(tp)
                val symbol = dexProtocol.topics.substring(
                    "market.".length,
                    dexProtocol.topics.indexOf(".kline")
                )
                klineHandler.invoke(kline, symbol)
            } else if (dexProtocol.topics.matches(Regex("market.*\\.depth"))) {
                val tp =
                    DexPushMessage.DepthListProto.newBuilder().mergeFrom(dexProtocol.message)
                        .build()
                val depthList = DepthList.fromPb(tp)
                val symbol =
                    dexProtocol.topics.replace("market.", "").replace(".depth", "")
                depthListHandler.invoke(depthList, symbol)
            } else if (dexProtocol.topics.matches(Regex("order.*"))) {
                val tp =
                    DexPushMessage.OrderProto.newBuilder().mergeFrom(dexProtocol.message)
                        .build()
                val order = Order.fromPb(tp)
                orderHandler.invoke(order)
            } else {
                val tp =
                    DexPushMessage.TickerStatisticsProto.newBuilder().mergeFrom(dexProtocol.message)
                        .build()
                messageHandler.invoke(TickerStatistics.fromPb(tp))
            }

        } catch (e: Throwable) {
            loge(e)
            e.printStackTrace()
        }
    }

}