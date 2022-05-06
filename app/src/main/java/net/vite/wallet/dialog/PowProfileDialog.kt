package net.vite.wallet.dialog

import android.app.Activity
import android.app.Dialog
import kotlinx.android.synthetic.main.dialog_text_title_with_logo.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.quota.PledgeQuotaActivity

class PowProfileDialog(val activity: Activity) : Dialog(activity, R.style.BaseDialog) {
    private var dismisslistener: () -> Unit = {}

    init {
        setContentView(R.layout.dialog_text_title_with_logo)
        setCancelable(false)
        bottomBtn.setOnClickListener {
            PledgeQuotaActivity.show(activity, AccountCenter.currentViteAddress())
        }
        closeBtn.setOnClickListener {
            this.dismiss()
            dismisslistener.invoke()
        }
    }

    fun setPowProfile(timeCost: String, utAcquired: String) {
        messageTxt.text = activity.getString(R.string.pow_cost_time, timeCost, utAcquired)
    }

    fun setDismissListener(dismisslistener: () -> Unit) {
        this.dismisslistener = dismisslistener
    }

}