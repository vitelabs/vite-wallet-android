package net.vite.wallet.balance.tokenselect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_add_token.*
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.network.http.vitex.TokenFamily


class TokenSelectActivity : UnchangableAccountAwareActivity() {
    private lateinit var tokenSearchVM: TokenSearchVM
    private val accountTokenManager = AccountCenter.getCurrentAccountTokenManager()!!


    companion object {
        private const val FILTER_EXCHANGE = "exchange"

        fun show(context: Context, filter: String) {
            context.startActivity(Intent(context, TokenSelectActivity::class.java).apply {
                putExtra(FILTER_EXCHANGE, filter)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = intent.getStringExtra(FILTER_EXCHANGE)

        setContentView(R.layout.activity_add_token)
        tokenSearchVM = ViewModelProviders.of(this)[TokenSearchVM::class.java]

        val adapter = TokenSelectAdapter(Glide.with(this), accountTokenManager)
        if (filter != null) adapter.fromExchange = true
        adapter.setTokenInfos(if (filter == null) TokenInfoCenter.getAllOrderedTokenInfos() else TokenInfoCenter.getAllOrderedTokenInfos().filter {
            it.family() == TokenFamily.VITE
        })

        mainList.adapter = adapter

        tokenSearchVM.fuzzyQueryLd.observe(this, Observer {
            cycleProgressBar.visibility = it.progressVisible
            if (it.isLoading()) {
                return@Observer
            }
            it.resp?.let {
                adapter.setTokenInfos(if (filter == null) it else it.filter {
                    it.family() == TokenFamily.VITE
                })
                adapter.notifyDataSetChanged()
            }
        })

        tokenSearchVM.refreshCompleteLd.observe(this, Observer {
            cycleProgressBar.visibility = it.progressVisible
            if (it.isLoading()) {
                return@Observer
            }

            it.resp?.let {
                adapter.setTokenInfos(if (filter == null) it else it.filter {
                    it.family() == TokenFamily.VITE
                })
                adapter.notifyDataSetChanged()
            }

        })

        tokenSearchVM.refreshAllInfo()

        searchTokenEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    adapter.setTokenInfos(if (filter == null) TokenInfoCenter.getDefaultTokenInfoList() else TokenInfoCenter.getDefaultTokenInfoList().filter {
                        it.family() == TokenFamily.VITE
                    })
                    adapter.notifyDataSetChanged()
                } else {
                    tokenSearchVM.fuzzyQuery(s.toString())
                }
            }

        })

    }

}