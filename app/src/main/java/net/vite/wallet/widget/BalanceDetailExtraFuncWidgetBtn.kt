package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.widget_banlance_detail_extra_function_btn.view.*
import net.vite.wallet.R

class BalanceDetailExtraFuncWidgetBtn @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.widget_banlance_detail_extra_function_btn, this)

    fun setLeftImg(@DrawableRes resId: Int) {
        leftImg.setImageResource(resId)
    }

    fun setRightText(text: String) {
        rightText.text = text
    }

    fun setRightText(@StringRes resId: Int) {
        rightText.setText(resId)
    }


}