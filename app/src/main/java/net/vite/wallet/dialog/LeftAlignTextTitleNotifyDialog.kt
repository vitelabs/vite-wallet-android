package net.vite.wallet.dialog

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.TextView
import net.vite.wallet.R

class LeftAlignTextTitleNotifyDialog(activity: Activity) : Dialog(activity, R.style.BaseDialog) {

    private var titleTxt: TextView
    private var messageTxt: TextView
    private var bottomBtn: TextView


    init {
        setContentView(R.layout.dialog_notify_text_title)
        titleTxt = findViewById(R.id.title)!!
        messageTxt = findViewById(R.id.messageTxt)!!
        bottomBtn = findViewById(R.id.bottomBtn)!!
    }


    fun setTitle(str: String) {
        titleTxt.visibility = View.VISIBLE
        titleTxt.text = str
    }

    override fun setTitle(str: Int) {
        titleTxt.visibility = View.VISIBLE
        titleTxt.setText(str)
    }

    fun setMessage(str: Int) {
        messageTxt.visibility = View.VISIBLE
        messageTxt.setText(str)
    }

    fun setMessage(str: String) {
        messageTxt.visibility = View.VISIBLE
        messageTxt.text = str
    }

    fun setBottom(str: String, onClick: (dialog: Dialog) -> Unit = { it.dismiss() }) {
        bottomBtn.visibility = View.VISIBLE
        bottomBtn.text = str
        bottomBtn.setOnClickListener { onClick(this) }
    }

    fun setBottom(str: Int, onClick: (dialog: Dialog) -> Unit = { it.dismiss() }) {
        bottomBtn.visibility = View.VISIBLE
        bottomBtn.setText(str)
        bottomBtn.setOnClickListener { onClick(this) }
    }
}