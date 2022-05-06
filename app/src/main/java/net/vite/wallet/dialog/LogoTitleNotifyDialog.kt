package net.vite.wallet.dialog

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import net.vite.wallet.R

class LogoTitleNotifyDialog(activity: Activity) : Dialog(activity, R.style.BaseDialog) {

    private var topImg: ImageView
    private var messageTxt: TextView
    private var bottomBtn: TextView


    init {
        setContentView(R.layout.dialog_notify_logo_title)
        topImg = findViewById(R.id.topImg)!!
        messageTxt = findViewById(R.id.messageTxt)!!
        bottomBtn = findViewById(R.id.bottomBtn)!!
    }

    fun enableTopImage(enable: Boolean) {
        topImg.visibility = if (enable) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setMessage(str: String) {
        messageTxt.visibility = View.VISIBLE
        messageTxt.text = str
    }

    fun setBottom(str: String, onClick: () -> Unit = { dismiss() }) {
        bottomBtn.visibility = View.VISIBLE
        bottomBtn.text = str
        bottomBtn.setOnClickListener { onClick() }
    }
}