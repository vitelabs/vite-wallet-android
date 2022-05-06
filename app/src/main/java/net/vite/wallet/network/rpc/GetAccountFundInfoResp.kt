package net.vite.wallet.network.rpc

import androidx.annotation.Keep
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.network.toLocalReadableText
import java.math.BigDecimal
import java.math.BigInteger

@Keep
data class GetAccountFundInfoResp(
    val tokenInfo: ViteChainTokenInfo,
    val available: String,
    val locked: String,
    val vxLocked: String?,
    val vxUnlocking: String?,
    val cancellingStake: String?
) {
    fun getAvailableBase() =
        tokenInfo.smallestToBaseUnit(available.toBigDecimalOrNull() ?: BigDecimal.ZERO)

    fun getLockedBase() =
        tokenInfo.smallestToBaseUnit(locked.toBigDecimalOrNull() ?: BigDecimal.ZERO)

    fun getVxLockedBase() =
        tokenInfo.smallestToBaseUnit(vxLocked?.toBigDecimalOrNull() ?: BigDecimal.ZERO)

    fun getVxUnlockingBase() =
        tokenInfo.smallestToBaseUnit(vxUnlocking?.toBigDecimalOrNull() ?: BigDecimal.ZERO)

    fun getCancellingStakeBase() =
        tokenInfo.smallestToBaseUnit(cancellingStake?.toBigDecimalOrNull() ?: BigDecimal.ZERO)

    fun getOrderLockedAndAvaliableBase() = getLockedBase() + getAvailableBase()

    fun getAllBase() =
        getOrderLockedAndAvaliableBase() + getVxUnlockingBase() + getVxLockedBase() + getCancellingStakeBase() +
                ViteTokenInfo.smallestToBaseUnit(
                    (if (tokenInfo.tokenId == ViteTokenInfo.tokenId) {
                        ViteAccountInfoPoll.myLatestViteAccountInfo()?.let {
                            (it.stakeForDexVip ?: BigInteger.ZERO) +
                                    (it.stakeForDexMining ?: BigInteger.ZERO)
                        } ?: BigInteger.ZERO
                    } else {
                        BigInteger.ZERO
                    }).toBigDecimal()
                )

    fun getAvailableBaseText(): String {
        return getAvailableBase().toLocalReadableText(8)
    }

}