package net.vite.wallet.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.vite.wallet.ViteConfig
import net.vite.wallet.utils.hexToBytes
import net.vite.wallet.utils.toHex
import org.vitelabs.mobile.Mobile
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

@Keep
private data class EncryptParams(
    @SerializedName("ciphername") val CipherName: String,
    @SerializedName("ciphertext") val CipherText: String,
    @SerializedName("iv") val Iv: String,
    @SerializedName("timestamp") val Timestamp: Long = System.currentTimeMillis()
)

class SecurityStore(val dirname: String, val alias: String) {

    private fun filename(name: String): String {
        val dir = File(dirname)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir.absolutePath, Mobile.hash(16, name.toByteArray()).toHex()).absolutePath
    }


    fun getAesKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        return if (!ks.containsAlias(alias)) {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGen.init(
                KeyGenParameterSpec
                    .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .apply {
                        setKeySize(128)
                        setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    }.build()
            )
            keyGen.generateKey()
        } else {
            val entry = ks.getEntry(alias, null) as KeyStore.SecretKeyEntry
            entry.secretKey
        }
    }

    fun store(key: String, value: ByteArray): Boolean {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getAesKey())
            val cipherBytes = cipher.doFinal(value)
            val json = Gson().toJson(EncryptParams("AES/GCM/NoPadding", cipherBytes.toHex(), cipher.iv.toHex()))
            File(filename(key)).writeText(json)
            true
        } catch (e: Exception) {
            false
        }
    }


    fun get(key: String): ByteArray? {
        return try {
            val file = File(filename(key))
            if (!file.exists()) {
                return null
            }
            val cipherJson = file.readText()
            val encryptParams = Gson().fromJson(cipherJson, EncryptParams::class.java)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getAesKey(), GCMParameterSpec(128, encryptParams.Iv.hexToBytes()))

            val plainBytes = cipher.doFinal(encryptParams.CipherText.hexToBytes())
            plainBytes

        } catch (e: Exception) {
            null
        }
    }

    fun delete(key: String) {
        try {
            File(filename(key)).delete()
        } catch (e: Exception) {
        }
    }
}

object GlobalSecurityStore {
    const val DefaultAlias = "vite-root-pub"

    val ss = SecurityStore(ViteConfig.get().viteRootSsFilePath, DefaultAlias)

    fun store(key: String, value: ByteArray) = ss.store(key, value)

    fun get(key: String) = ss.get(key)

    fun delete(key: String) = ss.delete(key)
}