package net.vite.wallet.network.http.vitex

import androidx.annotation.Keep
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.*
import net.vite.wallet.exchange.net.ws.DexPushMessage
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.http.HttpClient
import net.vite.wallet.network.http.ViteNormalBaseResponse
import net.vite.wallet.network.http.processViteBase
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.math.BigDecimal

//API DOC https://docs.vite.org/go-vite/dex/api/rest-api.html
@Keep
data class OperatorInfo(
    val name: String,
    val tradePairs: Map<String, List<String>>,
    val address: String?,
    val gateway: String?,
    val icon: String?,
    val support: String?,
    val level: Int?,
    val links: Map<String, List<String>>?,
    val overview: Overview?
)

@Keep
data class OrderItem(
    val index: Int,
    val buyAmount: String,
    val sellAmount: String,
    val buyPrice: String,
    val sellPrice: String,

    val buyPercent: Float,
    val sellPercent: Float,

    val buyCanMining: Boolean,
    val sellCanMining: Boolean,

    val buyHasLine: Boolean,
    val sellHasLine: Boolean,

    val hasBuyAvatar: Boolean,
    val hasSellAvatar: Boolean
)

@Keep
data class ExchangeLimits(
    val minAmount: Map<String, String>
)

@Keep
data class ClosedMarket(
    var symbol: String? = null,
    var tradeToken: String? = null,
    var quoteToken: String? = null,
    var tradeTokenSymbol: String? = null,
    var quoteTokenSymbol: String? = null,
    var owner: String? = null,
    var stopped: Int = 0
)

@Keep
data class HiddenMarkets(
    var hideSymbols: List<String>?
)

@Keep
data class MiningSetting(
    val tradeSymbols: List<String>,
    val orderSymbols: List<String>,
    val orderMiningSettings: Map<String, OrderMiningSetting>,
    val orderMiningMultiples: Map<String, String>,
    val zeroFeePairs: List<String>
)

@Keep
data class Orders(
    val order: MutableList<Order>?,
    val total: Int?
)

@Keep
data class Order(
    var orderId: String? = null,
    var symbol: String? = null,
    var tradeTokenSymbol: String? = null,
    var quoteTokenSymbol: String? = null,
    var tradeToken: String? = null,
    var quoteToken: String? = null,
    var side: Int? = null,
    var price: String? = null,
    var quantity: String? = null,
    var amount: String? = null,
    var executedQuantity: String? = null,
    var executedAmount: String? = null,
    var executedPercent: String? = null,
    var executedAvgPrice: String? = null,
    var fee: String? = null,
    var status: Int? = null,
    var type: Int? = null,
    var createTime: Long? = null,
    var address: String? = null,
    var orderHash: String? = null
) {
    companion object {
        fun fromPb(from: DexPushMessage.OrderProto) =
            Order(
                orderId = from.orderHash,
                symbol = from.symbol,
                tradeTokenSymbol = from.tradeTokenSymbol,
                quoteTokenSymbol = from.quoteTokenSymbol,
                tradeToken = from.tradeToken,
                quoteToken = from.quoteToken,
                side = from.side,
                price = from.price,
                quantity = from.quantity,
                amount = from.amount,
                executedQuantity = from.executedQuantity,
                executedAmount = from.executedAmount,
                executedPercent = from.executedPercent,
                executedAvgPrice = from.executedAvgPrice,
                fee = from.fee,
                status = from.status,
                type = from.type,
                createTime = from.createTime,
                address = from.address,
                orderHash = from.orderId
            )
    }

    override fun hashCode(): Int {
        return orderId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return hashCode() == other.hashCode()
    }
}

@Keep
data class OrderMiningSetting(
    val buyRangeMin: String,
    val buyRangeMax: String,
    val sellRangeMin: String,
    val sellRangeMax: String,
    val buyMoreRatio: String,
    val sellMoreRatio: String
)

@Keep
data class StakeList(
    val stakeAmount: String?,
    val beneficiary: String?,
    val expirationHeight: String?,
    val expirationTime: Int?,
    val isDelegated: Boolean?,
    val delegateAddress: String?,
    val stakeAddress: String?,
    val bid: Int?,
    val id: String?
)

@Keep
data class PlaceOrderInfo(
    var available: String?,
    var minTradeAmount: String?,
    var feeRate: Int?,
    var side: Boolean?,
    var isVIP: Boolean?,
    var isSVIP: Boolean?,
    var isInvited: Boolean?
)

@Keep
data class VIPStakeInfoList(
    val totalStakeAmount: String?,
    val totalStakeCount: String?,
    val stakeList: List<StakeList>?
)

@Keep
data class Klines(
    val t: MutableList<Long>,
    val c: MutableList<String>,
    val p: MutableList<String>,
    val h: MutableList<String>,
    val l: MutableList<String>,
    val v: MutableList<String>
) {
    fun clear() {
        t.clear()
        c.clear()
        p.clear()
        h.clear()
        l.clear()
        v.clear()
    }
}

@Keep
data class Tradepair(
    var tradeTokenDetail: TradeTokenDetail? = null,
    var quoteTokenDetail: QuoteTokenDetail? = null,
    var operatorInfo: OperatorInfo? = null
)

@Keep
data class Overview(
    var en: String? = null,
    var zh: String? = null
)

@Keep
data class Policy(
    var en: String? = null,
    var zh: String? = null
)

@Keep
data class Links(
    var website: List<String>? = null,
    var twitter: List<String>? = null,
    var whitepaper: List<String>? = null,
    var facebook: List<String>? = null,
    var explorer: List<String>? = null
)

@Keep
data class MappedToken(
    var symbol: String? = null,
    var name: String? = null,
    var tokenCode: String? = null,
    var platform: String? = null,
    var tokenAddress: String? = null,
    var tokenIndex: Any? = null,
    var icon: String? = null,
    var decimal: Int? = null
)

@Keep
data class Gateway(
    var name: String? = null,
    var icon: String? = null,
    var policy: Policy? = null,
    var overview: Overview? = null,
    var links: Links? = null,
    var support: String? = null,
    var serviceSupport: String? = null,
    var isOfficial: Boolean? = null,
    var level: Int? = null,
    var website: String? = null,
    var mappedToken: MappedToken? = null,
    var url: String? = null
)


@Keep
data class QuoteTokenDetail(
    var tokenId: String? = null,
    var name: String? = null,
    var symbol: String? = null,
    var originalSymbol: String? = null,
    var totalSupply: String? = null,
    var publisher: String? = null,
    var tokenDecimals: Int? = null,
    var tokenAccuracy: String? = null,
    var publisherDate: Int? = null,
    var reissue: Int? = null,
    var urlIcon: String? = null,
    var gateway: Gateway? = null,
    val links: Map<String, List<String>>?,
    var overview: Overview? = null
)

@Keep
data class TradeTokenDetail(
    var tokenId: String? = null,
    var name: String? = null,
    var symbol: String? = null,
    var originalSymbol: String? = null,
    var totalSupply: String? = null,
    var publisher: String? = null,
    var tokenDecimals: Int? = null,
    var tokenAccuracy: String? = null,
    var publisherDate: Int? = null,
    var reissue: Int? = null,
    var urlIcon: String? = null,
    var gateway: Gateway? = null,
    val links: Map<String, List<String>>?,
    var overview: Overview? = null
)

@Keep
data class QueryPlatformTokensReq(
    val tokenAddresses: List<String>,
    val platformSymbol: String = "VITE"
) {
    companion object {
        fun create(tti: String) = QueryPlatformTokensReq(
            tokenAddresses = listOf(tti)
        )
    }
}

@Keep
data class ExchangeTokenRate(
    val tokenId: String,
    val tokenSymbol: String,
    val usdRate: Double,
    val cnyRate: Double,
    val rubRate: BigDecimal,
    val krwRate: BigDecimal,
    val tryRate: BigDecimal,
    val btcRate: BigDecimal,
    val vndRate: BigDecimal,
    val eurRate: BigDecimal,
    val gbpRate: BigDecimal,
) {
    fun getRate(): BigDecimal {
        return when (ViteConfig.get().currentCurrency()) {
            CURRENCY_CNY -> cnyRate.toBigDecimal()
            CURRENCY_USD -> usdRate.toBigDecimal()
            CURRENCY_VND -> vndRate
            CURRENCY_KRW -> krwRate
            CURRENCY_RUB -> rubRate
            CURRENCY_TRY -> tryRate
            CURRENCY_EUR -> eurRate
            CURRENCY_GBP -> gbpRate
            else -> BigDecimal.ZERO
        }
    }
}


@Keep
data class DexDepositWithdrawRecordRes(
    val total: Int,
    val record: List<DexDepositWithdrawRecord>
)

@Keep
data class DexDepositWithdrawRecord(
    val time: Long,
    val tokenSymbol: String,
    val amount: String,
    val type: Int
) {
    companion object {
        const val DEPOSIT = 1
        const val WITHDRAW = 2
    }
}

@Keep
data class RewardPledgeFullStat(
    val pledgeAmount: String?
)


class ViteXApiException : Exception {
    val code: Int?
    val msg: String?
    val isNullResp: Boolean?

    constructor(code: Int?, msg: String?) : super("code $code msg:$msg") {
        this.code = code
        this.msg = msg
        isNullResp = null
    }

    constructor() : super("null resp") {
        this.code = null
        this.msg = null
        isNullResp = true
    }

    fun toReadableString(): String {
        return msg ?: code?.toString() ?: "null resp"
    }

    fun isNullResp() = isNullResp == true
}


interface VitexApi {

    companion object {
        fun getApi() = HttpClient.vitexNetwork()
            .create(VitexApi::class.java)

        fun getHideSymbols():
                Observable<HiddenMarkets> {
            return getApi().getHideSymbols().applyIoScheduler()
        }

        fun get24HPriceChangeByCategory(quoteTokenCategory: String):
                Observable<List<TickerStatistics>> {
            return getApi().get24HPriceChangeByCategory(quoteTokenCategory).applyIoScheduler()
                .processViteBase()
        }

        fun get24HPriceChangeBySymbols(symbols: List<String>):
                Observable<List<TickerStatistics>> {
            return getApi().get24HPriceChangeBySymbols(symbols).applyIoScheduler()
                .processViteBase()
        }

        fun getTokenExchangeRate(symbols: List<String>):
                Observable<List<ExchangeTokenRate>> {
            return getApi().getTokenExchangeRate(symbols).applyIoScheduler()
                .processViteBase()
        }

        fun getAllOperatorInfo():
                Observable<List<OperatorInfo>> {
            return getApi().getAllOperatorInfo(emptyList()).applyIoScheduler()
                .processViteBase()
        }

        fun getMiningSetting():
                Observable<MiningSetting> {
            return getApi().getMiningSetting().applyIoScheduler()
                .processViteBase()
        }

        fun getTrades(symbol: String):
                Observable<TradeList> {
            return getApi().getTrades(symbol).applyIoScheduler()
                .processViteBase()
        }

        fun getOpenOrders(
            address: String,
            tradeTokenSymbol: String?,
            quoteTokenSymbol: String?,
            symbol: String?,
            offset: Int?,
            limit: Int?
        ): Observable<Orders> {
            return getApi().getOpenOrders(
                address = address,
                tradeTokenSymbol = tradeTokenSymbol,
                quoteTokenSymbol = quoteTokenSymbol,
                symbol = symbol,
                offset = offset,
                limit = limit
            ).applyIoScheduler()
                .processViteBase()
        }

        fun getDepth(
            symbol: String,
            step: Int? = null,
            limit: Int? = null
        ): Observable<DepthList> {
            return getApi().getDepth(
                symbol = symbol,
                step = step,
                limit = limit
            ).applyIoScheduler()
                .processViteBase()
        }

        fun getOperatorTradepair(
            tradeToken: String,
            quoteToken: String
        ): Observable<Tradepair> {
            return getApi().getOperatorTradepair(tradeToken, quoteToken).applyIoScheduler()
                .processViteBase()
        }

        fun getLimit(): Observable<ExchangeLimits> {
            return getApi().getLimit().applyIoScheduler().processViteBase()
        }

        fun getKlines(
            symbol: String,
            interval: String,
            limit: Int? = null,
            startTime: Long? = null,
            endTime: Long? = null
        ): Observable<Klines> {
            return getApi().getKlines(symbol, interval, limit, startTime, endTime)
                .applyIoScheduler()
                .processViteBase()
        }

        fun getMarketsClosed(): Observable<List<ClosedMarket>> {
            return getApi().getMarketsClosed()
                .applyIoScheduler()
                .processViteBase()
        }

        fun getDexTokens(): Observable<List<NormalTokenInfo>> {
            return getApi().getDexTokens()
                .processViteBase()
        }

        fun getDepositWithdrawRecords(
            address: String,
            tokenId: String,
            offset: Int,
            limit: Int
        ): Observable<DexDepositWithdrawRecordRes> {
            return getApi().getDepositWithdrawRecords(address, tokenId, offset, limit)
                .subscribeOn(Schedulers.io())
                .processViteBase()
        }
    }

    @GET("api/v1/cryptocurrency/info/default")
    fun getDefaultTokenInfos(): Observable<ViteNormalBaseResponse<Map<String, List<NormalTokenInfo>>>>

    @GET("api/v1/cryptocurrency/info/search")
    fun fuzzyQuery(@Query("fuzzy") fuzzy: String): Observable<ViteNormalBaseResponse<Map<String, List<NormalTokenInfo>>>>

    @POST("api/v1/cryptocurrency/info/assign")
    fun batchQueryTokenDetail(@Body tokenCodes: List<String>): Observable<ViteNormalBaseResponse<List<NormalTokenInfo>>>


    @POST("api/v1/cryptocurrency/info/platform/query")
    fun queryPlatformToken(@Body queryPlatformTokensReq: QueryPlatformTokensReq)
            : Observable<ViteNormalBaseResponse<List<NormalTokenInfo>>>


    @POST("api/v1/cryptocurrency/rate/assign")
    fun batchQueryTokenPrice(@Body tokenCodes: List<String>): Observable<ViteNormalBaseResponse<List<TokenPrice>>>

    @POST("api/v1/cryptocurrency/info/detail")
    fun batchQueryTokenInfoDetail(@Body tokenCodes: List<String>):
            Observable<ViteNormalBaseResponse<List<CCurrencyInfoDetail>>>


    @GET("api/v2/ticker/24hr")
    fun get24HPriceChangeByCategory(@Query("quoteTokenCategory") quoteTokenCategory: String):
            Observable<ViteNormalBaseResponse<List<TickerStatistics>>>

    @GET("api/v2/ticker/24hr")
    fun get24HPriceChangeBySymbols(@Query("symbols") symbols: List<String>):
            Observable<ViteNormalBaseResponse<List<TickerStatistics>>>

    @GET("api/v2/exchange-rate")
    fun getTokenExchangeRate(@Query("tokenSymbols") tokenSymbols: List<String>):
            Observable<ViteNormalBaseResponse<List<ExchangeTokenRate>>>

    @POST("api/v1/operator/info")
    fun getAllOperatorInfo(@Body tokenSymbols: List<String>):
            Observable<ViteNormalBaseResponse<List<OperatorInfo>>>

    @GET("api/v1/operator/tradepair")
    fun getOperatorTradepair(
        @Query("tradeToken") tradeToken: String,
        @Query("quoteToken") quoteToken: String
    ): Observable<ViteNormalBaseResponse<Tradepair>>

    @GET("api/v1/mining/setting")
    fun getMiningSetting():
            Observable<ViteNormalBaseResponse<MiningSetting>>

    @GET("api/v2/trades/all")
    fun getTrades(@Query("symbol") symbol: String):
            Observable<ViteNormalBaseResponse<TradeList>>

    @GET("api/v2/orders/open")
    fun getOpenOrders(
        @Query("address") address: String,
        @Query("quoteTokenSymbol") quoteTokenSymbol: String?,
        @Query("tradeTokenSymbol") tradeTokenSymbol: String?,
        @Query("symbol") symbol: String?,
        @Query("offset") offset: Int?,
        @Query("limit") limit: Int?
    ): Observable<ViteNormalBaseResponse<Orders>>

    @GET("api/v2/depth/all")
    fun getDepth(
        @Query("symbol") symbol: String,
        @Query("step") step: Int?,
        @Query("limit") limit: Int?
    ): Observable<ViteNormalBaseResponse<DepthList>>

    @GET("api/v2/klines")
    fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int?,
        @Query("startTime") startTime: Long?,
        @Query("endTime") endTime: Long?
    ): Observable<ViteNormalBaseResponse<Klines>>

    @GET("api/v2/limit")
    fun getLimit(): Observable<ViteNormalBaseResponse<ExchangeLimits>>

    @GET("api/v2/markets/closed")
    fun getMarketsClosed(): Observable<ViteNormalBaseResponse<List<ClosedMarket>>>

    @GET("api/v1/cryptocurrency/dex/tokens")
    fun getDexTokens(): Observable<ViteNormalBaseResponse<List<NormalTokenInfo>>>

    @GET("https://static.vite.net/web-wallet-1257137467/uiController/main.json")
    fun getHideSymbols(): Observable<HiddenMarkets>

    @GET("api/v2/deposit-withdraw")
    fun getDepositWithdrawRecords(
        @Query("address") address: String,
        @Query("tokenId") tokenId: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Observable<ViteNormalBaseResponse<DexDepositWithdrawRecordRes>>

    @GET("reward/pledge/full/stat")
    // stakeForFullNode
    fun rewardPledgeFullStat(
        @Query("address") address: String
    ): Observable<ViteNormalBaseResponse<RewardPledgeFullStat>>

}


interface SuspendableVitexApi {

    companion object {
        fun getApi() = HttpClient.vitexNetworkSuspendable()
            .create(SuspendableVitexApi::class.java)


        suspend fun getOpenOrders(
            address: String,
            tradeTokenSymbol: String?,
            quoteTokenSymbol: String?,
            symbol: String?,
            offset: Int?,
            limit: Int?
        ): Orders {
            val resp = getApi().getOpenOrders(
                address = address,
                tradeTokenSymbol = tradeTokenSymbol,
                quoteTokenSymbol = quoteTokenSymbol,
                symbol = symbol,
                offset = offset,
                limit = limit
            )
            when {
                resp.code != 0 -> throw ViteXApiException(resp.code, resp.msg)
                resp.data == null -> throw ViteXApiException()
                else -> return resp.data
            }
        }

        suspend fun getAllOrders(
            address: String,
            symbol: String?,
            quoteTokenSymbol: String?,
            tradeTokenSymbol: String?,
            startTime: Long?,
            endTime: Long?,
            side: Int?,
            status: Int?,
            offset: Int?,
            limit: Int?,
            total: Int?
        ): Orders {
            val resp = getApi().getAllOrders(
                address,
                symbol,
                quoteTokenSymbol,
                tradeTokenSymbol,
                startTime,
                endTime,
                side,
                status,
                offset,
                limit,
                total
            )
            when {
                resp.code != 0 -> throw ViteXApiException(resp.code, resp.msg)
                resp.data == null -> throw ViteXApiException()
                else -> return resp.data
            }
        }
    }


    @GET("api/v2/orders/open")
    suspend fun getOpenOrders(
        @Query("address") address: String,
        @Query("quoteTokenSymbol") quoteTokenSymbol: String?,
        @Query("tradeTokenSymbol") tradeTokenSymbol: String?,
        @Query("symbol") symbol: String?,
        @Query("offset") offset: Int?,
        @Query("limit") limit: Int?
    ): ViteNormalBaseResponse<Orders>

    @GET("api/v2/orders")
    suspend fun getAllOrders(
        @Query("address") address: String,
        @Query("symbol") symbol: String?,
        @Query("quoteTokenSymbol") quoteTokenSymbol: String?,
        @Query("tradeTokenSymbol") tradeTokenSymbol: String?,
        @Query("startTime") startTime: Long?,
        @Query("endTime") endTime: Long?,
        @Query("side") side: Int?,
        @Query("status") status: Int?,
        @Query("offset") offset: Int?,
        @Query("limit") limit: Int?,
        @Query("total") total: Int?
    ): ViteNormalBaseResponse<Orders>
}
