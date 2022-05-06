package net.vite.wallet.dialog

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.TextView
import net.vite.wallet.R

class TextTitleNotifyDialog(activity: Activity) : Dialog(activity, R.style.BaseDialog) {

    private val titleTxt: TextView
    private val messageTxt: TextView
    private val bottomBtn: TextView
    private val bottomBtnLeft: TextView
    private val closeIcon: View


    var canCancel = true
        set(value) {
            field = value
            if (canCancel) {
                closeIcon.visibility = View.GONE
            } else {
                closeIcon.visibility = View.VISIBLE
            }
            super.setCancelable(value)
        }

    override fun setCancelable(flag: Boolean) {
        canCancel = flag
    }

    init {
        setContentView(R.layout.dialog_notify_text_title)
        titleTxt = findViewById(R.id.title)!!
        messageTxt = findViewById(R.id.messageTxt)!!
        bottomBtn = findViewById(R.id.bottomBtn)!!
        bottomBtnLeft = findViewById(R.id.bottomBtnLeft)!!
        closeIcon = findViewById(R.id.closeBtn)!!
        closeIcon.setOnClickListener {
            dismiss()
        }
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

    fun setBottomLeft(str: String, onClick: (dialog: Dialog) -> Unit = { it.dismiss() }) {
        bottomBtnLeft.visibility = View.VISIBLE
        bottomBtnLeft.text = str
        bottomBtnLeft.setOnClickListener { onClick(this) }
    }

    fun setBottomLeft(str: Int, onClick: (dialog: Dialog) -> Unit = { it.dismiss() }) {
        bottomBtnLeft.visibility = View.VISIBLE
        bottomBtnLeft.setText(str)
        bottomBtnLeft.setOnClickListener { onClick(this) }
    }
}