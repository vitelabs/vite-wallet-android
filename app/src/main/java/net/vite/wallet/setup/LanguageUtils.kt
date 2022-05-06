package net.vite.wallet.setup

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import net.vite.wallet.utils.isChinese
import java.util.*

object LanguageUtils {

    val Chinese = "zh_CN"
    val English = "en"
    val Turkish = "tr"
    val Filipino = "fil"
    val French = "fr"
    val Korean = "ko"
    val Russian = "ru"
    val Vietnamese = "vi"
    val Spanish = "es"

    val LocalMap = mapOf(
        Chinese to Locale.SIMPLIFIED_CHINESE,
        English to Locale.ENGLISH,
        Turkish to Locale(Turkish),
        Filipino to Locale(Filipino),
        French to Locale.FRENCH,
        Korean to Locale.KOREAN,
        Russian to Locale(Russian),
        Vietnamese to Locale(Vietnamese),
        Spanish to Locale("es", "ES")
    )

    fun storeLanguage(context: Context, lang: String) {
        context.getSharedPreferences("globallang", Context.MODE_PRIVATE)
            .edit()
            .putString("userSetLanguage", lang)
            .apply()
    }


    fun currentLang(context: Context): String {
        return context.getSharedPreferences(
            "globallang",
            Context.MODE_PRIVATE
        ).getString("userSetLanguage", "")!!
    }

    fun onAttach(context: Context): Context {
        return setLocale(context, currentLang(context))
    }

    fun setLocale(context: Context, language: String): Context {
        val local = if (language == "") {
            if (context.isChinese()) {
                Locale.SIMPLIFIED_CHINESE
            } else {
                Locale.ENGLISH
            }
        } else {
            storeLanguage(context, language)
            LocalMap[language] ?: throw IllegalArgumentException("unknown language $language")
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, local)
        } else updateResourcesLegacy(context, local)

    }


    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val configuration = Configuration()
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale
        configuration.setLayoutDirection(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }
}
