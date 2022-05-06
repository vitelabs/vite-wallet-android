package net.vite.wallet.network.http

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.rpc.RpcException
import net.vite.wallet.network.rpc.RpcResponse
import retrofit2.http.Body
import retrofit2.http.POST
import java.math.BigInteger

@Keep
data class Request(
    val id: Int,
    val method: String,
    val params: Any?,
    val jsonrpc: String = "2.0"
)

@Keep
data class Response(
    val result: JsonElement?,
    val id: Int?,
    val jsonrpc: String,
    val error: RpcException?
)


interface JSONRCApi {

    companion object {
        fun call(method: String, params: Any?) =
            getApi().p(
                Request(
                    id = 0,
                    method = method,
                    params = params
                )
            )

        fun getApi() = HttpClient.jsonRpc2Network()
            .create(JSONRCApi::class.java)
    }

    @POST(".")
    fun p(@Body request: Request): Observable<Response>

}

@Keep
data class SBP(
    val stakeAddress: String?,
    val stakeAmount: String?
)

object MyRPC {

    fun contract_getSBPStakeAll(address: String): Observable<RpcResponse<BigInteger>> {
        return JSONRCApi.call("contract_getSBPList", listOf(address))
            .flatMap {
                if (it.error != null) {
                    Observable.just(null, it.error)
                }

                val sum = it.result?.let {
                    val list: List<SBP> =
                        Gson().fromJson(it, object : TypeToken<List<SBP>>() {}.type)
                    list
                }?.filter { it.stakeAddress == address }?.fold(BigInteger.ZERO) { acc, SBP ->
                    acc.add(SBP.stakeAmount?.toBigIntegerOrNull() ?: BigInteger.ZERO)
                } ?: BigInteger.ZERO
                Observable.just(RpcResponse(sum))

            }.applyIoScheduler()
    }

    fun dex_getCurrentMiningStakingAmountByAddress(address: String): Observable<RpcResponse<BigInteger>> {
        return JSONRCApi.call("dex_getCurrentMiningStakingAmountByAddress", listOf(address))
            .flatMap {
                if (it.error != null) {
                    Observable.just(null, it.error)
                }
                val sum = it.result?.let {
                    val result: Map<String, String> =
                        Gson().fromJson(it, object : TypeToken<Map<String, String>>() {}.type)
                    result
                }?.values?.map { it.toBigIntegerOrNull() }
                    ?.fold(BigInteger.ZERO) { acc, bigInteger ->
                        acc.add(bigInteger ?: BigInteger.ZERO)
                    } ?: BigInteger.ZERO
                Observable.just(RpcResponse(sum))
            }.applyIoScheduler()

    }
}
