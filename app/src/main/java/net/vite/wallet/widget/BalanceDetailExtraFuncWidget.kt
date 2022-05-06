package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.widget_banlance_detail_extra_function.view.*
import net.vite.wallet.R

class BalanceDetailExtraFuncWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.widget_banlance_detail_extra_function, this)

    fun setupSingleBtn(icon: Int, text: Int, f: () -> Unit) {
        with(singleBtn) {
            visibility = View.VISIBLE
            setImg(icon)
            setText(text)
            setOnClickListener {
                f()
            }
        }
        leftWidget.visibility = View.GONE
        rightWidget.visibility = View.GONE

    }


    fun setupLeft(icon: Int, text: Int, f: () -> Unit) {
        with(leftWidget) {
            visibility = View.VISIBLE
            setLeftImg(icon)
            setRightText(text)
            setOnClickListener {
                f()
            }
        }
    }

    fun setupRight(icon: Int, text: Int, f: () -> Unit) {
        with(rightWidget) {
            visibility = View.VISIBLE
            setLeftImg(icon)
            setRightText(text)
            setOnClickListener {
                f()
            }
        }
    }

}