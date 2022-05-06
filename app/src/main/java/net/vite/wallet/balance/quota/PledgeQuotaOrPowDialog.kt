import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.TextView
import net.vite.wallet.R
import net.vite.wallet.balance.quota.PowCount

class PledgeQuotaOrPowDialog(val activity: Activity) :
    Dialog(activity, R.style.NormalAlertDialog) {
    val closeBtn: View
    val confirmBtn: View
    val cancelBtn: View
    val secondTitleTxt: TextView
    val titleTxt: TextView

    init {
        setContentView(R.layout.dialog_pledge_quota_or_pow)
        closeBtn = findViewById(R.id.closeBtn)
        secondTitleTxt = findViewById(R.id.secondTitleTxt)
        titleTxt = findViewById(R.id.titleTxt)
        confirmBtn = findViewById(R.id.firstButton)
        cancelBtn = findViewById(R.id.cancelButton)
        closeBtn.setOnClickListener { this.dismiss() }
        setCancelable(false)
    }

    fun show(canPow: Boolean, pledgeQuota: () -> Unit, runPow: () -> Unit) {
        if (!canPow) {
            cancelBtn.visibility = View.GONE
        }
        confirmBtn.setOnClickListener {
            dismiss()
            pledgeQuota()
        }
        cancelBtn.setOnClickListener {
            dismiss()
            runPow()
        }
        show()
    }

    fun showAsPowExceedLimit(pledgeQuota: () -> Unit) {
        titleTxt.setText(R.string.quota_not_enough)
        secondTitleTxt.text = activity.getString(
            R.string.quota_exceed_limit_message,
            PowCount.powCount
        )
        cancelBtn.visibility = View.GONE
        confirmBtn.setOnClickListener {
            dismiss()
            pledgeQuota()
        }
        show()
    }

}
