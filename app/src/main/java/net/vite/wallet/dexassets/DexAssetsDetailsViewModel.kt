package net.vite.wallet.dexassets

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_trade.view.*
import net.vite.wallet.*
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.http.JSONRCApi
import net.vite.wallet.network.http.MyRPC
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenPrice
import net.vite.wallet.network.http.vitex.VIPStakeInfoList
import net.vite.wallet.network.rpc.AsyncNet
import net.vite.wallet.network.rpc.ViteChainTokenInfo
import net.vite.wallet.utils.addTo
import net.vite.wallet.utils.showToast
import java.math.BigInteger

class DexAssetsDetailsViewModel : ViewModel() {

    val compositeDisposable = CompositeDisposable()
    val rtPrice = MutableLiveData<List<TokenPrice>>()
    val rtDexTokens = MutableLiveData<List<NormalTokenInfo>>()

    fun pollViteXTokensListeners(infos: List<NormalTokenInfo>) {
        rtDexTokens.postValue(infos)
    }

    init {
        TokenInfoCenter.pollViteXTokensListeners.add(::pollViteXTokensListeners)
    }

    fun pollPrice() {
        ExternalPriceCenter.pollQueryTokensPrice()
            .subscribe({
                rtPrice.postValue(it)
            }, {

            }).addTo(compositeDisposable)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        TokenInfoCenter.pollViteXTokensListeners.remove(::pollViteXTokensListeners)
    }

    val vipStakeForSBPLD = MutableLiveData<ViewObject<BigInteger>>()
    val vipStakeInfoListLD = MutableLiveData<ViewObject<VIPStakeInfoList>>()
    val miningStakingAmountLD = MutableLiveData<ViewObject<BigInteger>>()
    val vipStakeForFullNodeLD = MutableLiveData<ViewObject<BigInteger>>()

    @SuppressLint("CheckResult")
    fun getStakeForFullNode(address: String) {
        ViteAccountInfoPoll.getStakeForFullNode(
            address
        ).applyIoScheduler().subscribe({
            it.data?.pledgeAmount?.toBigIntegerOrNull()?.let {
                vipStakeForFullNodeLD.postValue(ViewObject.Loaded(it))
            }
        }, {
            vipStakeForFullNodeLD.postValue(ViewObject.Error(it))
        })
    }


    @SuppressLint("CheckResult")
    fun getStakeForSBP(address: String) {
        ViteAccountInfoPoll.getStakeForSBP(
            address
        ).applyIoScheduler().subscribe({
            it.resp?.let {
                vipStakeForSBPLD.postValue(ViewObject.Loaded(it))
            }
        }, {
            vipStakeForSBPLD.postValue(ViewObject.Error(it))
        })

    }


    @SuppressLint("CheckResult")
    fun dexGetVIPStakeInfoList(address: String) {
        ViteAccountInfoPoll.dexGetVIPStakeInfoList(
            address,
            0, 0
        ).applyIoScheduler().subscribe({
            it.resp?.let {
                vipStakeInfoListLD.postValue(ViewObject.Loaded(it))
            }
        }, {
            vipStakeInfoListLD.postValue(ViewObject.Error(it))
        })
    }



    @SuppressLint("CheckResult")
    fun dexGetCurrentMiningStakingAmountByAddress(address: String) {
        ViteAccountInfoPoll.dexGetCurrentMiningStakingAmountByAddress(address).subscribe({
            if (it.throwable != null) {
                miningStakingAmountLD.postValue(ViewObject.Error(it.throwable))
            }
            it.resp?.let {
                miningStakingAmountLD.postValue(ViewObject.Loaded(it))
            }
        }, {
            miningStakingAmountLD.postValue(ViewObject.Error(it))
        })
    }
}

