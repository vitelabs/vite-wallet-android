package net.vite.wallet.network.http.nut

import androidx.annotation.Keep
import io.reactivex.Observable
import net.vite.wallet.network.http.HttpClient
import retrofit2.http.GET
import retrofit2.http.Path

@Keep
data class NutConfig(
    val isShowAirDrop: Int?,
    val tags: List<Tag>?
)

@Keep
data class Tag(
    val list: List<NutItem>?,
    val tag: String?
)

@Keep
data class NutItem(
    val createTime: Long?,
    val desc: String?,
    val imgUrl: String?,
    val isExpire: Int?,
    val skipUrl: String?,
    val source: Int?,
    val title: String?
)

interface NutConfigApi {
    companion object {

        fun getApi() = HttpClient.discoverNetwork().create(NutConfigApi::class.java)

        fun pull(isChinese: Boolean): Observable<NutConfig> {
            return getApi().pull(
                if (isChinese) {
                    "discover_zh.json"
                } else {
                    "discover_en.json"
                }
            )
        }
    }

    @GET("discover/{language}")
    fun pull(@Path("language") language: String): Observable<NutConfig>

}