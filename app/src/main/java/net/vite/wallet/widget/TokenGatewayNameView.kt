package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import kotlinx.android.synthetic.main.widget_token_gateway.view.*
import net.vite.wallet.R


class TokenGatewayNameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.widget_token_gateway, this)

    fun setTextColor(@ColorInt color: Int) {
        gatewayNameTxt.setTextColor(color)
    }

    fun setText(text: String) {
        gatewayNameTxt.text = text
    }

}