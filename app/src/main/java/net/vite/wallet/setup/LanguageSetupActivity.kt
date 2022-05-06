package net.vite.wallet.setup

import android.os.Bundle
import android.view.View
import net.vite.wallet.R
import net.vite.wallet.ViteApplication
import net.vite.wallet.account.UnchangableAccountAwareActivity

class LanguageSetupActivity : UnchangableAccountAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_list)
    }

    fun onLanguageClick(v: View) {
        val lang = when (v.id) {
            R.id.langChinese -> LanguageUtils.Chinese
            R.id.langEnglish -> LanguageUtils.English
            R.id.langKorean -> LanguageUtils.Korean
            R.id.langPilipino -> LanguageUtils.Filipino
            R.id.langRussian -> LanguageUtils.Russian
            R.id.langVietnamese -> LanguageUtils.Vietnamese
            R.id.langTurkish -> LanguageUtils.Turkish
            R.id.langSpanish -> LanguageUtils.Spanish
            else -> ""
        }
        if (lang == "") return
        if (LanguageUtils.currentLang(this) != lang) {
            LanguageUtils.setLocale(this, lang)
            restartApp()
        } else {
            finish()
        }
    }

    fun restartApp() {
        if (application is ViteApplication) {
            (application as ViteApplication).restartApp()
        }
    }

}