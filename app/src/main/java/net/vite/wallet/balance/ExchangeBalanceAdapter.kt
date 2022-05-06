package net.vite.wallet.balance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.AccountTokenManager
import net.vite.wallet.network.http.vitex.PinnedTokenInfo
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.widget.TokenIconWidget


class ExchangeBalanceAdapter(
    private val accountTokenProfile: AccountTokenManager,
    val glide: RequestManager
) : RecyclerView.Adapter<ExchangeBalanceViewHolder>() {

    var isShowMoneyDetail = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeBalanceViewHolder {
        return ExchangeBalanceViewHolder.create(parent, glide)
    }

    override fun getItemCount(): Int {
        return accountTokenProfile.pinnedExchangeTokenInfoList.size
    }

    override fun onBindViewHolder(holder: ExchangeBalanceViewHolder, position: Int) {
        accountTokenProfile.pinnedExchangeTokenInfoList[position].let {
            holder.bind(it, isShowMoneyDetail)
        }
    }
}


class ExchangeBalanceViewHolder(val view: View, val glide: RequestManager) :
    RecyclerView.ViewHolder(view) {
    private val tokenWidget = view.findViewById<TokenIconWidget>(R.id.tokenWidget)
    private val tokenName = view.findViewById<TextView>(R.id.tokenName)
    private val availableBalance = view.findViewById<TextView>(R.id.availableBalance)
    private val totalBalance = view.findViewById<TextView>(R.id.totalBalance)
    private val rightLine = view.findViewById<View>(R.id.rightLine)
    private val more = view.findViewById<ImageView>(R.id.more)

    companion object {
        fun create(parent: ViewGroup, glide: RequestManager): ExchangeBalanceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_balance_exchange, parent, false)
            return ExchangeBalanceViewHolder(view, glide)
        }
    }

    private fun update(pinnedAccountInfo: PinnedTokenInfo, isShowMoneyDetail: Boolean) {

        val normalTokenInfo = TokenInfoCenter.getTokenInfoInCache(pinnedAccountInfo.tokenCode)


        rightLine.setBackgroundColor(pinnedAccountInfo.mainColor())
        tokenName.text = normalTokenInfo?.uniqueName()

        if (isShowMoneyDetail) {
            val accountFundInfo = DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                ?.get(normalTokenInfo?.tokenAddress)
            availableBalance.text = accountFundInfo?.getAvailableBase()?.toLocalReadableText(8) ?: "0"
            totalBalance.text = accountFundInfo?.getOrderLockedAndAvaliableBase()?.toLocalReadableText(8) ?: "0"
        } else {
            availableBalance.text = "****"
            totalBalance.text = "****"
        }

        tokenWidget.setup(normalTokenInfo?.icon, normalTokenInfo?.isGatewayToken() == true)

        view.setOnClickListener {
            InternalTransferActivity.show(more.context, normalTokenInfo?.tokenAddress!!)
        }
    }

    fun bind(pinnedAccountInfo: PinnedTokenInfo, isShowMoneyDetail: Boolean) {
        update(pinnedAccountInfo, isShowMoneyDetail)
    }

}