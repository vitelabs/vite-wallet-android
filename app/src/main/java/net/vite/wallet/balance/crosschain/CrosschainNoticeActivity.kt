package net.vite.wallet.balance.crosschain

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_crosschain_notice.*
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.crosschain.deposit.NewDepositByOtherAppActivity
import net.vite.wallet.balance.crosschain.withdraw.NewWithdrawActivity
import net.vite.wallet.dialog.ProgressDialog
import net.vite.wallet.network.http.gw.GwCrosschainApi
import net.vite.wallet.network.http.gw.MetaInfoResp
import net.vite.wallet.network.http.gw.OverChainApiException
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.showDialogMsg
import net.vite.wallet.utils.showToast

class CrosschainNoticeActivity : UnchangableAccountAwareActivity() {

    companion object {

        const val REQUEST_WITHDRAW = 101
        const val REQUEST_DEPOSIT = 100
        fun show(activity: Activity, tokenCode: String, requestCode: Int) {
            activity.startActivity(Intent(activity, CrosschainNoticeActivity::class.java).apply {
                putExtra("tokenCode", tokenCode)
                putExtra("requestCode", requestCode)
            })
        }
    }

    val cycleProgressBar by lazy {
        ProgressDialog(this)
    }

    val disposable = CompositeDisposable()

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crosschain_notice)

        val tokenCode = intent.getStringExtra("tokenCode")!!
        val normalTokenInfo = TokenInfoCenter.getTokenInfoInCache(tokenCode) ?: kotlin.run {
            showToast("can not find tokenInfo with$tokenCode")
            finish()
            return
        }
        val tokenGatewayInfo = normalTokenInfo.gatewayInfo ?: kotlin.run {
            finish()
            return
        }

        val tokenGatewayInfoUrl = tokenGatewayInfo.url ?: kotlin.run {
            finish()
            return
        }

        if (tokenGatewayInfo.isOfficial == true) {
            val textString = getString(
                R.string.crosschain_main_notice_official,
                tokenGatewayInfo.name ?: "",
                getString(R.string.crosschain_term_link)
            )
            val clickableText = getString(R.string.crosschain_term_link)
            makeTextClickable(mainNotice, textString, clickableText, tokenGatewayInfo.getPolicy())
        } else {
            val textString = getString(
                R.string.crosschain_main_notice,
                tokenGatewayInfo.name ?: "",
                getString(R.string.crosschain_term_link)
            )
            val clickableText = getString(R.string.crosschain_term_link)
            makeTextClickable(mainNotice, textString, clickableText, tokenGatewayInfo.getPolicy())
        }

        kotlin.run {
            val textString = "\n" + getString(
                R.string.crosschain_contact_us,
                tokenGatewayInfo.name ?: "",
                tokenGatewayInfo.serviceSupport ?: ""
            )
            val clickableText = tokenGatewayInfo.serviceSupport ?: ""
            makeTextClickable(
                supportText,
                textString,
                clickableText,
                tokenGatewayInfo.serviceSupport ?: "",
                clickableText.contains("@")
            )
        }

        kotlin.run {
            val textString = getString(
                R.string.crosschain_next_notice,
                tokenGatewayInfo.name ?: "",
                getString(R.string.crosschain_term_link)
            )
            val clickableText = getString(R.string.crosschain_term_link)
            makeTextClickable(
                nextNoticeText,
                textString,
                clickableText,
                tokenGatewayInfo.getPolicy()
            )
        }

        nextBtn.setOnClickListener {
            disposable.clear()
            if (!cycleProgressBar.isShowing) {
                cycleProgressBar.show()
            }

            val errorHandler: (Throwable) -> Unit = {
                cycleProgressBar.dismiss()
                if (it is OverChainApiException) {
                    it.showErrorDialog(this) {
                        finish()
                    }
                } else {
                    it.showDialogMsg(this)
                }
            }
            val requestCode = intent.getIntExtra("requestCode", 0)
            if (requestCode == REQUEST_DEPOSIT) {
                disposable.add(
                    GwCrosschainApi.depositInfo(
                        normalTokenInfo.tokenAddress!!,
                        currentAccount.nowViteAddress()!!,
                        tokenGatewayInfoUrl
                    ).subscribe({
                        disposable.add(
                            GwCrosschainApi.metaInfo(normalTokenInfo.tokenAddress, tokenGatewayInfoUrl).subscribe({
                                cycleProgressBar.dismiss()
                                if (it.depositState == MetaInfoResp.OPEN) {
                                    NewDepositByOtherAppActivity.show(
                                        this,
                                        normalTokenInfo.tokenAddress,
                                        currentAccount.nowViteAddress()!!
                                    )
                                    finish()
                                } else {
                                    it.showCheckDialog(this) {
                                        it.dismiss()
                                        finish()
                                    }
                                }
                            }, errorHandler)
                        )
                    }, errorHandler)
                )
            } else if (requestCode == REQUEST_WITHDRAW) {
                disposable.add(
                    GwCrosschainApi.withdrawInfo(
                        normalTokenInfo.tokenAddress!!,
                        currentAccount.nowViteAddress()!!,
                        tokenGatewayInfoUrl
                    ).subscribe({
                        disposable.add(
                            GwCrosschainApi.metaInfo(normalTokenInfo.tokenAddress, tokenGatewayInfoUrl).subscribe(
                                {
                                    cycleProgressBar.dismiss()
                                    if (it.withdrawState == MetaInfoResp.OPEN) {
                                        NewWithdrawActivity.show(this, normalTokenInfo.tokenCode!!)
                                        finish()
                                    } else {
                                        it.showCheckDialog(this) {
                                            it.dismiss()
                                            finish()
                                        }
                                    }
                                },
                                errorHandler
                            )
                        )
                    }, errorHandler)
                )
            }
        }
    }


    private fun makeTextClickable(
        textView: TextView,
        originText: String,
        clickableText: String,
        jumpUrl: String,
        isEmail: Boolean = false
    ) {
        textView.text = originText
        val clickableSpan = object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                ds.color = resources.getColor(R.color.viteblue)
                ds.isUnderlineText = true
            }

            override fun onClick(widget: View) {
                try {
                    if (isEmail) {
                        val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$jumpUrl"))
                        startActivity(Intent.createChooser(i, getString(R.string.get_support)))
                    } else {
                        startActivity(createBrowserIntent(jumpUrl))
                    }
                } catch (e: Exception) {
                }
            }
        }
        val spannableBuilder = SpannableStringBuilder()
        val start = originText.indexOf(clickableText)
        val end = start + clickableText.length
        spannableBuilder.append(originText)
            .setSpan(clickableSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.text = spannableBuilder
    }

    override fun onStop() {
        super.onStop()
        cycleProgressBar.dismiss()
        disposable.clear()
    }
}
