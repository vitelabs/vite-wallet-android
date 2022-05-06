package net.vite.wallet.exchange

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.account.AccountProfile
import net.vite.wallet.network.http.growth.DexInviteCodeApi
import net.vite.wallet.network.rpc.*
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class DexInviteManager(val accountProfile: AccountProfile) {
    companion object {
        fun checkInviteCode(code: String): Observable<Boolean> {
            return Observable.fromCallable {
                if (code.toBigIntegerOrNull() == null) {
                    false
                } else {
                    val resp = SyncNet.dexIsInviteCodeValid(code)
                    resp.throwable?.let {
                        throw it
                    }
                    resp.resp ?: false
                }
            }
        }
    }

    class ErrorCodeException() : Exception()

    private var lastBindDisposable: Disposable? = null

    fun startBind() {
        if (getBoundAddresses()?.contains(
                accountProfile.nowViteAddress()!!
            ) == true
        ) {
            return
        }
        val code = getInviteCode() ?: return
        val codeBigInteger = code.toBigIntegerOrNull() ?: kotlin.run {
            deleteCode()
            return
        }


        lastBindDisposable?.dispose()
        lastBindDisposable = checkInviteCode(code)
            .subscribeOn(Schedulers.io())
            .retryWhen { it.delay(5, TimeUnit.SECONDS) }
            .flatMap {
                if (it) {
                    accountInit().retryWhen { it.delay(5, TimeUnit.SECONDS) }
                } else {
                    throw ErrorCodeException()
                }
            }.flatMap {
                checkAccountBlock()
            }.flatMap {
                tryBind(codeBigInteger).retryWhen { it.delay(10, TimeUnit.SECONDS) }
            }.subscribe({
                addressHasBound()
            }, {
                if (it is ErrorCodeException) {
                    deleteCode()
                }
            })

    }

    fun end() {
        lastBindDisposable?.dispose()
        lastBindDisposable = null
    }

    fun tryBind(codeBigInteger: BigInteger): Observable<Unit> {
        return Observable.fromCallable {
            val addr = accountProfile.nowViteAddress()!!
            val hasBound =
                SyncNet.dexGetInviteCodeBinding(addr).resp ?: 0 != 0
            if (hasBound) {
                Unit
            } else {
                BlockSendManager.mustSend(
                    NormalTxParams(
                        accountAddr = addr,
                        toAddr = "123",
                        tokenId = ViteTokenInfo.tokenId!!,
                        amountInSu = BigInteger.ZERO,
                        data = BuildInContractEncoder.dexBindInviteCode(codeBigInteger)
                    )
                )
                Unit
            }

        }

    }

    fun checkAccountBlock(): Observable<Unit> {
        return Observable.fromCallable {
            while (SyncNet.getLatestBlock(accountProfile.nowViteAddress()!!).resp?.hash?.length ?: 0 == 0) {
                Thread.sleep(2 * 1000)
            }
            Unit
        }
    }

    fun accountInit(): Observable<Unit> {
        return DexInviteCodeApi.initAddress(accountProfile.nowViteAddress()!!)
    }

    fun deleteCode() {
        accountProfile.sharedPreferences()?.edit()?.remove("invite_mining_code")?.apply()
    }

    fun saveInviteCode(code: String) {
        if (getInviteCode() == code) {
            return
        }
        accountProfile.sharedPreferences()?.edit()?.putString("invite_mining_code", code)?.apply()
        startBind()
    }

    fun getInviteCode(): String? {
        return accountProfile.sharedPreferences()?.getString(
            "invite_mining_code",
            null
        )
    }

    fun getBoundAddresses(): MutableSet<String>? {
        return accountProfile.sharedPreferences()
            ?.getStringSet("invite_mining_code_bind_address_set", HashSet<String>())
    }

    fun addressHasBound() {
        val set = getBoundAddresses() ?: HashSet<String>()
        set.add(accountProfile.nowViteAddress()!!)
        accountProfile.sharedPreferences()?.edit()
            ?.putStringSet("invite_mining_code_bind_address_set", set)?.apply()
    }


}