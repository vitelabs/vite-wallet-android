package net.vite.wallet.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat

open class TextRightIconEditText(context: Context, attrs: AttributeSet) :
    AppCompatEditText(context, attrs) {
    init {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18.0F)
    }

    private val imgSize =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14.0F, resources.displayMetrics)
    private val imgMarginLeft =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10.0F, resources.displayMetrics)

    private var rightPadding: Float = 0F

    var colorStr = "#77808a"
        set(value) {
            field = value
            invalidate()
        }
    var customEndText: String = ""
        set(value) {
            field = value
            rightPadding = getEndPainter().measureText(value) + TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                6.0f,
                context.resources.displayMetrics
            )
            setPadding(paddingLeft, paddingBottom, rightPadding.toInt(), paddingTop)
            invalidate()
        }

    private fun getEndPainter(): Paint {
        val newPaint = Paint()
        newPaint.textSize = textSize
        newPaint.color = Color.parseColor(colorStr)
        return newPaint
    }


    var customRightDrawable: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    private var imgRect: Rect? = null

    private var lis: () -> Unit = {}

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onTouchEvent(event)
        }
        imgRect?.let {
            if (event.x > it.left && event.x < it.right && event.y > it.top && event.y < it.bottom) {
                return if (event.action == MotionEvent.ACTION_UP) {
                    lis()
                    true
                } else {
                    true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun setOnRightDrawableClickListener(f: () -> Unit) {
        lis = f
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) {
            return
        }
        customRightDrawable?.let {
            val editText = editableText.toString()
            val r = Rect()
            paint.getTextBounds(editText, 0, editText.length, r)
            imgRect =
                Rect(
                    r.right + imgMarginLeft.toInt(),
                    baseline - imgSize.toInt(),
                    r.right + imgMarginLeft.toInt() + imgSize.toInt(),
                    baseline
                )
            val drawable = ResourcesCompat.getDrawable(resources, it, null)
            imgRect?.let {
                drawable?.setBounds(it)
            }
            drawable?.draw(canvas)
        }

        if (!TextUtils.isEmpty(customEndText)) {
            canvas.drawText(
                customEndText,
                scrollX + width - paint.measureText(customEndText),
                baseline.toFloat(),
                getEndPainter()
            )
        }

    }
}