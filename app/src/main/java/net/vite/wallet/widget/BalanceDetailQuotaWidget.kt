package net.vite.wallet.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.widget_banlance_quota_info.view.*
import net.vite.wallet.R

class BalanceDetailQuotaWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.widget_banlance_quota_info, this)
    val getQuotaBtn = getQuota


    fun setUtpeAndCurrent(utpe: String, current: String) {
        if (utpe == "0") {
            valueOfQuotaAvailableAndMax.visibility = View.VISIBLE
            progressIncGroup.visibility = View.GONE
        } else {
            valueOfQuotaAvailableAndMax.visibility = View.GONE
            progressIncGroup.visibility = View.VISIBLE
            utProgressEnd.text = "${utpe}Quota"
            utProgressStart.text = "${current}Quota/"
            val utpeInt = utpe.toDoubleOrNull()?.toInt() ?: 0
            val currentInt = current.toDoubleOrNull()?.toInt() ?: 0
            quotaIncProgress.max = utpeInt
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                quotaIncProgress.setProgress(currentInt, true)
            } else {
                quotaIncProgress.progress = currentInt
            }
        }
    }


}