package net.vite.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import net.vite.wallet.network.rpc.BlockSendManager
import net.vite.wallet.network.rpc.CalcQuotaRequiredReq
import net.vite.wallet.network.rpc.GetPowNonceReq
import net.vite.wallet.network.rpc.NormalTxParams

open class TxSendViewModel : ViewModel() {

    private val txSendTrigger = MutableLiveData<Pair<NormalTxParams, Int?>>()
    val txSendLiveData = Transformations.switchMap(txSendTrigger) {
        BlockSendManager.normalSendCallTxSend(it.first, it.second)
    }

    fun sendTx(p: NormalTxParams, requestCode: Int? = null) {
        txSendTrigger.postValue(p to requestCode)
    }


    private val getPowNonceTrigger = MutableLiveData<Pair<GetPowNonceReq, Int?>>()
    val powNonceLd = Transformations.switchMap(getPowNonceTrigger) {
        BlockSendManager.getPowNonce(it.first, it.second)
    }

    fun getPowNonce(req: GetPowNonceReq, requestCode: Int? = null) {
        getPowNonceTrigger.postValue(req to requestCode)
    }

    fun cancelPow() {
        BlockSendManager.cancelPow()
    }

    private val calcPowQuotaReqTrigger = MutableLiveData<CalcQuotaRequiredReq>()
    val calcQuotaRequiredLd = Transformations.switchMap(calcPowQuotaReqTrigger) {
        BlockSendManager.calcQuotaRequired(it)
    }

    fun calcQuotaRequired(req: CalcQuotaRequiredReq) {
        calcPowQuotaReqTrigger.postValue(req)
    }


}
