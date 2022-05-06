package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.widget_balance_detail_single_btn.view.*
import net.vite.wallet.R

class BalanceDetailSingleExtraFuncWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.widget_balance_detail_single_btn, this)

    fun setImg(@DrawableRes resId: Int) {
        leftImg.setImageResource(resId)
    }

    fun setText(text: String) {
        btnText.text = text
    }

    fun setText(@StringRes resId: Int) {
        btnText.setText(resId)
    }



}