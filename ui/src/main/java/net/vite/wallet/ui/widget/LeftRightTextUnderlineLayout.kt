package net.vite.wallet.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.left_right_text_underline_layout.view.*
import net.vite.wallet.kline.R

class LeftRightTextUnderlineLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.left_right_text_underline_layout, this)
    fun setLeftText(text: String) {
        leftText.text = text
    }

    fun setLeftText(@StringRes textId: Int) {
        leftText.setText(textId)
    }

    fun setRightText(text: String, @ColorInt color: Int = Color.parseColor("#3E4A59")) {
        rightText.visibility = View.VISIBLE
        rightText.text = text
        rightText.setTextColor(color)
    }

    fun setRightText(@StringRes textId: Int, @ColorInt color: Int = Color.parseColor("#3E4A59")) {
        rightText.visibility = View.VISIBLE
        rightText.setText(textId)
        rightText.setTextColor(color)
    }

    fun setRightImage(@DrawableRes drawableRes: Int) {
        rigthImg.visibility = View.VISIBLE
        rigthImg.setImageResource(drawableRes)
    }
}