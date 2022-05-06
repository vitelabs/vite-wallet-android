package net.vite.wallet.network.http.growth

import io.reactivex.Observable
import net.vite.wallet.network.http.HttpClient
import net.vite.wallet.network.http.ViteNormalBaseResponse
import net.vite.wallet.network.http.processViteBaseWithoutData
import retrofit2.http.GET
import retrofit2.http.Query

interface DexInviteCodeApi {
    companion object {
        fun getApi() = HttpClient.normalGrwothNetwork()
            .create(DexInviteCodeApi::class.java)

        fun initAddress(address: String): Observable<Unit> {
            return getApi().initAddress(address).processViteBaseWithoutData()
        }
    }

    @GET("api/coin/v1/init")
    fun initAddress(@Query("address") address: String): Observable<ViteNormalBaseResponse<Unit>>

}