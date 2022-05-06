package net.vite.wallet.dexassets.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import net.vite.wallet.R

class StartTextEndTextRow @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.widget_starttext_endtext, this)
    val keyTextView = findViewById<TextView>(R.id.startText)
    val valueTextView = findViewById<TextView>(R.id.endText)

    fun setKey(@StringRes id: Int) {
        keyTextView.setText(id)
    }

    fun setKey(str: CharSequence) {
        keyTextView.text = str
    }

    fun setValue(str: CharSequence) {
        valueTextView.text = str
    }
}