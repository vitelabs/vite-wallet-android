package net.vite.wallet.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.left_bottom_text_underline_layout.view.*
import net.vite.wallet.kline.R

class LeftBottomTextUnderlineLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view =
        LayoutInflater.from(context).inflate(R.layout.left_bottom_text_underline_layout, this)

    fun setLeftText(text: String) {
        leftText.text = text
    }

    fun setLeftText(@StringRes textId: Int) {
        leftText.setText(textId)
    }

    fun setBottomText(text: String, @ColorInt color: Int = Color.parseColor("#3E4A59")) {
        bottomText.visibility = View.VISIBLE
        bottomText.text = text
        bottomText.setTextColor(color)
    }

    fun setBottomText(@StringRes textId: Int, @ColorInt color: Int = Color.parseColor("#3E4A59")) {
        bottomText.visibility = View.VISIBLE
        bottomText.setText(textId)
        bottomText.setTextColor(color)
    }

}