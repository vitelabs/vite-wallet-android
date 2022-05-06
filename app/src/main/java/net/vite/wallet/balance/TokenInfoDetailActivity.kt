package net.vite.wallet.balance

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_tokeninfo_detail.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.toLocalReadableTextWithThouands
import net.vite.wallet.ui.widget.LeftBottomTextUnderlineLayout
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.isChinese
import net.vite.wallet.widget.ItemTokenDetailMedium
import java.math.BigDecimal


class TokenInfoDetailActivity : UnchangableAccountAwareActivity() {
    companion object {
        fun show(context: Context, tokenCode: String) {
            context.startActivity(Intent(context, TokenInfoDetailActivity::class.java).apply {
                putExtra("tokenCode", tokenCode)
            })
        }
    }

    override fun startActivity(intent: Intent?) {
        kotlin.runCatching { super.startActivity(intent) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tokeninfo_detail)
        val queryCode = intent.getStringExtra("tokenCode") ?: run {
            finish()
            return
        }

        val queryTokenInfoDetailVm = ViewModelProviders.of(this)[QueryTokenInfoDetailVm::class.java]
        queryTokenInfoDetailVm.queryTokenInfoDetail(queryCode)

        queryTokenInfoDetailVm.networkState.observe(this, Observer {
            progressCycle.visibility = it.progressVisible
        })

        queryTokenInfoDetailVm.queryResultLd.observe(this, Observer { tokenDetail ->

            tokenWidget.setup(tokenDetail?.icon)

            val tokenAddr = tokenDetail.platform?.tokenAddress
            val url = when (tokenDetail.family()) {
                TokenFamily.ETH -> NetConfigHolder.netConfig.remoteEtherscanPrefix + if (tokenAddr != null) {
                    "/address/$tokenAddr"
                } else {
                    ""
                }
                TokenFamily.VITE -> if (tokenAddr == null) {
                    null
                } else {
                    "${NetConfigHolder.netConfig.explorerUrlPrefix}/token/$tokenAddr"
                }
                else -> null
            }

            val tokenNameItem = LeftBottomTextUnderlineLayout(this).apply {
                setLeftText(R.string.detail_token_name)
                val tokenSymbol = tokenDetail.symbol?.let { "($it)" } ?: ""
                tokenDetail.name?.let {
                    setBottomText("$it$tokenSymbol", Color.parseColor("#ff007aff"))
                } ?: kotlin.run {
                    setBottomText("--")
                }
                url?.let { url ->
                    setOnClickListener {
                        startActivity(createBrowserIntent(url))
                    }
                }
            }
            mainItems.addView(tokenNameItem)


            val tokenIdItem = LeftBottomTextUnderlineLayout(this).apply {
                setLeftText(R.string.detail_token_id)

                tokenAddr?.let {
                    setBottomText(it, Color.parseColor("#ff007aff"))
                } ?: kotlin.run {
                    setBottomText("--")
                }


                url?.let { url ->
                    setOnClickListener {
                        startActivity(createBrowserIntent(url))
                    }
                }
            }
            mainItems.addView(tokenIdItem)

            val overview = if (isChinese()) {
                tokenDetail.overview?.get("zh")
            } else {
                tokenDetail.overview?.get("en")
            }

            val overviewView = overview?.let {
                LeftBottomTextUnderlineLayout(this).apply {
                    setLeftText(R.string.detail_token_overview)
                    setBottomText(it)
                }
            } ?: kotlin.run {
                LeftBottomTextUnderlineLayout(this).apply {
                    setLeftText(R.string.detail_token_overview)
                    setBottomText("--")
                }
            }

            mainItems.addView(overviewView)


            val total = LeftBottomTextUnderlineLayout(this).apply {
                setLeftText(R.string.detail_total)
                tokenDetail.total?.toBigDecimalOrNull()?.let { totalInssuance ->

                    val totalReadable = kotlin.run {
                        val Thousand = BigDecimal.TEN.pow(3)
                        val Million = BigDecimal.TEN.pow(6)
                        val Billion = BigDecimal.TEN.pow(9)
                        val Trillion = BigDecimal.TEN.pow(12)

                        val million = totalInssuance.divide(Million)
                        if (million < BigDecimal.ONE) {
                            return@run totalInssuance.toLocalReadableTextWithThouands(4)
                        }

                        if (million >= BigDecimal.ONE && million < Thousand) {
                            return@run million.toLocalReadableTextWithThouands(4) + "Million"
                        }
                        val billion = totalInssuance.divide(Billion)
                        if (billion >= BigDecimal.ONE && billion < Thousand) {
                            return@run billion.toLocalReadableTextWithThouands(4) + "Billion"
                        }

                        val trillion = totalInssuance.divide(Trillion)
                        return@run trillion.toLocalReadableTextWithThouands(4) + "Trillion"

                    }

                    setBottomText(totalReadable)
                } ?: kotlin.run {
                    setBottomText("--")
                }
            }
            mainItems.addView(total)


            val gatewayTypeName = LeftBottomTextUnderlineLayout(this).apply {
                setLeftText(R.string.token_type)

                if (tokenDetail.isGatewayToken()) {
                    setBottomText(R.string.crosschain_token)
                } else {
                    setBottomText(R.string.raw_token)
                }
            }
            mainItems.addView(gatewayTypeName)


            if (tokenDetail.isGatewayToken()) {
                val gatewayName = LeftBottomTextUnderlineLayout(this).apply {
                    setLeftText(R.string.detail_gateway)
                    tokenDetail.gatewayInfo?.name?.let {
                        setBottomText(it, Color.parseColor("#ff007aff"))
                    } ?: kotlin.run {
                        setBottomText("--")
                    }
                    setOnClickListener {
                        GatewayInfoDetailActivity.show(
                            this@TokenInfoDetailActivity, tokenDetail.tokenCode ?: ""
                        )
                    }


                }

                mainItems.addView(gatewayName)
            }

            val officialUrlItem = LeftBottomTextUnderlineLayout(this).apply {
                setLeftText(R.string.official_net_title)
                tokenDetail.website?.let { url ->
                    setBottomText(url, Color.parseColor("#ff007aff"))
                    setOnClickListener {
                        startActivity(createBrowserIntent(url))
                    }
                } ?: kotlin.run {
                    setBottomText("--")
                }
            }
            mainItems.addView(officialUrlItem)


            val whitePaperItem = LeftBottomTextUnderlineLayout(this).apply {
                setLeftText(R.string.white_paper_title)
                tokenDetail.whitepaper?.let { url ->
                    setBottomText(url, Color.parseColor("#ff007aff"))
                    setOnClickListener {
                        startActivity(createBrowserIntent(url))
                    }
                } ?: kotlin.run {
                    setBottomText("--")
                }
            }
            mainItems.addView(whitePaperItem)

            val explorerItem = LeftBottomTextUnderlineLayout(this).apply {
                setLeftText(R.string.explorer)
                kotlin.runCatching {
                    tokenDetail.links?.get("explorer")?.get(0)
                }.getOrNull()?.let { url ->
                    setBottomText(url, Color.parseColor("#ff007aff"))
                    setOnClickListener {
                        startActivity(createBrowserIntent(url))
                    }
                } ?: kotlin.run {
                    setBottomText("--")
                }
            }
            mainItems.addView(explorerItem)


            val githubItem = LeftBottomTextUnderlineLayout(this).apply {
                setLeftText(R.string.github_title)
                kotlin.runCatching {
                    tokenDetail.links?.get("github")?.get(0)
                }.getOrNull()?.let { url ->
                    setBottomText(url, Color.parseColor("#ff007aff"))
                    setOnClickListener {
                        startActivity(createBrowserIntent(url))
                    }
                } ?: kotlin.run {
                    setBottomText("--")
                }
            }
            mainItems.addView(githubItem)


            val itemTokenDetailMedium = ItemTokenDetailMedium(this).apply {
                kotlin.runCatching { tokenDetail.links?.get("twitter")?.get(0) }.getOrNull()?.let {
                    setTwitter(it)
                }
                kotlin.runCatching { tokenDetail.links?.get("facebook")?.get(0) }.getOrNull()?.let {
                    setFacebook(it)
                }
                kotlin.runCatching { tokenDetail.links?.get("telegram")?.get(0) }.getOrNull()?.let {
                    setTelegram(it)
                }
            }

            mainItems.addView(itemTokenDetailMedium)

        })

    }
}
