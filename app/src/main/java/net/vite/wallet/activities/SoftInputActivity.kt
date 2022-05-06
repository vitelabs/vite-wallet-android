package net.vite.wallet.activities

import android.animation.ObjectAnimator
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout

open class SoftInputActivity : BaseActivity() {


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val c = findViewById<FrameLayout>(android.R.id.content)
        val rootView = c.getChildAt(0)
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                var r = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val screenHeight = rootView.rootView.height

                val keypadHeight = screenHeight - r.bottom
//                logt(
//                    "keypadHeight$keypadHeight r.bottom${r.bottom} sc${screenHeight}" +
//                            " rootView${rootView.height} rootViewB${rootView.bottom} rootViewBT${rootView.translationY} "
//                )
                if (keypadHeight > screenHeight * 0.15) {
                    onSoftInputShow(rootView, keypadHeight)
//                    logt(
//                        "keypadHeighthahah $keypadHeight r.bottom${r.bottom} sc${screenHeight}" +
//                                " rootView${rootView.height} rootViewB${rootView.bottom} rootViewBT${rootView.translationY} "
//                    )
                } else {
                    onSoftInputDismiss(rootView, keypadHeight)
                }
            }
        })
    }


    open fun onSoftInputShow(rootView: View, keypadHeight: Int) {
        val o = ObjectAnimator.ofFloat(rootView, "translationY", -keypadHeight.toFloat())
        o.duration = 300
        o.start()

    }

    open fun onSoftInputDismiss(rootView: View, keypadHeight: Int) {
        val o = ObjectAnimator.ofFloat(rootView, "translationY", 0.toFloat())
        o.duration = 300
        o.start()
    }


}