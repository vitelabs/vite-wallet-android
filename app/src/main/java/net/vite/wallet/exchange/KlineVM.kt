package net.vite.wallet.exchange

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.vite.wallet.ViewObject
import net.vite.wallet.network.http.vitex.TradeList

class KlineVM : ViewModel() {
    //TODO refactor to mvvm
    val tradeInfoLd = MutableLiveData<ViewObject<List<TradeList>>>()

//    fun getTradeInfoLd(symbol: String, isForce: Boolean) {
//        TradeListCenter.getTradeList(tradeInfoLd, symbol, isForce, 0)
//    }
}
