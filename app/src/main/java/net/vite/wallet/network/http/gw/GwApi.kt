package net.vite.wallet.network.http.gw

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import net.vite.wallet.network.http.HttpClient
import retrofit2.http.Body
import retrofit2.http.POST


@Keep
data class GwBaseResponse<T>(
    val code: Int?,
    val msg: String?,
    val data: T? = null
)

@Keep
data class GwConvertBindBody(
    @SerializedName("pub_key") val pubKey: String,
    @SerializedName("eth_tx_hash") val ethTxHash: String,
    @SerializedName("eth_addr") val ethAddr: String,
    @SerializedName("vite_addr") val viteAddr: String,
    @SerializedName("value") val value: String,
    @SerializedName("signature") val signature: String? = null
)


interface GwApi {

    companion object {
        fun getApi() = HttpClient.normalGwNetwork()
            .create(GwApi::class.java)
    }

    @POST("gw/bind")
    fun convertBind(@Body gwExchangeBindBody: GwConvertBindBody): Observable<GwBaseResponse<Unit>>

}