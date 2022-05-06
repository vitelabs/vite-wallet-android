package net.vite.wallet.balance.txsend

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.eth.EthNet
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.utils.Convert
import java.math.BigInteger

val DefaultGasPrice = Convert.toWei("1", Convert.Unit.GWEI).toBigInteger()


@SuppressLint("CheckResult")
class EthSendViewModel : ViewModel() {
    val gasPriceLd = MutableLiveData<BigInteger>()
    val sendTxLd = MutableLiveData<EthSendTransaction?>()
    val signTxLd = MutableLiveData<EthNet.EthSignResult>()
    val netWorkLd = MutableLiveData<NetworkState>()

    fun getGas() {
        Single.fromCallable {
            EthNet.getGasPrice() ?: DefaultGasPrice
        }.subscribeOn(Schedulers.io()).subscribe({
            gasPriceLd.postValue(it)
        }, {
            gasPriceLd.postValue(DefaultGasPrice)
        })
    }

    fun signErc20TransferTx(
        params: EthNet.EthSendParams
    ) {
        netWorkLd.postValue(NetworkState.LOADING)
        Observable.fromCallable {
            EthNet.signErc20TransferTx(params)
        }.subscribeOn(Schedulers.io()).subscribe({
            netWorkLd.postValue(NetworkState.LOADED)
            signTxLd.postValue(it)
        }, {
            netWorkLd.postValue(NetworkState.error(it))
        })
    }

    fun sendErc20Token(
        params: EthNet.EthSendParams
    ) {
        netWorkLd.postValue(NetworkState.LOADING)
        Observable.fromCallable {
            EthNet.sendErc20Tx(params)
        }.subscribeOn(Schedulers.io()).subscribe({
            netWorkLd.postValue(NetworkState.LOADED)
            sendTxLd.postValue(it)
        }, {
            netWorkLd.postValue(NetworkState.error(it))
            sendTxLd.postValue(null)
        })
    }

    fun sendEth(
        params: EthNet.EthSendParams
    ) {
        netWorkLd.postValue(NetworkState.LOADING)
        Observable.fromCallable {
            EthNet.sendEth(params)
        }.subscribeOn(Schedulers.io()).subscribe({
            netWorkLd.postValue(NetworkState.LOADED)
            sendTxLd.postValue(it)
        }, {
            netWorkLd.postValue(NetworkState.error(it))
            sendTxLd.postValue(null)
        })
    }

}