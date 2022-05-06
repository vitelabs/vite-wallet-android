package net.vite.wallet.balance.walletconnect.session

import android.os.Handler
import android.os.HandlerThread
import com.google.gson.Gson
import net.vite.wallet.ViewObject
import net.vite.wallet.balance.walletconnect.session.fsm.EventTxSendSuccess
import net.vite.wallet.balance.walletconnect.taskdetail.toNormalTxParams
import net.vite.wallet.logt
import net.vite.wallet.network.rpc.*
import net.vite.wallet.utils.ZeroHash
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

typealias PostTaskToUserCheck = () -> Unit

class SignManager(val currentViteAddr: String) {

    private val postTaskToUserChecks = HashMap<Int, PostTaskToUserCheck>()
    private var postTaskToUserCheckId = 0
    fun addPostTaskToUserCheck(postTaskToUserCheck: PostTaskToUserCheck): Int {
        postTaskToUserChecks.clear()
        postTaskToUserCheckId++
        postTaskToUserChecks[postTaskToUserCheckId] = postTaskToUserCheck
        return postTaskToUserCheckId
    }

    @Volatile
    private var isWaitingUserProcess = false

    fun rmPostTaskToUserCheck(id: Int) {
        postTaskToUserChecks.remove(id)
    }

    private val signHandler: Handler
    private val workThread: HandlerThread = HandlerThread("SignManager")
    @Volatile
    private var currentTaskIndex = 0
    @Volatile
    private var isSuspend = false
    @Volatile
    var isAutoSignEnabled = false
        set(value) {
            if (value) {
                appIsForeground = true
            }
            field = value
        }
    @Volatile
    var appIsForeground = false

    init {
        workThread.start()
        signHandler = Handler(workThread.looper) { msg ->
            run()
            true
        }
    }

    val requestTaskArray = Collections.synchronizedList(ArrayList<VcRequestTask>())

    fun getTask(index: Int) = kotlin.runCatching { requestTaskArray[index] }.getOrNull()
    fun getCurrentTask() = kotlin.runCatching { requestTaskArray[currentTaskIndex] }.getOrNull()

    fun addTask(task: VcRequestTask) {
        requestTaskArray.add(task)
        if (isWaitingUserProcess) {
            postTaskToUserChecks.values.forEach { it() }
        }
        signHandler.sendEmptyMessage(0)
    }


    fun suspend() {
        isSuspend = true
    }

    fun resume() {
        isSuspend = false
        signHandler.sendEmptyMessage(0)
    }

    fun stop() {
        Thread {
            kotlin.runCatching {
                workThread.quitSafely()
            }
        }.start()
    }

    private fun autoSignEnabled(currentTask: VcRequestTask): Boolean {
        return appIsForeground && isAutoSignEnabled && currentTask.isSupportAutoSign()
    }

    private val recoverableCode = listOf(
        -32002,
        -35002,
        -35005,
        -36005
    )

    private fun checkErrorRecoverable(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        if (throwable is RpcException) {
            return recoverableCode.indexOf(throwable.code) != -1
        }
        return true
    }

    fun run() {

        while (currentTaskIndex < requestTaskArray.size && !isSuspend) {
            val currentTask = requestTaskArray[currentTaskIndex]
            logt("currentTaskIndex $currentTaskIndex")
            if (autoSignEnabled(currentTask)) {
                currentTask.currentState = VcRequestTask.AutoProcessing

                val startTime = System.currentTimeMillis()
                val vo =
                    send(currentTask.tx.sendTransaction!!.block.toNormalTxParams(currentViteAddr))
                if (System.currentTimeMillis() - startTime < 1000) {
                    Thread.sleep(1000)
                }
                if (vo.isSuccess()) {
                    currentTask.currentState = VcRequestTask.Success
                    VCFsmHolder.sendEvent(
                        EventTxSendSuccess(
                            currentTask.tx.sendTransaction!!.id,
                            vo.resp!!
                        )
                    )
                    currentTaskIndex++
                    continue
                }
                val error = vo.error()
                if (!checkErrorRecoverable(error)) {
                    continue
                } else {
                    currentTask.currentState = VcRequestTask.Failed
                    currentTask.throwable = error
                    currentTaskIndex++
                }
            } else {
                if (postTaskToUserChecks.isEmpty()) {
                    continue
                }
                currentTask.currentState = VcRequestTask.WaitUserProcessing
                postTaskToUserChecks.values.forEach { it() }
                isWaitingUserProcess = true
                currentTask.completeLatch.await()
                isWaitingUserProcess = false
                currentTaskIndex++
            }
        }

    }


    private fun send(
        params: NormalTxParams
    ): ViewObject<AccountBlock> {

        val latestBlock = SyncNet.getLatestBlock(params.accountAddr)
        latestBlock.throwable?.let {
            return ViewObject.Error(it)
        }

        val preHash = latestBlock.resp?.hash ?: ZeroHash
        var nonce: String? = null
        var difficulty: String? = null


        val calcPowDifficultyReq = CalcPowDifficultyReq(
            blockType = AccountBlock.BlockTypeSendCall,
            data = params.data,
            prevHash = preHash,
            selfAddr = params.accountAddr,
            toAddr = params.toAddr,
            usePledgeQuota = true
        )
        val calcPowResp = SyncNet.calcPoWDifficulty(calcPowDifficultyReq)

        calcPowResp.throwable?.let {
            return ViewObject.Error(it)
        }

        calcPowResp.resp?.let { calcPowDiffResp ->
            if (calcPowDiffResp.isCongestion == true) {
                return ViewObject.Error(
                    CalcPowDifficultyRespException(
                        calcPowDiffResp,
                        calcPowDifficultyReq
                    )
                )
            }

            if (!calcPowDiffResp.difficulty.isNullOrEmpty()) {
                val nonceResp = SyncNet.getPowNonce(
                    difficulty = calcPowDiffResp.difficulty,
                    preHashHex = preHash,
                    addr = params.accountAddr
                )

                nonceResp.throwable?.let {
                    return ViewObject.Error(it)
                }
                nonce = nonceResp.resp
                difficulty = calcPowDiffResp.difficulty
            }
        }


        val ab = AccountBlock(
            blockType = AccountBlock.BlockTypeSendCall,
            toAddress = params.toAddr,
            amount = params.amountInSu.toString(),
            tokenId = params.tokenId,
            accountAddress = params.accountAddr,
            prevHash = preHash,
            height = (latestBlock.resp?.height?.toLong()?.plus(1) ?: 1).toString(),
            data = params.data,
            difficulty = difficulty,
            nonce = nonce,
            fee = params.fee?.toString()
        )

        ab.computeHash()
        ab.sign()

        val r = SyncNet.sendRawTx(Gson().toJson(ab))
        r.throwable?.let {
            return ViewObject.Error(it)
        }
        return ViewObject.Loaded(ab)

    }


}