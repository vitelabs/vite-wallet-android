package net.vite.wallet.balance.tokenselect

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.DexAccountFundInfoPoll
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.widget.TokenIconWidget
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class LocalTokenSelectAdapter(
    val activity: LocalTokenSelectActivity,
    val fromWallet: Boolean,
    val onlyShowCrossChainToken: Boolean
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var orderedList = ArrayList<NormalTokenInfo>()

    fun setDefaultTokenInfos(tokenInfos: List<NormalTokenInfo>) {
        val result = ArrayList<NormalTokenInfo>()

        orderedList.clear()
        orderedList.addAll(tokenInfos)

        AccountCenter.getCurrentAccountTokenManager()?.pinnedTokenInfoList?.forEach {
            if (it.family == TokenFamily.VITE) {
                TokenInfoCenter.getTokenInfoInCache(it.tokenCode)?.let { normalTokenInfo ->
                    if (orderedList.find { it.uniqueName() == normalTokenInfo.uniqueName() } == null) {
                        orderedList.add(normalTokenInfo)
                    }
                }
            }
        }

        result.addAll(orderedList.filter {
            if (fromWallet)
                it.balance() > BigInteger.ZERO
            else
                DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                    ?.get(it.tokenAddress)?.getAvailableBase() ?: BigDecimal.ZERO > BigDecimal.ZERO
        }.sortedBy {
            it.uniqueName()
        })

        result.addAll(orderedList.filter {
            if (fromWallet)
                it.balance() == BigInteger.ZERO
            else
                DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                    ?.get(it.tokenAddress)?.getAvailableBase() ?: BigDecimal.ZERO == BigDecimal.ZERO
        }.sortedBy {
            it.uniqueName()
        })

        setTokenInfos(result)
    }


    fun setTokenInfos(tokenInfos: List<NormalTokenInfo>) {
        orderedList.clear()
        orderedList.addAll(tokenInfos.filter { onlyShowCrossChainToken && it.isGatewayToken() || !onlyShowCrossChainToken })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_local_token_select -> LocalInfoVH.create(parent, activity, fromWallet)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun getItemCount(): Int {
        return orderedList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LocalInfoVH -> holder.bind(orderedList[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.item_local_token_select
    }
}


private class LocalInfoVH(
    val mitemView: View,
    val activity: LocalTokenSelectActivity,
    val fromWallet: Boolean
) :
    RecyclerView.ViewHolder(mitemView) {
    val tokenName = mitemView.findViewById<TextView>(R.id.tokenName)
    val tokenWidget = mitemView.findViewById<TokenIconWidget>(R.id.tokenWidget)
    val tokenFamily = mitemView.findViewById<TextView>(R.id.tokenFamily)
    val tokenBalance = mitemView.findViewById<TextView>(R.id.tokenBalance)

    companion object {
        fun create(
            parent: ViewGroup,
            activity: LocalTokenSelectActivity,
            fromWallet: Boolean
        ): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_local_token_select, parent, false)
            return LocalInfoVH(view, activity, fromWallet)
        }
    }

    fun bind(n: NormalTokenInfo) {

        n.let { info ->
            tokenName.text = info.uniqueName()
            tokenFamily.text = info.name

            if (fromWallet) {
                tokenBalance.text = info.balanceText(8)
            } else {
                val accountFundInfo = DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                    ?.get(info.tokenAddress)
                tokenBalance.text =
                    accountFundInfo?.getAvailableBase()?.toLocalReadableText(8) ?: "0"
            }

            tokenWidget.setup(info.icon)

            mitemView.setOnClickListener {
                val intent = Intent()
                intent.putExtra("tokenAddress", info.tokenAddress)
                activity.setResult(Activity.RESULT_OK, intent)
                activity.finish()
            }
        }

    }
}