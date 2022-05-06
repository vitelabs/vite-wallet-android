package net.vite.wallet.ui.widget

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class BackButton : AppCompatImageView {
    @JvmOverloads
    constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : super(context, attrs, defStyleAttr) {
        if (context is Activity) {
            setOnClickListener {
                context.onBackPressed()
            }
        }
    }
}