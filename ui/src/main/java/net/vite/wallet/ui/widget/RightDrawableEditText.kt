package net.vite.wallet.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText


class RightDrawableEditText(context: Context, attrs: AttributeSet) :
    AppCompatEditText(context, attrs) {
    val DRAWABLE_LEFT = 0
    val DRAWABLE_TOP = 1
    val DRAWABLE_RIGHT = 2
    val DRAWABLE_BOTTOM = 3
    val fuzz = 6

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18.0F)
    }

    var onClickRightDrawableListener: (() -> Unit)? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        compoundDrawables[DRAWABLE_RIGHT]?.bounds?.let { bounds ->
            if (event.rawX >= right - bounds.width()) {
                return if (event.action == MotionEvent.ACTION_UP) {
                    onClickRightDrawableListener?.invoke()
                    true
                } else {
                    true
                }
            }
        }


        return super.onTouchEvent(event)
    }
}