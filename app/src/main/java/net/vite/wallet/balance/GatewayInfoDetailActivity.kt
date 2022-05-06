package net.vite.wallet.balance

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_gatewayinfo_detail.*
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.ui.widget.LeftBottomTextUnderlineLayout
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.isChinese

class GatewayInfoDetailActivity : UnchangableAccountAwareActivity() {
    companion object {
        fun show(context: Context, tokenCode: String) {
            context.startActivity(Intent(context, GatewayInfoDetailActivity::class.java).apply {
                putExtra("tokenCode", tokenCode)
            })
        }
    }

    override fun startActivity(intent: Intent?) {
        kotlin.runCatching { super.startActivity(intent) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gatewayinfo_detail)
        val tokenCode = intent.getStringExtra("tokenCode") ?: run {
            finish()
            return
        }
        val tokenDetail = TokenInfoCenter.getTokenInfoInCache(tokenCode) ?: run {
            finish()
            return
        }
        val gatewayInfo = tokenDetail.gatewayInfo ?: kotlin.run {
            finish()
            return
        }
        tokenWidget.setup(tokenDetail.icon)

        val nameItem = LeftBottomTextUnderlineLayout(this).apply {
            setLeftText(R.string.gateway_name_title)
            setBottomText(gatewayInfo.name ?: "--")
        }
        mainItems.addView(nameItem)

        val officialItem = LeftBottomTextUnderlineLayout(this).apply {
            setLeftText(R.string.gateway_official_web)

            kotlin.runCatching { gatewayInfo.links?.get("website")?.get(0) }.getOrNull()?.let { url ->
                setBottomText(url, Color.parseColor("#ff007aff"))
                setOnClickListener {
                    startActivity(createBrowserIntent(url))
                }
            } ?: kotlin.run {
                setBottomText("--")
            }
        }
        mainItems.addView(officialItem)


        val overviewtem = LeftBottomTextUnderlineLayout(this).apply {
            setLeftText(R.string.gateway_overview)
            val overview = if (isChinese()) {
                gatewayInfo.overview?.get("zh")
            } else {
                gatewayInfo.overview?.get("en")
            }
            setBottomText(overview ?: "--")
        }
        mainItems.addView(overviewtem)


        val emailItem = LeftBottomTextUnderlineLayout(this).apply {
            setLeftText(R.string.commit_ticket)
            gatewayInfo.serviceSupport?.let { url ->
                setBottomText(url, Color.parseColor("#ff007aff"))
                setOnClickListener {

                    if (url.contains("@")) {
                        val mailtoUrl = if (url.trim().startsWith("mailto:")) {
                            url.trim()
                        } else {
                            "mailto:${url.trim()}"
                        }
                        try {
                            val i = Intent(Intent.ACTION_SENDTO, Uri.parse(mailtoUrl))
                            startActivity(
                                Intent.createChooser(
                                    i,
                                    getString(R.string.commit_ticket)
                                )
                            )
                        } catch (e: Exception) {

                        }
                    } else {
                        startActivity(createBrowserIntent(url))
                    }
                }
            } ?: kotlin.run {
                setBottomText("--")
            }
        }
        mainItems.addView(emailItem)

        val policyItem = LeftBottomTextUnderlineLayout(this).apply {
            setLeftText(R.string.gateway_policy_title)
            if (gatewayInfo.getPolicy().isEmpty()) {
                setBottomText("--")
            } else {
                setBottomText(gatewayInfo.getPolicy(), Color.parseColor("#ff007aff"))
                setOnClickListener {
                    startActivity(createBrowserIntent(gatewayInfo.getPolicy()))
                }
            }
        }
        mainItems.addView(policyItem)

        val gatewayLink = LeftBottomTextUnderlineLayout(this).apply {
            setLeftText(R.string.gateway_link_title)
            gatewayInfo.url?.let { url ->
                setBottomText(url, Color.parseColor("#ff007aff"))
                setOnClickListener {
                    startActivity(createBrowserIntent(url))
                }
            } ?: kotlin.run {
                setBottomText("--")
            }
        }
        mainItems.addView(gatewayLink)
    }
}