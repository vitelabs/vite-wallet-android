package net.vite.wallet.account

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.vite.wallet.*
import net.vite.wallet.utils.hexToBytes
import net.vite.wallet.utils.mustMakeDirs
import org.vitelabs.mobile.Mobile
import org.vitelabs.mobile.SignDataResult
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val EthBip44OfficialPrefix = "m/44'/60'/%d'"
const val EthBip44CommunityPrefix = "m/44'/60'/0'/0/"
const val EthBip44CommunityDefault =
    "m/44'/60'/0'/0/0" // m/purpose'/coin_type'/account'/change/address_index

object AddressCache {
    val cache = ConcurrentHashMap<String, String>()
    fun put(entropyStore: String, index: Int, address: String) {
        cache[key(entropyStore, index)] = address
    }

    fun get(entropyStore: String, index: Int) = cache[key(entropyStore, index)]

    private fun key(entropyStore: String, index: Int) = "$entropyStore $index"
}


@Keep
data class AccountProfile(
    @SerializedName("name")
    var name: String,
    @SerializedName("entropyStore")
    val entropyStore: String,
    @SerializedName("timestamp")
    var timestamp: Long,
    @SerializedName("uuid")
    var uuid: UUID,

    @SerializedName("index")
    var index: Int = 0,

    @SerializedName("ethIndex")
    var ethIndex: Int = 0,

    @SerializedName("allIndex")
    var allIndex: MutableList<Int> = mutableListOf(0),

    @SerializedName("allEthIndex")
    var allEthIndex: MutableList<Int> = mutableListOf(0),

    @SerializedName("ethBip44Paths")
    var ethBip44Paths: MutableList<String>? = mutableListOf(EthBip44CommunityDefault),

    @SerializedName("nowEthPath")
    var nowEthPath: String? = null,

    @SerializedName("nowGrinPath")
    var nowGrinPath: String? = null,

    @SerializedName("addressNameMap")
    var addressNameMap: HashMap<String, String>? = null
) {

    fun deleteAll() {
        kotlin.runCatching {
            AccountCenter.removeCurrentAccount()
            deleteEntropyProfileFile()
        }

        kotlin.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ViteConfig.get().context.deleteSharedPreferences(this.uuid.toString())
            } else {
                sharedPreferences()?.edit()?.clear()?.apply()
            }
        }

        kotlin.runCatching {
            File(ViteConfig.get().viteRootPath + "/pri" + this.uuid.toString()).deleteRecursively()
            logi(ViteConfig.get().viteRootPath + "/pri" + this.uuid.toString(), "DeleteAccount")
        }

        kotlin.runCatching {
            deleteEntropyFile()
        }
    }

    fun sharedPreferences(): SharedPreferences? {
        return ViteConfig.get().context.getSharedPreferences(
            this.uuid.toString(),
            Context.MODE_PRIVATE
        )
    }


    fun getViteAddrParentFile(): File {
        return File(getPrivateFileDir(), "viteAddr").mustMakeDirs()
    }

    fun getCurrentViteAddrPrivDir(): File {
        return File(getViteAddrParentFile(), "index#$index").mustMakeDirs()
    }

    fun getPrivateFileDir(): File {
        val f = File(ViteConfig.get().viteRootPath + "/pri" + this.uuid.toString())
        if (f.exists() && !f.isDirectory) {
            f.delete()
        }
        if (!f.exists()) {
            f.mkdirs()
        }
        return f
    }

    private fun filename(): String {
        val dir = File(ViteConfig.get().viteRootWalletProfilePath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath + "/" + uuid
    }

    fun changeAddressName(address: String, name: String): Boolean {
        if (addressNameMap == null) {
            addressNameMap = HashMap()
        }
        addressNameMap?.put(address, name)
        return saveToFile()
    }

    fun getAddressName(address: String) =
        addressNameMap?.get(address)
            ?: (ViteConfig.get().context as? ViteApplication)?.getTopActivity()
                ?.getString(R.string.unname) ?: "Untitled"

    val nowMaxIndex get() = allIndex.lastIndex

    val nowEthMaxIndex get() = allEthIndex.lastIndex

    fun deriveNewViteAddress(extensionWord: String = ""): Pair<Int, String>? {
        return try {
            val newIndex = nowMaxIndex + 1
            val result =
                Wallet.instance.deriveByIndex(entropyStore, newIndex.toLong(), extensionWord)
            AddressCache.put(entropyStore, newIndex, result.address)
            allIndex.add(newIndex)
            if (!saveToFile()) {
                // rollback
                allIndex.removeAt(allIndex.lastIndex)
            }
            Pair(newIndex, result.address)
        } catch (e: Exception) {
            loge(e)
            null
        }
    }

    fun deriveNewEthAddress(extensionWord: String = ""): Pair<Int, String>? {
        return try {
            val newIndex = nowEthMaxIndex + 1

            val path = EthBip44CommunityPrefix + newIndex

            val privateKey = Wallet.instance.deriveEthHexPrivateKeyByBip44(
                entropyStore,
                path,
                extensionWord
            )

            val ecKeyPair = ECKeyPair.create(privateKey.hexToBytes())
            val result = Keys.toChecksumAddress(Keys.getAddress(ecKeyPair))

            allEthIndex.add(newIndex)
            ethBip44Paths?.add(path)

            if (!saveToFile()) {
                // rollback
                allEthIndex.removeAt(allEthIndex.lastIndex)
            }

            Pair(newIndex, result)
        } catch (e: Exception) {
            loge(e)
            null
        }
    }

    fun nowDerivedViteAddresses(): List<String> {
        val addrs = ArrayList<String>()
        for (i in 0..nowMaxIndex) {
            getViteAddressByBip44Index(i)?.let {
                addrs.add(it)
            }
        }
        return addrs
    }

    fun nowDerivedEthAddresses(): List<String> {
        val addrs = ArrayList<String>()
        for (i in 0..nowEthMaxIndex) {
            getEthAddressByBip44Index(i)?.let {
                addrs.add(it)
            }
        }
        return addrs
    }

    fun getViteAddressByBip44Index(index: Int, extensionWord: String = ""): String? {
        return try {
            val result = Wallet.instance.deriveByIndex(entropyStore, index.toLong(), extensionWord)
            result.address
        } catch (e: Exception) {
            null
        }
    }

    fun getEthAddressByBip44Index(index: Int, extensionWord: String = ""): String? {
        return try {
            val privateKey = Wallet.instance.deriveEthHexPrivateKeyByBip44(
                entropyStore,
                EthBip44CommunityPrefix + index,
                extensionWord
            )
            val ecKeyPair = ECKeyPair.create(privateKey.hexToBytes())
            Keys.toChecksumAddress(Keys.getAddress(ecKeyPair))
        } catch (e: Exception) {
            null
        }
    }

    fun changeViteByBip44Index(index: Int, extensionWord: String = ""): String? {
        return try {
            val result = Wallet.instance.deriveByIndex(entropyStore, index.toLong(), extensionWord)
            this.index = index
            saveToFile()
            result.address
        } catch (e: Exception) {
            loge(e)
            null
        }
    }

    fun changeEthByBip44Index(index: Int, extensionWord: String = ""): String? {
        return try {

            nowEthPath = if (nowEthPath == null) {
                EthBip44CommunityDefault
            } else {
                EthBip44CommunityPrefix + index
            }

            val privateKey = Wallet.instance.deriveEthHexPrivateKeyByBip44(
                entropyStore,
                nowEthPath,
                extensionWord
            )
            val ecKeyPair = ECKeyPair.create(privateKey.hexToBytes())
            val result = Keys.toChecksumAddress(Keys.getAddress(ecKeyPair))

            this.ethIndex = index

            saveToFile()

            result
        } catch (e: Exception) {
            loge(e)
            null
        }
    }

    fun saveToFile(): Boolean {
        val json = Gson().toJson(this)
        return try {
            File(filename()).writeText(json)
            true
        } catch (e: IOException) {
            loge(e)
            false
        }
    }

    fun deleteEntropyFile() {
        val file = File(ViteConfig.get().viteRootWalletPath, entropyStore)
        val fileName = file.name
        file.deleteRecursively()
        logi(fileName, "DeleteAccount deleteEntropyFile")
    }

    fun deleteEntropyProfileFile(): Boolean {
        return try {
            val fileName = filename()
            File(fileName).deleteRecursively()
            logi(fileName, "DeleteAccount deleteEntropyProfileFile")
            true
        } catch (e: Exception) {
            loge(e)
            false
        }
    }

    @Synchronized
    fun signVite(hexData: String, extensionWord: String = ""): SignDataResult {
        val result = Wallet.instance.deriveByIndex(entropyStore, this.index.toLong(), extensionWord)
        return Mobile.signData(result.privateKey, hexData.hexToBytes())
    }


    @Synchronized
    fun nowViteAddress(extensionWord: String = ""): String? {
        return try {
            AddressCache.get(entropyStore, this.index) ?: Wallet.instance.deriveByIndex(
                entropyStore,
                this.index.toLong(),
                extensionWord
            ).address
        } catch (e: Exception) {
            loge(e)
            null
        }?.also {
            AddressCache.put(entropyStore, this.index, it)
        }
    }

    @Synchronized
    fun viteAddressByIndex(index: Int, extensionWord: String = ""): String? {
        return try {
            val result =
                Wallet.instance.deriveByIndex(entropyStore, this.index.toLong(), extensionWord)
            result.address
        } catch (e: Exception) {
            loge(e)
            null
        }
    }

    @Synchronized
    fun vitePrivByIndex(index: Int, extensionWord: String = ""): ByteArray? {
        return try {
            val result =
                Wallet.instance.deriveByIndex(entropyStore, index.toLong(), extensionWord)
            result.privateKey
        } catch (e: Exception) {
            loge(e)
            null
        }
    }

    @Synchronized
    fun nowVitePriv(extensionWord: String = ""): ByteArray? {
        return vitePrivByIndex(this.index)
    }

    @Synchronized
    fun firstVitePriv(extensionWord: String = ""): ByteArray? {
        return vitePrivByIndex(0)
    }

    @Synchronized
    fun firstViteAddress(extensionWord: String = ""): String? {
        return viteAddressByIndex(0)
    }

    @Synchronized
    fun nowEthPrivateKey(extensionWord: String = ""): String? {
        return try {
            if (nowEthPath == null) {
                nowEthPath = EthBip44CommunityDefault
                saveToFile()
            }

            Wallet.instance.deriveEthHexPrivateKeyByBip44(entropyStore, nowEthPath, extensionWord)
        } catch (e: Exception) {
            loge(e)
            null
        }
    }

    @Synchronized
    fun nowEthAddress(extensionWord: String = ""): String? {
        return try {

            if (nowEthPath == null) {
                nowEthPath = EthBip44CommunityDefault
                saveToFile()
            } else {
                nowEthPath = EthBip44CommunityPrefix + ethIndex
            }

            val privateKey = Wallet.instance.deriveEthHexPrivateKeyByBip44(
                entropyStore,
                nowEthPath,
                extensionWord
            )
            val ecKeyPair = ECKeyPair.create(privateKey.hexToBytes())
            val result = Keys.toChecksumAddress(Keys.getAddress(ecKeyPair))
            result
        } catch (e: Exception) {
            loge(e)
            null
        }
    }
}
