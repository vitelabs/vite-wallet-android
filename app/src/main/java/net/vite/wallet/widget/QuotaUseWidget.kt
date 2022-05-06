package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import net.vite.wallet.R
import net.vite.wallet.ui.widget.TextRightIconEditText
import net.vite.wallet.utils.createBrowserIntent

class QuotaUseWidget(context: Context, attrs: AttributeSet) : TextRightIconEditText(context, attrs) {
    init {
        setText(R.string.quota_cost_tag)
        isEnabled = false
        customEndText = "-- Quota"
        customRightDrawable = R.mipmap.infor
        setOnRightDrawableClickListener {
            //            context.startActivity(createBrowserIntent("https://app.vite.net/quotaDefinition/?localize=${context.currentLang()}"))
            context.startActivity(createBrowserIntent("https://vite-static-pages.netlify.com/quota/en/quota.html"))
        }
    }

    fun setQuotaNeedCost(quota: String) {
        customEndText = "$quota Quota"
    }

}