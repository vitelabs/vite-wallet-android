package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.layout_tokendetail_medium.view.*
import net.vite.wallet.R
import net.vite.wallet.utils.createBrowserIntent

class ItemTokenDetailMedium @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.layout_tokendetail_medium, this)

    fun setTwitter(url: String) {
        twitterIcon.visibility = View.VISIBLE
        twitterIcon.setOnClickListener {
            context.startActivity(createBrowserIntent(url))
        }
    }

    fun setFacebook(url: String) {
        facebookIcon.visibility = View.VISIBLE
        facebookIcon.setOnClickListener {
            context.startActivity(createBrowserIntent(url))
        }
    }

    fun setTelegram(url: String) {
        telegramIcon.visibility = View.VISIBLE
        telegramIcon.setOnClickListener {
            context.startActivity(createBrowserIntent(url))
        }
    }
}