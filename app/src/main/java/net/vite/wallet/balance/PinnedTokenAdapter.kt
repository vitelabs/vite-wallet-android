package net.vite.wallet.balance

import android.app.Activity
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.AccountTokenManager
import net.vite.wallet.balance.conversion.EthViteTokenBalanceDetailActivity
import net.vite.wallet.network.http.vitex.PinnedTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.utils.showToast
import net.vite.wallet.widget.TokenIconWidget


class PinnedTokenAdapter(
    val accountTokenProfile: AccountTokenManager,
    val glide: RequestManager
) : RecyclerView.Adapter<BalanceViewHolder>() {

    var isShowMoneyDetail = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
        return BalanceViewHolder.create(parent, glide)
    }

    override fun getItemCount(): Int {
        return accountTokenProfile.pinnedTokenInfoList.size
    }

    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        accountTokenProfile.pinnedTokenInfoList[position].let {
            holder.bind(it, isShowMoneyDetail)
        }
    }
}


class BalanceViewHolder(val view: View, val glide: RequestManager) : RecyclerView.ViewHolder(view) {
    private val tokenWidget = view.findViewById<TokenIconWidget>(R.id.tokenWidget)
    private val tokenName = view.findViewById<TextView>(R.id.tokenName)
    private val tokenFamily = view.findViewById<TextView>(R.id.tokenFamily)
    private val balanceAmount = view.findViewById<TextView>(R.id.balanceAmount)
    private val tokenValue = view.findViewById<TextView>(R.id.tokenValue)
    private val rightLine = view.findViewById<View>(R.id.rightLine)
    private val colorFilter = PorterDuffColorFilter(0x0f000000, PorterDuff.Mode.DARKEN)

    companion object {
        fun create(parent: ViewGroup, glide: RequestManager): BalanceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_balance_wallet, parent, false)
            return BalanceViewHolder(view, glide)
        }
    }

    private fun update(pinnedAccountInfo: PinnedTokenInfo, isShowMoneyDetail: Boolean) {

        val normalTokenInfo = TokenInfoCenter.getTokenInfoInCache(pinnedAccountInfo.tokenCode)

        view.setOnClickListener {
            when (pinnedAccountInfo.family) {
                TokenFamily.ETH -> when {
                    pinnedAccountInfo.tokenCode == TokenInfoCenter.EthViteTokenCodes.tokenCode -> EthViteTokenBalanceDetailActivity.show(
                        view.context
                    )
                    else -> EthTokenBalanceDetailActivity.show(
                        view.context,
                        pinnedAccountInfo.tokenCode
                    )
                }
                TokenFamily.VITE -> ViteTokenBalanceDetailActivity.show(
                    view.context,
                    pinnedAccountInfo.tokenCode
                )
                TokenFamily.GRIN -> {
                    view.context.showToast(R.string.grin_is_removed)
                }
            }
        }

        if (pinnedAccountInfo.family == TokenFamily.GRIN) {
            view.background.colorFilter = null
            tokenValue.visibility = View.VISIBLE
            balanceAmount.visibility = View.VISIBLE
        } else {
            view.background.colorFilter = null
            tokenValue.visibility = View.VISIBLE
            balanceAmount.visibility = View.VISIBLE
        }
        tokenFamily.text = pinnedAccountInfo.familyName()

        rightLine.setBackgroundColor(pinnedAccountInfo.mainColor())
        tokenName.text = normalTokenInfo?.uniqueName()

        if (isShowMoneyDetail) {
            balanceAmount.text = normalTokenInfo?.balanceText(4) ?: "0"
            tokenValue.text = normalTokenInfo?.balanceValue().toCurrencyText(2, true)
        } else {
            balanceAmount.text = "****"
            tokenValue.text = "****"
        }
        tokenWidget.setup(normalTokenInfo?.icon, normalTokenInfo?.isGatewayToken() == true)
    }

    fun bind(pinnedAccountInfo: PinnedTokenInfo, isShowMoneyDetail: Boolean) {
        update(pinnedAccountInfo, isShowMoneyDetail)
    }

}