package net.vite.wallet.network.http.gw

import android.app.Activity
import android.app.Dialog
import androidx.annotation.Keep
import io.reactivex.Observable
import net.vite.wallet.R
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.http.HttpClient
import retrofit2.http.GET
import retrofit2.http.Query

class OverChainApiException : Exception {
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

    fun showErrorDialog(activity: Activity, onConfirm: () -> Unit = {}) {
        TextTitleNotifyDialog(activity).apply {
            setMessage(msg ?: "unknown error")
            setBottom(R.string.confirm) {
                it.dismiss()
                onConfirm()
            }
            show()
        }
    }

}


@Keep
data class DepositInfoResp(
    val depositAddress: String?,
    val labelName: String?,
    val label: String?,
    val minimumDepositAmount: String?,
    val confirmationCount: Int?,
    val noticeMsg: String?
) {
    fun needLabel(): Boolean {
        return labelName != null
    }
}


@Keep
data class DepositRecordsResp(
    val totalCount: Int?,
    val inTxExplorerFormat: String?,
    val outTxExplorerFormat: String?,
    val depositRecords: List<DepositRecord>?
)


@Keep
data class WithdrawRecordsResp(
    val totalCount: Int?,
    val inTxExplorerFormat: String?,
    val outTxExplorerFormat: String?,
    val withdrawRecords: List<WithdrawRecord>?
)

@Keep
data class MetaInfoResp(
    val type: Int?,
    val depositState: String?,
    val withdrawState: String?
) {
    companion object {
        const val OPEN = "OPEN"
        const val MAINTAIN = "MAINTAIN"
        const val CLOSED = "CLOSED"
    }

    fun showCheckDialog(activity: Activity, f: (dialog: Dialog) -> Unit) {
        TextTitleNotifyDialog(activity).apply {
            val message = when (depositState) {
                CLOSED -> R.string.crosschain_gw_meta_closed
                MAINTAIN -> R.string.crosschain_gw_meta_maintain
                else -> R.string.crosschain_gw_meta_unknown
            }
            setMessage(message)
            setBottom(R.string.confirm, f)
            show()
        }
    }
}

@Keep
data class WithdrawRecord(
    val inTxHash: String?,
    val inTxConfirmedCount: String?,
    val inTxConfirmationCount: String?,
    val outTxHash: String?,
    val amount: String?,
    val fee: String?,
    val state: String?,
    val dateTime: String?,
    val tokenAddress: String? = null,
    val rawResponse: WithdrawRecordsResp? = null
) {
    companion object {
        const val OPPOSITE_PROCESSING = "OPPOSITE_PROCESSING"
        const val OPPOSITE_CONFIRMED = "OPPOSITE_CONFIRMED"
        const val TOT_PROCESSING = "TOT_PROCESSING"
        const val TOT_CONFIRMED = "TOT_CONFIRMED"
        const val TOT_EXCEED_THE_LIMIT = "TOT_EXCEED_THE_LIMIT"
        const val WRONG_WITHDRAW_ADDRESS = "WRONG_WITHDRAW_ADDRESS"
    }
}

@Keep
data class DepositRecord(
    val inTxHash: String?,
    val inTxConfirmedCount: String?,
    val inTxConfirmationCount: String?,
    val outTxHash: String?,
    val amount: String?,
    val fee: String?,
    val state: String?,
    val dateTime: String?,
    val tokenAddress: String? = null,
    val rawResponse: DepositRecordsResp? = null
) {
    companion object {
        const val OPPOSITE_PROCESSING = "OPPOSITE_PROCESSING"
        const val OPPOSITE_CONFIRMED = "OPPOSITE_CONFIRMED"
        const val TOT_PROCESSING = "TOT_PROCESSING"
        const val TOT_CONFIRMED = "TOT_CONFIRMED"
        const val BELOW_MINIMUM = "BELOW_MINIMUM"
        const val OPPOSITE_CONFIRMED_FAIL = "OPPOSITE_CONFIRMED_FAIL"
    }
}

@Keep
data class WithdrawInfoResp(
    val minimumWithdrawAmount: String?,
    val maximumWithdrawAmount: String?,
    val gatewayAddress: String?,
    val labelName: String?,
    val noticeMsg: String?
) {
    fun needLabel(): Boolean {
        return labelName != null
    }
}

@Keep
data class WithdrawFeeResp(
    val fee: String?
)

@Keep
data class WithdrawIsValidAddrResp(
    val isValidAddress: Boolean?
)


fun <T> Observable<GwBaseResponse<T>>.processGwBase() = flatMap {
    when {
        it.code != 0 -> Observable.error(OverChainApiException(it.code, it.msg))
        it.data == null -> Observable.error(OverChainApiException())
        else -> Observable.just(it.data)
    }
}


interface GwCrosschainApi {

    companion object {
        private fun getClient(url: String) =
            HttpClient.normalGwOverchainNetwork(url).create(GwCrosschainApi::class.java)

        fun metaInfo(tokenId: String, gwUrl: String) =
            getClient(gwUrl).metaInfo(tokenId).applyIoScheduler()
                .processGwBase()

        fun depositInfo(tokenId: String, walletAddress: String, gwUrl: String) =
            getClient(gwUrl).depositInfo(
                tokenId,
                walletAddress
            ).applyIoScheduler().processGwBase()

        fun withdrawInfo(tokenId: String, walletAddress: String, gwUrl: String) =
            getClient(gwUrl).withdrawInfo(
                tokenId,
                walletAddress
            ).applyIoScheduler().processGwBase()

        fun withdrawFee(tokenId: String, walletAddress: String, amount: String, gwUrl: String) =
            getClient(gwUrl).withdrawFee(
                tokenId,
                walletAddress,
                amount
            ).applyIoScheduler().processGwBase()

        fun withdrawAddressVerification(
            tokenId: String,
            withdrawAddress: String,
            label: String?,
            gwUrl: String
        ) =
            getClient(gwUrl).withdrawAddressVerification(
                tokenId,
                withdrawAddress,
                label
            ).applyIoScheduler().processGwBase()


        fun withdrawRecords(
            tokenId: String,
            withdrawAddress: String,
            pageNum: Int,
            pageSize: Int,
            gwUrl: String
        ) = getClient(gwUrl).withdrawRecords(
            tokenId,
            withdrawAddress,
            pageNum,
            pageSize
        ).applyIoScheduler()

        fun depositRecords(
            tokenId: String,
            walletAddress: String,
            pageNum: Int,
            pageSize: Int,
            gwUrl: String
        ) = getClient(gwUrl).depositRecords(
            tokenId,
            walletAddress,
            pageNum,
            pageSize
        ).applyIoScheduler()
    }


    @GET("deposit-info")
    fun depositInfo(
        @Query("tokenId") tokenId: String,
        @Query("walletAddress") walletAddress: String
    ): Observable<GwBaseResponse<DepositInfoResp>>

    @GET("meta-info")
    fun metaInfo(
        @Query("tokenId") tokenId: String
    ): Observable<GwBaseResponse<MetaInfoResp>>


    @GET("deposit-records")
    fun depositRecords(
        @Query("tokenId") tokenId: String,
        @Query("walletAddress") walletAddress: String,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): Observable<GwBaseResponse<DepositRecordsResp>>


    @GET("withdraw-records")
    fun withdrawRecords(
        @Query("tokenId") tokenId: String,
        @Query("walletAddress") walletAddress: String,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): Observable<GwBaseResponse<WithdrawRecordsResp>>

    @GET("withdraw-info")
    fun withdrawInfo(
        @Query("tokenId") tokenId: String,
        @Query("walletAddress") walletAddress: String
    ): Observable<GwBaseResponse<WithdrawInfoResp>>

    @GET("withdraw-fee")
    fun withdrawFee(
        @Query("tokenId") tokenId: String,
        @Query("walletAddress") walletAddress: String,
        @Query("amount") amount: String,
        @Query("containsFee") containsFee: Boolean = false
    ): Observable<GwBaseResponse<WithdrawFeeResp>>


    @GET("withdraw-address/verification")
    fun withdrawAddressVerification(
        @Query("tokenId") tokenId: String,
        @Query("withdrawAddress") withdrawAddress: String,
        @Query("label") label: String?
    ): Observable<GwBaseResponse<WithdrawIsValidAddrResp>>


}