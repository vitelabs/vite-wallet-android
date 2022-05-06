package net.vite.wallet.balance.walletconnect.session

import net.vite.wallet.balance.walletconnect.taskdetail.WCConfirmInfo
import net.vite.wallet.balance.walletconnect.taskdetail.buildin.*
import net.vite.wallet.utils.toHex
import org.walletconnect.Session
import java.util.concurrent.CountDownLatch

class VcRequestTask(
    val tx: Session.MethodCall.VCTask,
    val confirmInfo: WCConfirmInfo,
    @Volatile
    var currentState: Int = Pending,
    var throwable: Throwable? = null,
    val completeLatch: CountDownLatch = CountDownLatch(1),
    val timepStamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val Success = 0
        const val AutoProcessing = 1
        const val WaitUserProcessing = 6
        const val Failed = 2
        const val Cancel = 3
        const val Pending = 4

        var autoSignContracts: java.util.ArrayList<String>? = null
    }

    fun isSupportAutoSign(): Boolean {
        if (autoSignContracts != null) {
            return (autoSignContracts?.contains(tx.sendTransaction?.block?.data?.sliceArray(0 until 4)?.toHex())
                ?: false) || DexPost.isSupportPair(confirmInfo)
        }

        return DexDeposit.isThisType(confirmInfo)
                || DexWithdraw.isThisType(confirmInfo)
                || DexBecomeVIP.isThisType(confirmInfo)
                || QuotaAcquire.isThisType(confirmInfo)
                || QuotaRegainStakefor.isThisType(confirmInfo)
                || QuotaRegainStakeforNew.isThisType(confirmInfo)
                || DexCancelStakeById.isThisType(confirmInfo)
                || DexStakingAsMining.isThisType(confirmInfo)
                || Vote.isThisType(confirmInfo)
                || VoteRevoke.isThisType(confirmInfo)
                || DexNewInviter.isThisType(confirmInfo)
                || DexBindInviter.isThisType(confirmInfo)
                || DexPost.isSupportPair(confirmInfo)
                || DexCancel.isThisType(confirmInfo)
                || DexLockVxForDividend.isThisType(confirmInfo)
    }

}

