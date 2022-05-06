package net.vite.wallet.network.http.coinpurchase

import androidx.annotation.Keep
import io.reactivex.Observable
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.http.HttpClient
import net.vite.wallet.network.http.nut.GrowthBaseResponse
import net.vite.wallet.network.http.nut.processGrowthBase
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Keep
data class CoinPurchaseExchangeReq(
    val address: String,
    val hash: String,
    val market: String = "eth_vite"
)


@Keep
data class CoinPurchaseExchangeResp(
    val rightRate: String?,
    val sendHash: String?,
    val viteAmount: String?,
    val code: Int?
)

@Keep
data class Quota(
    val ctime: String?,
    val id: Int?,
    val quotaTotal: Double?,
    val quotaRest: Double?,
    val unitQuotaMax: Double?,
    val unitQuotaMin: Double?,
    val version: Int?
)

@Keep
data class ConvertRateResp(
    val quota: Quota?,
    val rightRate: Double?,
    val storeAddress: String?
)

@Keep
data class PurchaseRecordItem(
    val id: Int?,
    val address: String?,
    val market: String?,
    val xAmount: Double?,
    val viteAmount: Double?,
    val ratePrice: Double?,
    val receiveHash: String?,
    val sendHash: String?,
    val ctime: Long?
)

interface CoinPurchaseApi {
    companion object {

        fun getApi() = HttpClient.coinPurchaseNetWork()
            .create(CoinPurchaseApi::class.java)

        fun exchange(coinPurchaseExchangeReq: CoinPurchaseExchangeReq): Observable<CoinPurchaseExchangeResp> {
            return getApi().exchange(coinPurchaseExchangeReq).applyIoScheduler().processGrowthBase()
        }

        fun convertRate(address: String, market: String = "eth_vite")
                : Observable<ConvertRateResp> {
            return getApi().convertRate(address, market).applyIoScheduler().processGrowthBase()
        }

        fun history(
            address: String,
            pageIndex: Int,
            pageSize: Int,
            market: String = "eth_vite"
        ): Observable<List<PurchaseRecordItem>> {
            return getApi().history(address, pageIndex, pageSize, market).applyIoScheduler()
                .processGrowthBase()
        }

    }

    @POST("api/coin/convert/v1/exchange")
    fun exchange(@Body pushBindInfo: CoinPurchaseExchangeReq): Observable<GrowthBaseResponse<CoinPurchaseExchangeResp>>

    @GET("api/coin/convert/v1/convert_rate")
    fun convertRate(@Query("address") address: String, @Query("market") market: String)
            : Observable<GrowthBaseResponse<ConvertRateResp>>

    @GET("api/coin/convert/v1/history")
    fun history(
        @Query("address") address: String,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: Int,
        @Query("market") market: String
    ): Observable<GrowthBaseResponse<List<PurchaseRecordItem>>>
}