package net.vite.wallet.balance

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.network.rpc.AccountBlock
import net.vite.wallet.network.rpc.CalcPowDifficultyReq
import net.vite.wallet.network.rpc.RpcException
import net.vite.wallet.network.rpc.SyncNet
import net.vite.wallet.utils.ZeroHash
import java.util.concurrent.TimeUnit

object Onroad {
    var d: Disposable? = null
    fun start() {
        stop()
        autoReceive()
    }

    fun stop() {
        d?.dispose()
    }

    private var lastPowTime = 0L

    private fun autoReceive() {
        d = Observable.fromCallable {
            val nowAddr = AccountCenter.currentViteAddress() ?: return@fromCallable
            val blocksRes = SyncNet.getOnroadBlocksByAddress(nowAddr, 0, 3)
            if (blocksRes.resp?.size ?: 0 == 0) {
                return@fromCallable
            }

            blocksRes.resp?.forEach { sendBlock ->
                if (System.currentTimeMillis() - lastPowTime < 1000) {
                    Thread.sleep(2000)
                }
                val latestBlockRes = SyncNet.getLatestBlock(nowAddr)
                if (!latestBlockRes.success()) {
                    return@fromCallable
                }
                val preHash = latestBlockRes.resp?.hash ?: ZeroHash

                val calcPowDifficultyReq = CalcPowDifficultyReq(
                    blockType = AccountBlock.BlockTypeReceive,
                    data = null,
                    prevHash = preHash,
                    selfAddr = nowAddr,
                    toAddr = null,
                    usePledgeQuota = true
                )
                val calcPowResp = SyncNet.calcPoWDifficulty(calcPowDifficultyReq)
                if (calcPowResp.throwable != null) {
                    if (calcPowResp.throwable is RpcException && calcPowResp.throwable.isVmCalPoWTwice()) {
                        Thread.sleep(1000)
                    } else {
                        return@fromCallable
                    }
                }

                if (calcPowResp.resp?.isCongestion == true) {
                    return@fromCallable
                }

                val difficulty = if (calcPowResp.resp?.difficulty.isNullOrEmpty()) {
                    null
                } else {
                    calcPowResp.resp?.difficulty
                }

                val nonce = if (!difficulty.isNullOrEmpty()) {
                    val nonceRes = SyncNet.getPowNonce(difficulty, preHash, nowAddr)
                    if (!nonceRes.success()) {
                        return@fromCallable
                    }
                    lastPowTime = System.currentTimeMillis()
                    nonceRes.resp
                } else {
                    null
                }

                val ab = AccountBlock(
                    blockType = AccountBlock.BlockTypeReceive,
                    accountAddress = nowAddr,
                    fromBlockHash = sendBlock.hash,
                    prevHash = preHash,
                    height = (latestBlockRes.resp?.height?.toLong()?.plus(1) ?: 1).toString(),
                    difficulty = difficulty,
                    nonce = nonce
                )

                ab.computeHash()
                ab.sign()

                SyncNet.sendRawTx(Gson().toJson(ab))
            }

        }.repeatWhen { it.delay(4, TimeUnit.SECONDS) }
            .retryWhen { it.delay(5, TimeUnit.SECONDS) }
            .subscribeOn(Schedulers.io())
            .subscribe({}, {})
    }
}
