package net.vite.wallet.dexassets.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.vite.wallet.R

class DexDetailGroupButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.widget_dex_detail_group_button, this)
    val textView = findViewById<TextView>(R.id.text)
    val icon = findViewById<ImageView>(R.id.icon)


    fun setText(@StringRes id: Int) {
        textView.setText(id)
    }

    fun setImage(@DrawableRes id: Int) {
        icon.setImageResource(id)
    }

}