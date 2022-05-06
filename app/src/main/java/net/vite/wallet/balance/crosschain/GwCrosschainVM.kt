package net.vite.wallet.balance.crosschain

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import net.vite.wallet.ViewObject
import net.vite.wallet.network.http.gw.*


@SuppressLint("CheckResult")
class GwCrosschainVM : ViewModel() {


    val depositInfoLd = MutableLiveData<ViewObject<DepositInfoResp>>()
    fun depositInfo(tokenId: String, walletAddress: String, gwUrl: String, requestCode: Int = 0) {
        depositInfoLd.postValue(ViewObject.Loading(requestCode))
        GwCrosschainApi.depositInfo(tokenId, walletAddress, gwUrl)
            .subscribe({
                depositInfoLd.postValue(ViewObject.Loaded(it, requestCode))
            }, {
                depositInfoLd.postValue(ViewObject.Error(it, requestCode))
            })
    }

    val metaInfoLd = MutableLiveData<ViewObject<MetaInfoResp>>()
    fun getOverchainMetaInfo(
        gwUrl: String,
        tokenId: String,
        requestCode: Int = 0,
        tokenCode: String? = null
    ) {
        metaInfoLd.postValue(ViewObject.Loading(requestCode, tokenCode))
        GwCrosschainApi.metaInfo(tokenId, gwUrl)
            .subscribe({
                metaInfoLd.postValue(ViewObject.Loaded(it, requestCode, tokenCode))
            }, {
                metaInfoLd.postValue(ViewObject.Error(it, requestCode, tokenCode))
            })

    }


    val withdrawFeeLd = MutableLiveData<ViewObject<WithdrawFeeResp>>()
    fun withdrawFee(
        gwUrl: String,
        tokenId: String,
        walletAddress: String,
        amount: String,
        requestCode: Int = 0
    ) {
        withdrawFeeLd.postValue(ViewObject.Loading(requestCode))
        GwCrosschainApi.withdrawFee(tokenId, walletAddress, amount, gwUrl)
            .subscribe({
                withdrawFeeLd.postValue(ViewObject.Loaded(it, requestCode))
            }, {
                withdrawFeeLd.postValue(ViewObject.Error(it, requestCode))
            })
    }

    val withdrawAddrVerifyAndGetFeeLd =
        MutableLiveData<ViewObject<Pair<WithdrawIsValidAddrResp, WithdrawFeeResp>>>()

    fun withdrawAddressVerificationAndGetFee(
        gwUrl: String,
        tokenId: String,
        withdrawAddr: String,
        walletAddress: String,
        amount: String,
        label: String? = null,
        requestCode: Int = 0
    ) {
        withdrawAddrVerifyAndGetFeeLd.postValue(ViewObject.Loading(requestCode))
        Observable.zip(
            GwCrosschainApi.withdrawAddressVerification(tokenId, withdrawAddr, label, gwUrl),
            GwCrosschainApi.withdrawFee(tokenId, walletAddress, amount, gwUrl),
            { t1: WithdrawIsValidAddrResp, t2: WithdrawFeeResp ->
                t1 to t2
            }
        ).subscribe({
            withdrawAddrVerifyAndGetFeeLd.postValue(ViewObject.Loaded(it, requestCode))
        }, {
            withdrawAddrVerifyAndGetFeeLd.postValue(ViewObject.Error(it, requestCode))
        })

    }
}