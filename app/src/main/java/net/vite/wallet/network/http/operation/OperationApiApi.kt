package net.vite.wallet.network.http.operation

import androidx.annotation.Keep
import io.reactivex.Observable
import net.vite.wallet.network.http.HttpClient
import retrofit2.http.GET
import retrofit2.http.Query

@Keep
data class ExchangeBannerResp(
    val link: String,
    val image: ExchangeBannerImageResp
)

@Keep
data class VCConfigsResp(
    val created_at: String,
    val `data`: Data,
    val id: Int,
    val updated_at: String
)

@Keep
data class Data(
    val contract: List<String>,
    val dexPostContractPairs: List<String>
)

@Keep
data class ExchangeBannerImageResp(
    val url: String
)

interface OperationApi {

    companion object {
        fun getApi() = HttpClient.exhangeBannerNetwork()
            .create(OperationApi::class.java)

        fun pullExchangeBanner(language: String): Observable<List<ExchangeBannerResp>> {
            return getApi().pullExchangeBanner(language)
        }

        fun pullVCConfigs(): Observable<VCConfigsResp> {
            return getApi().pullVCConfigs()
        }
    }

    @GET("marketbanners")
    fun pullExchangeBanner(@Query("language") language: String, @Query("_sort") _sort: String = "position:asc")
            : Observable<List<ExchangeBannerResp>>

    @GET("vcconfigs/1")
    fun pullVCConfigs()
            : Observable<VCConfigsResp>


}
