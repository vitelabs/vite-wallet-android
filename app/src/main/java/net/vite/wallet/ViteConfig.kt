package net.vite.wallet

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.setup.LanguageUtils
import net.vite.wallet.utils.isChinese
import java.util.*

const val ENV_PRODUCT = 0
const val ENV_STAGE = 1
const val ENV_TEST = 2


const val CURRENCY_CNY = "CNY"
const val CURRENCY_USD = "USD"
const val CURRENCY_RUB = "RUB"
const val CURRENCY_KRW = "KRW"
const val CURRENCY_TRY = "TRY"
const val CURRENCY_VND = "VND"
const val CURRENCY_EUR = "EUR"
const val CURRENCY_GBP = "GBP"

class ViteConfig(val context: Context, val channel: String) {
    val currentEnv: Int
    val viteExternalRootPath: String

    val viteRootPath: String
    val viteLogCachePath: String
    val viteLogPath: String
    val viteRootLogName: String
    val viteRootWalletPath: String
    val viteRootWalletProfilePath: String
    val viteRootSsFilePath: String
    val viteExternalHiddenRootFilePath: String

    val globalConfig: SharedPreferences
    val kvstore: SharedPreferences

    val viteCacheDir: String

    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    val versionCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode

    fun switchEnv(env: Int) {
        globalConfig.edit().putInt("env", env).commit()
    }

    fun showEnvSwitchBtn(show: Boolean) {
        globalConfig.edit().putBoolean("showEnvSwitchBtn", show).commit()
    }

    fun needShowEnvSwitchBtn() = globalConfig.getBoolean("showEnvSwitchBtn", false)


    fun getUniversalUuidOrGenerate(): String {
        return if (kvstore.contains("app_universal_uuid")) {
            kvstore.getString("app_universal_uuid", "") ?: ""
        } else {
            val uuid = UUID.randomUUID().toString()
            kvstore.edit().putString("app_universal_uuid", uuid).apply()
            uuid
        }
    }

    fun currentCurrency(): String {
        val userSetCurrency = kvstore.getString("userSetCurrency", "") ?: ""
        return if (userSetCurrency.isBlank()) {
            return when (LanguageUtils.currentLang(context)) {
                LanguageUtils.Turkish -> CURRENCY_TRY
                LanguageUtils.Chinese -> CURRENCY_CNY
                LanguageUtils.English -> CURRENCY_USD
                LanguageUtils.Filipino -> CURRENCY_USD
                LanguageUtils.Vietnamese -> CURRENCY_VND
                LanguageUtils.French -> CURRENCY_EUR
                LanguageUtils.Korean -> CURRENCY_KRW
                LanguageUtils.Spanish -> CURRENCY_EUR
                LanguageUtils.Russian -> CURRENCY_RUB
                else -> if (context.isChinese()) {
                    CURRENCY_CNY
                } else {
                    CURRENCY_USD
                }
            }
        } else {
            userSetCurrency
        }
    }

    fun currentCurrencySymbol(): String {
        return when (currentCurrency()) {
            CURRENCY_CNY -> "￥"
            CURRENCY_USD -> "$"
            CURRENCY_RUB -> "₽"
            CURRENCY_KRW -> "₩"
            CURRENCY_TRY -> "₺"
            CURRENCY_VND -> "₫"
            CURRENCY_EUR -> "€"
            CURRENCY_GBP -> "£"
            else -> ""
        }
    }

    fun changeCurrency(currency: String) {
        kvstore.edit().putString("userSetCurrency", currency).apply()
    }

    init {
        globalConfig = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        currentEnv = globalConfig.getInt("env", ENV_PRODUCT)

        kvstore = context.getSharedPreferences(
            if (currentEnv == ENV_PRODUCT) {
                "kv"
            } else {
                "kv$currentEnv"
            }, Context.MODE_PRIVATE
        )

        viteExternalRootPath =
            Environment.getExternalStorageDirectory().absolutePath + "/0aVite" + when (currentEnv) {
                ENV_PRODUCT -> ""
                ENV_STAGE -> "/stage"
                ENV_TEST -> "/test"
                else -> {
                    ""
                }
            }

        viteRootPath = context.filesDir.absolutePath + when (currentEnv) {
            ENV_PRODUCT -> ""
            ENV_STAGE -> "/stage"
            ENV_TEST -> "/test"
            else -> {
                ""
            }
        } + "/vite"

        viteLogPath = "$viteRootPath/vitelog"
        viteLogCachePath = "$viteRootPath/vitelogcahe"
        viteRootLogName = "$viteLogPath/mainLog.log"
        viteRootWalletPath = "$viteRootPath/wallet"
        viteRootWalletProfilePath = "$viteRootWalletPath/profile"
        viteRootSsFilePath = "$viteRootWalletPath/ssfiles"

        viteExternalHiddenRootFilePath =
            Environment.getExternalStorageDirectory().absolutePath + when (currentEnv) {
                ENV_PRODUCT -> "/.vite"
                ENV_STAGE -> "/.vitestage"
                ENV_TEST -> "/.vitetest"
                else -> {
                    "/.vite"
                }
            }

        viteCacheDir = "${context.cacheDir}/${
            when (currentEnv) {
                ENV_PRODUCT -> ""
                ENV_STAGE -> "stage"
                ENV_TEST -> "test"
                else -> {
                    ""
                }
            }
        }"
    }

    companion object {
        private var instance: ViteConfig? = null
        fun build(context: Context, channel: String) {
            instance = ViteConfig(context, channel)
            NetConfigHolder.init()
        }

        fun get(): ViteConfig {
            return instance!!
        }
    }

}