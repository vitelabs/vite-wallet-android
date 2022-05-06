package net.vite.wallet.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import net.vite.wallet.R
import net.vite.wallet.utils.dp2px


class TokenFamilyTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {
    companion object {
        const val VITE_STYLE = 1
        const val ETH_STYLE = 2
        const val VITE_GATEWAY_STYLE = 3
        const val VITE_GATEWAY_NAME_STYLE = 4
    }


    var style = VITE_STYLE
        set(value) {
            setPadding(4.0F.dp2px().toInt(), 2.0F.dp2px().toInt(), 4.0F.dp2px().toInt(), 2.0F.dp2px().toInt())
            when (value) {
                VITE_STYLE -> {
                    setTextColor(Color.parseColor("#007aff"))
                    setBackgroundResource(R.drawable.vite_family_rect)
                }
                ETH_STYLE -> {
                    setTextColor(Color.parseColor("#5bc500"))
                    setBackgroundResource(R.drawable.eth_family_rect)
                }
                VITE_GATEWAY_STYLE -> {
                    setTextColor(Color.parseColor("#007aff"))
                    setBackgroundResource(R.drawable.vite_gw_rect_family_outline)
                }
                VITE_GATEWAY_NAME_STYLE -> {
                    setTextColor(Color.parseColor("#007aff"))
                    setBackgroundResource(R.drawable.vite_gw_rect_gw_outline)
                }
            }

            field = value
            invalidate()
        }


}