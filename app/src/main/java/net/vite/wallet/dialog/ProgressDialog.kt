package net.vite.wallet.dialog

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import net.vite.wallet.R


class ProgressDialog(context: Activity) : Dialog(context, R.style.TransparentDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_cycle_progress)
        setCancelable(false)
    }
}