package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.widget_banlance_detail_info.view.*
import net.vite.wallet.R
import net.vite.wallet.network.http.vitex.TokenFamily

class BalanceDetailWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.widget_banlance_detail_info, this)

    fun setCoinFamily(family: Int) {
        when (family) {
            TokenFamily.ETH -> {
                rootConstraintLayout.setBackgroundResource(R.drawable.widget_balance_detail_eth_bg)
//                addressNameTxt.visibility = View.GONE
//                goViewAddrInfo.setImageResource(R.mipmap.paste_white)
//                setAddressManagerOnClickListener {
//                    (view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.let { cm ->
//                        cm.primaryClip = ClipData.newPlainText(null, addressTxt.text)
//                        Toast.makeText(
//                            view.context, view.context.getString(R.string.copy_success), Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
            }
            TokenFamily.VITE ->
                rootConstraintLayout.setBackgroundResource(R.drawable.widget_balance_detail_vite_bg)

        }
    }

    fun setAddressName(name: String) {
        addressNameTxt.text = name
    }

    fun setAddress(name: String) {
        addressTxt.text = name
    }

    fun setBalanceAmount(amount: String) {
        balanceAmount.text = amount
    }

    fun setBalanceValue(value: String) {
        balanceValue.text = "≈$value"
    }


    fun setUnreceivedAmount(amount: String, txCount: Int) {
        unreceivedAmountTitle.visibility = View.VISIBLE
        unreceivedAmount.visibility = View.VISIBLE
        if (txCount > 0) {
            unreceivedAmountTitle.text = context.getString(R.string.unreceived_quota_cost, txCount)
        } else {
            unreceivedAmountTitle.setText(R.string.title_unreceived)
        }
        unreceivedAmount.text = amount
    }

    fun setAlreadyPledgeAmount(amount: String) {
        alreadyPledgeAmount.visibility = View.VISIBLE
        titleAlreadyPledge.visibility = View.VISIBLE
        alreadyPledgeAmount.text = amount
    }

    fun setReceiveBtnOnClickListener(f: () -> Unit) {
        receiveBtn.setOnClickListener { f() }
    }

    fun setTransferBtnOnClickListener(f: () -> Unit) {
        transferBtn.setOnClickListener { f() }
    }

    fun setAddressManagerOnClickListener(f: () -> Unit) {
        addressInfoContainer.setOnClickListener { f() }
        goViewAddrInfo.setOnClickListener { f() }
    }


}