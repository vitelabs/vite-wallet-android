package net.vite.wallet.balance.tokenselect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_add_token.mainList
import kotlinx.android.synthetic.main.activity_add_token.searchTokenEditText
import kotlinx.android.synthetic.main.activity_local_select_token.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.exchange.TickerStatisticsCenter


class LocalTokenSelectActivity : UnchangableAccountAwareActivity() {
    private lateinit var tokenSearchVM: TokenSearchVM

    companion object {
        const val FROM_WALLET = "fromWallet"
        const val OnlyShowCrossChainToken = "OnlyShowCrossChainToken"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val onlyShowCrossChainToken = intent.getBooleanExtra(OnlyShowCrossChainToken, false)
        val fromWallet = intent.getBooleanExtra(FROM_WALLET, false)

        setContentView(R.layout.activity_local_select_token)
        tokenSearchVM = ViewModelProviders.of(this)[TokenSearchVM::class.java]

        val adapter = LocalTokenSelectAdapter(this, fromWallet, onlyShowCrossChainToken)

        adapter.setDefaultTokenInfos(TickerStatisticsCenter.dexTokensCache)
        emptyGroup.visibility = View.GONE

        mainList.adapter = adapter

        tokenSearchVM.refreshAllInfo()

        cancelSelect.setOnClickListener {
            finish()
        }

        searchTokenEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    adapter.setDefaultTokenInfos(TickerStatisticsCenter.dexTokensCache)
                    emptyGroup.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                } else {
                    val result = adapter.orderedList.filter {
                        it.name?.contains(s, true) == true || it.uniqueName().contains(s, true)
                    }
                    if (result.isEmpty()) {
                        emptyGroup.visibility = View.VISIBLE
                    } else {
                        emptyGroup.visibility = View.GONE
                    }
                    adapter.setTokenInfos(result)
                    adapter.notifyDataSetChanged()
                }
            }
        })

    }

}