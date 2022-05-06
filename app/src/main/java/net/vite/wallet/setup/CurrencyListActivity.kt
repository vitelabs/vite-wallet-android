package net.vite.wallet.setup

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_currency_list.*
import net.vite.wallet.*
import net.vite.wallet.account.UnchangableAccountAwareActivity


class CurrencyListActivity : UnchangableAccountAwareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_list)


        currencyCNY.setOnClickListener {
            ViteConfig.get().changeCurrency(CURRENCY_CNY)
            finish()
        }
        currencyUSD.setOnClickListener {
            ViteConfig.get().changeCurrency(CURRENCY_USD)
            finish()
        }
        currencyRUB.setOnClickListener {
            ViteConfig.get().changeCurrency(CURRENCY_RUB)
            finish()
        }
        currencyKRW.setOnClickListener {
            ViteConfig.get().changeCurrency(CURRENCY_KRW)
            finish()
        }
        currencyTRY.setOnClickListener {
            ViteConfig.get().changeCurrency(CURRENCY_TRY)
            finish()
        }
        currencyVND.setOnClickListener {
            ViteConfig.get().changeCurrency(CURRENCY_VND)
            finish()
        }
        currencyEUR.setOnClickListener {
            ViteConfig.get().changeCurrency(CURRENCY_EUR)
            finish()
        }
        currencyGBP.setOnClickListener {
            ViteConfig.get().changeCurrency(CURRENCY_GBP)
            finish()
        }

    }


}