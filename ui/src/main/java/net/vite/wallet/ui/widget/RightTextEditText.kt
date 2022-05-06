package net.vite.wallet.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText

class RightTextEditText(context: Context, attrs: AttributeSet) : AppCompatEditText(context, attrs) {

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18.0F)
    }

    var customRightText: String = ""
        set(value) {
            field = value
            rightPadding = getPainter().measureText(value) + TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                6.0f,
                context.resources.displayMetrics
            )
            setPadding(paddingLeft, paddingBottom, rightPadding.toInt(), paddingTop)
            invalidate()
        }


    private var rightPadding: Float = 0F

    var colorStr = "#77808a"
        set(value) {
            field = value
            invalidate()
        }


    private fun getPainter(): Paint {
        val newPaint = Paint()
        newPaint.textSize = textSize
        newPaint.color = Color.parseColor(colorStr)
        return newPaint
    }

    private var lis: () -> Unit = {}

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onTouchEvent(event)
        }

        if (event.rawX > x + width - rightPadding) {
            return if (event.action == MotionEvent.ACTION_UP) {
                lis()
                true
            } else {
                true
            }
        }
        return super.onTouchEvent(event)

    }

    fun setOnRightTextClickListener(f: () -> Unit) {
        lis = f
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (!TextUtils.isEmpty(customRightText)) {
            canvas?.drawText(
                customRightText,
                scrollX + width - paint.measureText(customRightText),
                baseline.toFloat(),
                getPainter()
            )
        }
    }
}