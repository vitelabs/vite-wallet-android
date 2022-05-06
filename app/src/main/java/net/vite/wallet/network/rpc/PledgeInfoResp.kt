package net.vite.wallet.network.rpc

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.min

@Keep
data class PledgeInfo(
    @SerializedName("amount") val amount: BigInteger?,
    @SerializedName("withdrawHeight") val withdrawHeight: Long?,
    @SerializedName("beneficialAddr") val addr: String?,
    @SerializedName("id") val id: String?,
    @SerializedName("withdrawTime") val withdrawTime: Long?
) {
    fun getAmountReadableText(scale: Int): String {
        return amount?.toBigDecimal()?.divide(
            BigDecimal.TEN.pow(ViteTokenInfo.decimals ?: 0),
            min(scale, ViteTokenInfo.decimals ?: 0),
            RoundingMode.DOWN
        ).toString()
    }

    fun getMsWithdrawTime() = (withdrawTime ?: 0) * 1000
}

@Keep
data class PledgeInfoResp(
    @SerializedName("totalPledgeAmount") var totalPledgeAmount: String? = "",
    @SerializedName("totalCount") var totalCount: Long? = 0,
    @SerializedName("pledgeInfoList") var pledgeInfoList: List<PledgeInfo>? = emptyList()
)