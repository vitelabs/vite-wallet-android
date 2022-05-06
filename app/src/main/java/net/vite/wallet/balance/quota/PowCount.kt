package net.vite.wallet.balance.quota

import android.annotation.SuppressLint
import android.util.Log
import com.google.gson.Gson
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.ViteConfig
import net.vite.wallet.network.http.configbucket.ConfigBucketApi
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat


object PowCount {

    private val dateFormat = SimpleDateFormat("yyyyMMdd")
    var powCount = 100
        private set

    @SuppressLint("CheckResult")
    fun fetchPowMaxLimit() {
        clean()
        Thread {
            try {
                val r = Request.Builder()
                    .url("https://static1.vite.net/config/ConfigHash.json")
                    .build()
                val response = OkHttpClient.Builder().build().newCall(r).execute()
                val bodyString = response.body()?.string() ?: ""
                powCount = JSONObject(bodyString).getInt("getPowTimesPreDay")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        clean()
    }

    fun usePow(address: String) {
        clean()
        ViteConfig.get().kvstore.edit().putLong("powcount_time", System.currentTimeMillis()).apply()
        val used = ViteConfig.get().kvstore.getInt("powcount_$address", 0)
        ViteConfig.get().kvstore.edit().putInt("powcount_$address", used + 1).apply()
    }

    fun isExceedPow(address: String): Boolean {
        clean()
        return ViteConfig.get().kvstore.getInt("powcount_$address", 0) >= powCount
    }

    private fun clean() {
        val powTime = ViteConfig.get().kvstore.getLong("powcount_time", 0)
        if (dateFormat.format(powTime) != dateFormat.format(System.currentTimeMillis())) {
            val keys = mutableListOf<String>()
            ViteConfig.get().kvstore.all.keys.forEach {
                if (it.startsWith("powcount_")) {
                    keys.add(it)
                }
            }
            keys.forEach {
                ViteConfig.get().kvstore.edit().remove(it).apply()
            }
        }
    }

}