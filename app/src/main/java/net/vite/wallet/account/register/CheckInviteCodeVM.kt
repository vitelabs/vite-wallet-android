package net.vite.wallet.account.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.ViewObject
import net.vite.wallet.exchange.DexInviteManager
import net.vite.wallet.utils.addTo

class CheckInviteCodeVM : ViewModel() {
    val checkCodeLd = MutableLiveData<ViewObject<Boolean>>()

    private val compositeDisposable = CompositeDisposable()
    fun checkCode(code: String) {
        checkCodeLd.postValue(ViewObject.Loading())

        DexInviteManager.checkInviteCode(code).subscribeOn(Schedulers.io()).subscribe({
            checkCodeLd.postValue(ViewObject.Loaded(it))
        }, {
            checkCodeLd.postValue(ViewObject.Error(it))
        }).addTo(compositeDisposable)

    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }
}