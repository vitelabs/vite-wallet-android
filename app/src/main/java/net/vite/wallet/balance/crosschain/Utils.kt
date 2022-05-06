package net.vite.wallet.balance.crosschain

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import net.vite.wallet.R
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.utils.dp2px

@SuppressLint("UseCompatLoadingForDrawables")
fun HorizontalScrollView.addMappedToken(
    tokenInfo: NormalTokenInfo,
    onTokenClickListener: (NormalTokenInfo) -> Unit
) {
    val l = children.firstOrNull() as? LinearLayout ?: (LinearLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }.also { addView(it) })

    val t = TextView(context)
    t.layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        marginEnd = 8F.dp2px().toInt()
    }
    t.setPadding(18F.dp2px().toInt(), 5F.dp2px().toInt(), 18F.dp2px().toInt(), 5F.dp2px().toInt())
    t.textSize = 12F
    t.tag = tokenInfo.tokenAddress
    t.setTypeface(null, Typeface.BOLD)
    t.setTextColor(Color.parseColor("#5E6875"))
    t.background = context.getDrawable(R.drawable.choose_main_net_rect_unselect)
    t.setOnClickListener {
        onTokenClickListener.invoke(tokenInfo)
    }
    t.text = tokenInfo.standard()
    l.addView(t)
}

@SuppressLint("UseCompatLoadingForDrawables")
fun HorizontalScrollView.setMappedTokenSelected(tokenInfo: NormalTokenInfo) {
    (children.firstOrNull() as? LinearLayout)?.children?.map { it as? TextView }?.filterNotNull()
        ?.forEach { textView ->
            if (textView.tag == tokenInfo.tokenAddress) {
                textView.setTypeface(null, Typeface.BOLD)
                textView.setTextColor(Color.parseColor("#007aff"))
                textView.background = context.getDrawable(R.drawable.choose_main_net_rect)
            } else {
                textView.setTypeface(null, Typeface.BOLD)
                textView.setTextColor(Color.parseColor("#5E6875"))
                textView.background = context.getDrawable(R.drawable.choose_main_net_rect_unselect)
            }
        }
}