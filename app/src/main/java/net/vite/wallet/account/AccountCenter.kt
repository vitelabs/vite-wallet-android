package net.vite.wallet.account

import android.annotation.SuppressLint
import android.content.ContentValues
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.vite.wallet.*
import net.vite.wallet.balance.Onroad
import net.vite.wallet.balance.walletconnect.session.VCFsmHolder
import net.vite.wallet.contacts.ContactCenter
import net.vite.wallet.exchange.AccountFavExchangePairManager
import net.vite.wallet.exchange.DexInviteManager
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.security.GlobalSecurityStore
import org.vitelabs.mobile.Mobile
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference


class EmptyAddressException : Exception("empty address")

@Keep
object AccountCenter {

    private val accounts = ConcurrentHashMap<String, AccountProfile>()
    private val entropyToAccount = ConcurrentHashMap<String, AccountProfile>()
    private fun getLastLoginUUid() = ViteConfig.get().kvstore.getString("lastLoginAccount", "")

    private val currentAccount: AtomicReference<AccountProfile> = AtomicReference()
    fun getCurrentAccount(): AccountProfile? {
        return currentAccount.get()
    }

    private val currentAccountTokenManager: AtomicReference<AccountTokenManager> = AtomicReference()
    fun getCurrentAccountTokenManager(): AccountTokenManager? {
        return currentAccountTokenManager.get()
    }

    private val currentAccountFavExchangePairManager: AtomicReference<AccountFavExchangePairManager> =
        AtomicReference()


    fun getCurrentAccountFavExchangePairManager(): AccountFavExchangePairManager? {
        return currentAccountFavExchangePairManager.get()
    }

    @Volatile
    var dexInviteManager: DexInviteManager? = null
        private set

    fun currentViteAddress(extensionWord: String = "") =
        currentAccount.get()?.nowViteAddress(extensionWord)

    fun currentEthAddress(extensionWord: String = "") =
        currentAccount.get()?.nowEthAddress(extensionWord)

    fun isLogin() = currentAccount.get() != null

    fun getAccountList(): List<AccountProfile> {
        val list = accounts.values.toMutableList()
        list.sortByDescending { it.timestamp }
        return list
    }

    fun getLastLoginAccount(): AccountProfile? {
        val lastUuid = getLastLoginUUid()
        val ap = accounts.values.find {
            it.uuid.toString() == lastUuid
        } ?: kotlin.run {
            val list = getAccountList()
            if (list.isNotEmpty()) {
                list[0]
            } else {
                null
            }
        }

        return ap
    }

    fun hasAccount(accname: String) = accounts.containsKey(accname)

    fun buildAccountRelation(): Map<String, AccountProfile> {
        val dir = File(ViteConfig.get().viteRootWalletProfilePath)
        accounts.clear()
        entropyToAccount.clear()
        if (dir.isDirectory) {
            dir.listFiles().forEach { file ->
                try {
                    val readText = file.readText()
                    val ap = Gson().fromJson(readText, AccountProfile::class.java)

                    if (ap.allEthIndex == null) {//support old version AccountProfile
                        ap.allEthIndex = mutableListOf(0)
                        ap.saveToFile()
                    }

                    accounts[ap.name] = ap
                    entropyToAccount[ap.entropyStore] = ap
                } catch (e: Exception) {
                    loge(e, "buildAccountRelation")
                }
            }
        }
        return accounts
    }

    fun changeUserName(newName: String): Boolean {
        accounts.remove(currentAccount.get().name)
        currentAccount.get().name = newName.trim()
        accounts[newName] = currentAccount.get()
        GlobalSecurityStore.get("lastAccountSession")?.let {
            val lastSession = Gson().fromJson(String(it), LastLoginAccount::class.java)
            lastSession.name = currentAccount.get().name
            GlobalSecurityStore.store(
                "lastAccountSession",
                Gson().toJson(lastSession).toByteArray()
            )
        }
        return currentAccount.get().saveToFile()
    }

    fun tryGetExistAccount(primaryAddr: String) = entropyToAccount[primaryAddr]

    fun recoverAccount(
        mnemonic: String,
        newPassphrase: String,
        accountName: String,
        lang: String,
        lastAp: AccountProfile? = null
    ): Boolean {
        try {
            val primaryAddr = Mobile.tryTransformMnemonic(mnemonic, lang, "")

            val uuid = entropyToAccount[primaryAddr.hex]?.let { oldAcc ->
                oldAcc.deleteEntropyProfileFile()
                accounts.remove(oldAcc.name)?.uuid
            } ?: UUID.randomUUID()

            val primaryAddrStr =
                Wallet.instance.recoverEntropyStoreFromMnemonic(mnemonic, newPassphrase, lang, "")
            val ap = lastAp?.copy(
                name = accountName.trim(),
                entropyStore = primaryAddrStr,
                timestamp = System.currentTimeMillis(),
                uuid = uuid
            ) ?: AccountProfile(
                name = accountName.trim(),
                entropyStore = primaryAddrStr,
                timestamp = System.currentTimeMillis(),
                uuid = uuid
            )
            if (ap.saveToFile()) {
                accounts[ap.name] = ap
                entropyToAccount[primaryAddrStr] = ap
                return true
            }
        } catch (e: Exception) {
            throw e
        }
        return false
    }

    fun tryToAutoLogin(): Boolean {
        return GlobalSecurityStore.get("lastAccountSession")?.let {
            val lastSession = Gson().fromJson(String(it), LastLoginAccount::class.java)
            if (lastSession.uuid.toString() != getLastLoginUUid()) {
                GlobalSecurityStore.delete("lastAccountSession")
                false
            } else {
                login(lastSession.name, lastSession.passphrase) != null
            }
        } ?: false
    }

    fun modifyPassphrase(name: String, oldphrase: String, newphrase: String): Boolean {
        logi("vite app modifyPassphrase $name ${accounts[name]}")
        return try {
            val ap = accounts[name] ?: return false
            val mnemonic = Wallet.instance.extractMnemonic(ap.entropyStore, oldphrase)
            val checkResult = MnemonicHelper.checkMnemonic(mnemonic)
            val result =
                recoverAccount(checkResult.formatMnemonic, newphrase, name, checkResult.lang, ap)
            if (result) {
                GlobalSecurityStore.delete("lastAccountSession")
                GlobalSecurityStore.store(
                    "lastAccountSession",
                    Gson().toJson(
                        LastLoginAccount(
                            ap.uuid,
                            name,
                            newphrase,
                            System.currentTimeMillis()
                        )
                    ).toByteArray()
                )
            }
            login(name, newphrase)
            result
        } catch (e: Exception) {
            loge(e)
            false
        }
    }

    @SuppressLint("CheckResult")
    fun login(name: String, passphrase: String): AccountProfile? {
        logi("vite app login $name ${accounts[name]}")
        return accounts[name]?.let { ap ->
            try {
                Wallet.instance.unlock(ap.entropyStore, passphrase)
                logi("${ap.nowViteAddress()} unlock ok", "vite app login")
                currentAccount.set(ap)
                Onroad.start()
                logi("${ap.nowViteAddress()} Onroad.start ok", "vite app login")

                ViteConfig.get().kvstore.edit().putString("lastLoginAccount", ap.uuid.toString())
                    .apply()
                GlobalSecurityStore.delete("lastAccountSession")
                GlobalSecurityStore.store(
                    "lastAccountSession",
                    Gson().toJson(
                        LastLoginAccount(
                            ap.uuid,
                            name,
                            passphrase,
                            System.currentTimeMillis()
                        )
                    ).toByteArray()
                )

                val atp = AccountTokenManager(ap)
                atp.loadLastPinnedTokenCodesSync()

                atp.pinnedTokenInfoList.filter {
                    it.family == TokenFamily.VITE
                }.forEach {
                    atp.pinExchangeToken(it)
                }

                currentAccountTokenManager.set(atp)

                currentAccountFavExchangePairManager.set(AccountFavExchangePairManager(ap))
                dexInviteManager = DexInviteManager(ap)
                dexInviteManager?.startBind()

                ContactCenter.loadAllContacts().subscribe {}

                ap
            } catch (e: Exception) {
                null
            }
        }
    }

    fun deleteMyselfAndRestart() {
        currentAccount.get()?.deleteAll()
        ViteConfig.get().kvstore.edit().remove("lastLoginAccount").apply()
        logout()
    }

    fun removeCurrentAccount() {
        accounts.remove(currentAccount.get().name)
    }

    @SuppressLint("CheckResult")
    fun logout() {
        VCFsmHolder.close()
        Onroad.stop()
        try {
            Wallet.instance.lock(currentAccount.get().entropyStore)
            logi("Wallet lock ok", "vite app logout")
        } catch (e: Exception) {
            loge(e)
        }

        currentAccount.set(null)
        GlobalSecurityStore.delete("lastAccountSession")
        currentAccountTokenManager.set(null)
        currentAccountFavExchangePairManager.set(null)
        dexInviteManager?.end()
        dexInviteManager = null

        currentAccount.set(null)
    }


}

@Keep
private data class LastLoginAccount(
    @SerializedName("uuid")
    val uuid: UUID,
    @SerializedName("name")
    var name: String,
    @SerializedName("passphrase")
    val passphrase: String,
    @SerializedName("timestamp")
    var timestamp: Long
)