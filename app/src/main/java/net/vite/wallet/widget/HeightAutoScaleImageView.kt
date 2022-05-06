package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import net.vite.wallet.R

class HeightAutoScaleImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {
    var ratio: Float = -1.0F

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.HeightAutoScaleImageView)
        ratio = typedArray.getFloat(R.styleable.HeightAutoScaleImageView_radio, -1.0F)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = if (ratio != -1.0F) {
            (width * ratio).toInt()
        } else {
            MeasureSpec.getSize(heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }
}