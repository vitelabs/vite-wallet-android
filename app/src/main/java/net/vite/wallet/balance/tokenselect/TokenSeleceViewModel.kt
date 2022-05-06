package net.vite.wallet.balance.tokenselect

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.vite.wallet.network.NetworkState

class TokenSelectViewModel : ViewModel() {
    val loadCacheLd = MutableLiveData<Void>()
    val loadNetWorkLd = MutableLiveData<NetworkState>()
    fun loadCache() {

    }

}
