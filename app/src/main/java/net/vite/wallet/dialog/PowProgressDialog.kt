package net.vite.wallet.dialog

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import net.vite.wallet.R


class PowProgressDialog(context: Activity) : Dialog(context, R.style.BaseDialog) {

    var onCancelPowCb: () -> Unit = {}
    var quotaTxt = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_cycle_pow_progress)
        findViewById<View>(R.id.cancelButton).setOnClickListener {
            onCancelPowCb()
            dismiss()
        }
        findViewById<TextView>(R.id.quotaCostTxt).text = context.getString(R.string.ut_cost_in_powdialog, quotaTxt)
        setCancelable(false)
    }


    fun show(quotaCostTxt: String) {
        quotaTxt = quotaCostTxt
        super.show()
    }

}