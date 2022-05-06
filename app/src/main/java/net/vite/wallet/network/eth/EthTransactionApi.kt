package net.vite.wallet.network.eth

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.http.HttpClient
import net.vite.wallet.network.toLocalReadableText
import retrofit2.http.GET
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

@Keep
interface EthTransactionApi {
    companion object {
        fun getApi() = HttpClient.ethTransactionNetwork().create(EthTransactionApi::class.java)

        fun getTransactionsSync(
            address: String,
            page: Int,
            offset: Int
        ): Observable<EthTransactions> {
            return getApi().getTransactions(address = address, page = page, offset = offset)
        }

        fun getTransactions(
            address: String,
            page: Int,
            offset: Int
        ): Observable<EthTransactions> {
            return getApi().getTransactions(address = address, page = page, offset = offset)
                .applyIoScheduler()
        }

        fun getErc20Transactions(
            address: String,
            contractAddress: String,
            page: Int,
            offset: Int
        ): Observable<EthTransactions> {
            return getApi().getErc20Transactions(
                address = address,
                contractAddress = contractAddress,
                page = page,
                offset = offset
            ).applyIoScheduler()
        }

        fun getErc20TransactionsSync(
            address: String,
            contractAddress: String,
            page: Int,
            offset: Int
        ): Observable<EthTransactions> {
            return getApi().getErc20Transactions(
                address = address,
                contractAddress = contractAddress,
                page = page,
                offset = offset
            )
        }
    }

    @GET("api")
    fun getTransactions(
        @Query("address") address: String,
        @Query("page") page: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "txlist",
        @Query("module") module: String = "account",
        @Query("startblock") startblock: Long = 0,
        @Query("endblock") endblock: Long = 999999999,
        @Query("sort") sort: String = "desc"
    ): Observable<EthTransactions>

    @GET("api")
    fun getErc20Transactions(
        @Query("address") address: String,
        @Query("contractAddress") contractAddress: String,
        @Query("page") page: Int,
        @Query("offset") offset: Int,
        @Query("action") action: String = "tokentx",
        @Query("module") module: String = "account",
        @Query("startblock") startblock: Long = 0,
        @Query("endblock") endblock: Long = 999999999,
        @Query("sort") sort: String = "desc"
    ): Observable<EthTransactions>

}


@Keep
data class EthTransactions(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("result") val result: List<EthTransaction>
)

@Keep
data class EthTransaction(
    @SerializedName("blockNumber") val blockNumber: String,
    @SerializedName("timeStamp") val timeStamp: String,
    @SerializedName("hash") val hash: String,
    @SerializedName("nonce") val nonce: String,
    @SerializedName("blockHash") val blockHash: String,
    @SerializedName("transactionIndex") val transactionIndex: String,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("value") val value: String,
    @SerializedName("gas") val gas: String,
    @SerializedName("gasPrice") val gasPrice: String,
    @SerializedName("isError") val isError: String,
    @SerializedName("txreceipt_status") val txreceipt_status: String,
    @SerializedName("input") val input: String?,
    @SerializedName("contractAddress") val contractAddress: String,
    @SerializedName("cumulativeGasUsed") val cumulativeGasUsed: String,
    @SerializedName("gasUsed") val gasUsed: String,
    @SerializedName("confirmations") val confirmations: String,
    @SerializedName("tokenName") val tokenName: String?,
    @SerializedName("tokenSymbol") val tokenSymbol: String?,
    @SerializedName("tokenDecimal") val tokenDecimal: String?
) {
    fun getAmountReadableText(scale: Int, decimals: Int): String {
        return value?.toBigDecimal()?.divide(
            BigDecimal.TEN.pow(decimals),
            min(scale, decimals),
            RoundingMode.DOWN
        )?.toLocalReadableText(scale) ?: "0"
    }

    fun getGastReadableText(scale: Int, decimals: Int): String {
        return gasPrice?.toBigDecimal()?.times(gasUsed?.toBigDecimal())?.divide(
            BigDecimal.TEN.pow(decimals),
            min(scale, decimals),
            RoundingMode.DOWN
        )?.toLocalReadableText(scale) ?: "0"
    }

    fun getEthAmountReadableText(): String {
        return getAmountReadableText(4, tokenDecimal?.toIntOrNull() ?: 18)
    }
}