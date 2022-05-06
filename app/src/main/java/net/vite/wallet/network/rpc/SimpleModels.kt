package net.vite.wallet.network.rpc

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.math.BigInteger

/**
 * {
"current": "1575000",
"quotaPerSnapshotBlock": "21000",
"currentUt": "75",
"utpe": "75",
"pledgeAmount": "134000000000000000000"
}
 */
@Keep
data class GetPledgeQuotaResp(
    val current: String?,
    val quotaPerSnapshotBlock: String?,
    val currentUt: String?,
    val utpe: String?,
    val pledgeAmount: String?
)

@Keep
data class CandidateItem(
    val name: String = "",
    val nodeAddr: String = "",
    val voteNum: String = "0",
    var rank: Int = 0
)

@Keep
data class VoteInfo(
    @SerializedName("nodeName") val name: String = "",
    @SerializedName("nodeStatus") val nodeStatus: Int = 0,
    @SerializedName("balance") val balance: BigInteger = BigInteger.ZERO
) {
    fun isInvalid() = nodeStatus == 0
}

@Keep
data class CalcPowDifficultyReq(
    val blockType: Byte,
    val data: String?,
    val prevHash: String?,
    val selfAddr: String,
    val toAddr: String?,
    val usePledgeQuota: Boolean
) {
    companion object {
        fun fromAccountBlock(accountBlock: AccountBlock): CalcPowDifficultyReq {
            return CalcPowDifficultyReq(
                blockType = accountBlock.blockType ?: 0,
                data = accountBlock.data,
                prevHash = accountBlock.prevHash,
                selfAddr = accountBlock.accountAddress ?: "",
                toAddr = accountBlock.toAddress ?: "",
                usePledgeQuota = true
            )
        }
    }
}

@Keep
data class CalcPowDifficultyResp(
    val difficulty: String?,
    val quotaRequired: String?,
    val utRequired: String?,
    val isCongestion: Boolean?
)

@Keep
data class CalcQuotaRequiredReq(
    val selfAddr: String,
    val blockType: Byte,
    val toAddr: String,
    val data: String // base64
)

@Keep
data class CalcQuotaRequiredResp(
    val quotaRequired: String?,
    val utRequired: String?
)
