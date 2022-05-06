package net.vite.wallet.network.rpc

import android.annotation.SuppressLint
import androidx.annotation.Keep
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.ViewObject
import net.vite.wallet.utils.ZeroHash
import java.math.BigInteger


class CalcPowDifficultyRespException(
    val calcPowDifficultyResp: CalcPowDifficultyResp,
    val calcPowDifficultyReq: CalcPowDifficultyReq
) : Exception()

class RpcEmptyRespException(message: String) : Exception(message)

@Keep
data class NormalTxParams(
    val accountAddr: String,
    val toAddr: String,
    val tokenId: String,
    val amountInSu: BigInteger,
    var data: String?,
    var difficulty: String? = null,
    var nonce: String? = null,
    var fee: BigInteger? = null,
    //仅用户 动态配额 非pow发送
    var forceSend: Boolean = false,
    val blockType: Byte? = null
)


data class GetPowNonceReq(
    val difficulty: String,
    val preHash: String?,
    val addr: String
)

data class GetPowNonceResp(
    val req: GetPowNonceReq,
    val nonce: String
)


@SuppressLint("CheckResult")
object BlockSendManager {
    var powDispose: Disposable? = null

    fun cancelPow() {
        powDispose?.dispose()
    }

    fun calcQuotaRequired(
        req: CalcQuotaRequiredReq,
        requestCode: Int? = null
    ): MutableLiveData<ViewObject<CalcQuotaRequiredResp>> {
        val calcResp = MutableLiveData<ViewObject<CalcQuotaRequiredResp>>()
        calcResp.postValue(ViewObject.Loading(requestCode))
        Observable.fromCallable {
            val calcPowResp = SyncNet.calcQuotaRequired(req)
            calcPowResp.throwable?.let {
                throw it
            }
            calcPowResp.resp
        }.subscribe({
            calcResp.postValue(ViewObject.Loaded(it, requestCode))
        }, {
            calcResp.postValue(ViewObject.Error(it, requestCode))
        })
        return calcResp
    }

    @SuppressLint("CheckResult")
    fun normalSendCallTxSend(
        params: NormalTxParams,
        requestCode: Int? = null
    ): MutableLiveData<ViewObject<AccountBlock>> {
        val txSendLiveData = MutableLiveData<ViewObject<AccountBlock>>()
        txSendLiveData.postValue(ViewObject.Loading(requestCode))
        Observable.fromCallable<AccountBlock> {
            trySend(params)
        }.subscribeOn(Schedulers.io())
            .subscribe({
                txSendLiveData.postValue(ViewObject.Loaded(it, requestCode))
            }, {
                txSendLiveData.postValue(
                    ViewObject.Error(
                        it,
                        requestCode
                    )
                )// ABI error, contract error
                logi("abi error: $it")
            })

        return txSendLiveData
    }

    private fun trySend(params: NormalTxParams): AccountBlock {
        val latestBlock = SyncNet.getLatestBlock(params.accountAddr)
        latestBlock.throwable?.let {
            throw it
        }
        val preHash = latestBlock.resp?.hash ?: ZeroHash

        if (params.difficulty == null && !params.forceSend) {
            val calcPowDifficultyReq = CalcPowDifficultyReq(
                blockType = params.blockType ?: AccountBlock.BlockTypeSendCall,
                data = params.data,
                prevHash = preHash,
                selfAddr = params.accountAddr,
                toAddr = params.toAddr,
                usePledgeQuota = true
            )
            val calcPowResp = SyncNet.calcPoWDifficulty(calcPowDifficultyReq)

            calcPowResp.throwable?.let {
                throw it
            }
            calcPowResp.resp?.let { calcPowDiffResp ->
                if (calcPowDiffResp.isCongestion == true) {
                    throw CalcPowDifficultyRespException(calcPowDiffResp, calcPowDifficultyReq)
                }
                if (!calcPowDiffResp.difficulty.isNullOrEmpty()) {
                    throw CalcPowDifficultyRespException(calcPowDiffResp, calcPowDifficultyReq)
                }

            } ?: throw RpcEmptyRespException("calcPowResp")
        }

        val ab = AccountBlock(
            blockType = params.blockType ?: AccountBlock.BlockTypeSendCall,
            toAddress = params.toAddr,
            amount = params.amountInSu.toString(),
            tokenId = params.tokenId,
            accountAddress = params.accountAddr,
            prevHash = preHash,
            height = (latestBlock.resp?.height?.toLong()?.plus(1) ?: 1).toString(),
            data = params.data,
            difficulty = params.difficulty,
            nonce = params.nonce,
            fee = params.fee?.toString()
        )

        ab.computeHash()
        ab.sign()

        val r = SyncNet.sendRawTx(Gson().toJson(ab))
        r.throwable?.let {
            throw it
        }
        return ab
    }

    fun mustSend(params: NormalTxParams): AccountBlock {
        val latestBlock = SyncNet.getLatestBlock(params.accountAddr)
        latestBlock.throwable?.let {
            throw it
        }
        val preHash = latestBlock.resp?.hash ?: ZeroHash

        if (params.difficulty == null && !params.forceSend) {
            val calcPowDifficultyReq = CalcPowDifficultyReq(
                blockType = params.blockType ?: AccountBlock.BlockTypeSendCall,
                data = params.data,
                prevHash = preHash,
                selfAddr = params.accountAddr,
                toAddr = params.toAddr,
                usePledgeQuota = true
            )
            val calcPowResp = SyncNet.calcPoWDifficulty(calcPowDifficultyReq)
            calcPowResp.throwable?.let {
                throw it
            }
            if (calcPowResp.resp?.isCongestion == true) {
                throw CalcPowDifficultyRespException(calcPowResp.resp, calcPowDifficultyReq)
            }
            val difficulty = calcPowResp.resp?.difficulty
            val nonceResp = difficulty?.let {
                SyncNet.getPowNonce(it, preHash, params.accountAddr)
            }
            nonceResp?.throwable?.let {
                throw it
            }

            val nonce = nonceResp?.resp
            params.nonce = nonce
            params.difficulty = difficulty
        }

        val ab = AccountBlock(
            blockType = params?.blockType ?: AccountBlock.BlockTypeSendCall,
            toAddress = params.toAddr,
            amount = params.amountInSu.toString(),
            tokenId = params.tokenId,
            accountAddress = params.accountAddr,
            prevHash = preHash,
            height = (latestBlock.resp?.height?.toLong()?.plus(1) ?: 1).toString(),
            data = params.data,
            difficulty = params.difficulty,
            nonce = params.nonce,
            fee = params.fee?.toString()
        )

        ab.computeHash()
        ab.sign()

        val r = SyncNet.sendRawTx(Gson().toJson(ab))
        r.throwable?.let {
            throw it
        }
        return ab
    }

    fun getPowNonce(
        getPowNonceReq: GetPowNonceReq,
        requestCode: Int? = null
    ): MutableLiveData<ViewObject<GetPowNonceResp>> {
        val powLiveData = MutableLiveData<ViewObject<GetPowNonceResp>>()
        powLiveData.postValue(ViewObject.Loading(requestCode))
        powDispose = Observable.fromCallable<GetPowNonceResp> {
            val startTime = System.currentTimeMillis()
            val nonceResp =
                with(getPowNonceReq) {
                    SyncNet.getPowNonce(difficulty, preHash, addr)
                }

            nonceResp.throwable?.let {
                throw it
            }
            val result = nonceResp.resp ?: kotlin.run {
                throw RpcEmptyRespException("getPowNonce")
            }
            val cost = System.currentTimeMillis() - startTime
            if (cost < 3000) {
                Thread.sleep(3000 - cost)
            }
            GetPowNonceResp(
                req = getPowNonceReq,
                nonce = result
            )
        }.subscribeOn(Schedulers.io())
            .subscribe({
                powLiveData.postValue(ViewObject.Loaded(it, requestCode))
            }, {
                powLiveData.postValue(ViewObject.Error(it, requestCode))
            })

        return powLiveData
    }
}