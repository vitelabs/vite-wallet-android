package net.vite.wallet.balance

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import net.vite.wallet.ExternalPriceCenter
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.balance.poll.EthAccountInfoPoll
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.balance.quota.QuotaAndTxNumPoll
import net.vite.wallet.balance.quota.QuotaPledgeInfoPoll
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenPrice
import net.vite.wallet.network.rpc.*
import net.vite.wallet.utils.addTo

class BalanceViewModel : ViewModel() {
    private val compositeDisposable = CompositeDisposable()
    val rtViteAccInfo = ViteAccountInfoPollLiveData()
    val dexAccountFundInfo = DexAccountFundInfoPollLiveData()
    val rtEthAccInfo = EthAccountInfoPollLiveData()

    val rtQuotaAndTxNum = QuotaAndTxNumPollLiveData()

    val rtPledgeTotalAmount = QuotaTotalPledgePollLiveData()

    val tokenInfosLd = MutableLiveData<List<NormalTokenInfo>>()
    val tokenPriceLd = MutableLiveData<List<TokenPrice>>()

    fun loadAllCachedTokenInfo() {
        TokenInfoCenter.loadDefaultAndUserAddTokenInfoCache().subscribe({
            tokenInfosLd.postValue(TokenInfoCenter.getAllOrderedTokenInfos())
        }, {}).addTo(compositeDisposable)
    }

    fun loadAllCachedPrice() {
        ExternalPriceCenter.loadTokenPriceCache().subscribe({
            tokenPriceLd.postValue(it)
        }, {}).addTo(compositeDisposable)
    }

    fun refreshTokenPrice(tokenCodes: List<String>) {
        ExternalPriceCenter.refreshQueryTokenPrice(tokenCodes).subscribe({
            tokenPriceLd.postValue(it)
        }, {}).addTo(compositeDisposable)
    }

    fun batchQueryTokenDetail(tokenCodes: List<String>) {
        TokenInfoCenter.batchQueryTokenDetail(tokenCodes).subscribe({
            tokenInfosLd.postValue(it)
        }, {
            it.printStackTrace()
        }).addTo(compositeDisposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }

}


class QuotaTotalPledgePollLiveData : MutableLiveData<PledgeInfoResp>() {
    var lid: Int? = null
    override fun onActive() {
        lid = QuotaPledgeInfoPoll.register(this)
    }

    override fun onInactive() {
        lid?.let {
            QuotaPledgeInfoPoll.unregister(it)
        }
    }
}

class QuotaAndTxNumPollLiveData : MutableLiveData<GetPledgeQuotaResp>() {
    var lid: Int? = null
    override fun onActive() {
        lid = QuotaAndTxNumPoll.register(this)
    }

    override fun onInactive() {
        lid?.let {
            QuotaAndTxNumPoll.unregister(it)
        }
    }
}

class EthAccountInfoPollLiveData : MutableLiveData<EthAccountInfo>() {
    var lid: Int? = null
    override fun onActive() {
        lid = EthAccountInfoPoll.register(this)
    }

    override fun onInactive() {
        lid?.let {
            EthAccountInfoPoll.unregister(it)
        }
    }
}

class ViteAccountInfoPollLiveData : MutableLiveData<ViteAccountInfo>() {
    var lid: Int? = null
    override fun onActive() {
        lid = ViteAccountInfoPoll.register(this)
    }

    override fun onInactive() {
        lid?.let {
            ViteAccountInfoPoll.unregister(it)
        }
    }
}

class DexAccountFundInfoPollLiveData : MutableLiveData<Map<String, GetAccountFundInfoResp>>() {
    var lid: Int? = null
    override fun onActive() {
        lid = DexAccountFundInfoPoll.register(this)
    }

    override fun onInactive() {
        lid?.let {
            DexAccountFundInfoPoll.unregister(it)
        }
    }
}