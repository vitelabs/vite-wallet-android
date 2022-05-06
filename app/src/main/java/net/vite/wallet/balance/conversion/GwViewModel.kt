package net.vite.wallet.balance.conversion

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.network.NetworkState
import net.vite.wallet.network.http.gw.GwApi
import net.vite.wallet.network.http.gw.GwConvertBindBody
import org.apache.log4j.Logger

class GwViewModel : ViewModel() {
    val log = Logger.getLogger("GwViewModel")
    val network = MutableLiveData<NetworkState>()
    val bindResultLd = MutableLiveData<Boolean>()

    @SuppressLint("CheckResult")
    fun bind(gwConvertBindBody: GwConvertBindBody) {
        network.postValue(NetworkState.LOADING)
        GwApi.getApi().convertBind(gwConvertBindBody).subscribeOn(Schedulers.io()).subscribe({
            // 201 it has already bind
            log.info("GwApi.getApi() ${it.code}")
            bindResultLd.postValue(it != null && (it.code == 200 || it.code == 201))
            network.postValue(NetworkState.LOADED)
        }, {
            log.warn("GwApi.getApi() ${it.message}")
            bindResultLd.postValue(false)
            network.postValue(NetworkState.error(it))
        })

    }
}