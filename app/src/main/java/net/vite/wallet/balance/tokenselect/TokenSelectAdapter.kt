package net.vite.wallet.balance.tokenselect

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.AccountTokenManager
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.widget.TokenGatewayNameView
import net.vite.wallet.widget.TokenIconWidget
import java.util.*

private data class NormalTokenVO(
    val titleText: Int?,
    val normalTokenInfo: NormalTokenInfo?
)

class TokenSelectAdapter(val glide: RequestManager, val accountTokenManager: AccountTokenManager) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var orderedList = ArrayList<NormalTokenVO>()
    var fromExchange = false

    fun setTokenInfos(tokenInfos: List<NormalTokenInfo>) {
        val viteList = ArrayList<NormalTokenVO>()
        val ethList = ArrayList<NormalTokenVO>()
        val grinList = ArrayList<NormalTokenVO>()
        var foundEth = false
        var foundVite = false
        var foundGrin = false
        orderedList.clear()
        tokenInfos.forEach {
            if (it.family() == TokenFamily.VITE) {
                if (!foundVite) {
                    foundVite = true
                    viteList.add(NormalTokenVO(R.string.vite_net_title, null))
                }
                viteList.add(NormalTokenVO(null, it))
            }
            if (it.family() == TokenFamily.ETH) {
                if (!foundEth) {
                    foundEth = true
                    ethList.add(NormalTokenVO(R.string.eth_net_title, null))
                }
                ethList.add(NormalTokenVO(null, it))
            }
            if (it.family() == TokenFamily.GRIN) {
                if (!foundGrin) {
                    foundGrin = true
                    ethList.add(NormalTokenVO(R.string.grin_net_title, null))
                }
                grinList.add(NormalTokenVO(null, it))
            }
        }
        orderedList.addAll(viteList)
        orderedList.addAll(ethList)
        orderedList.addAll(grinList)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_token_title -> TitleVH.create(parent)
            R.layout.item_token_select -> InfoVH.create(
                parent,
                glide,
                accountTokenManager,
                fromExchange
            )
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun getItemCount(): Int {
        return orderedList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is InfoVH -> holder.bind(orderedList[position])
            is TitleVH -> holder.bind(orderedList[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (orderedList[position].titleText != null) {
            R.layout.item_token_title
        } else {
            R.layout.item_token_select
        }
    }
}


private class InfoVH(
    val mitemView: View,
    val glide: RequestManager,
    val accountTokenManager: AccountTokenManager,
    val fromExchange: Boolean
) :
    RecyclerView.ViewHolder(mitemView) {
    val tokenName = mitemView.findViewById<TextView>(R.id.tokenName)
    val gatewayName = mitemView.findViewById<TokenGatewayNameView>(R.id.gatewayName)
    val tokenWidget = mitemView.findViewById<TokenIconWidget>(R.id.tokenWidget)
    val tokenFamily = mitemView.findViewById<TextView>(R.id.tokenFamily)
    val tokenAddr = mitemView.findViewById<TextView>(R.id.tokenAddr)
    val pinnedSwitcher = mitemView.findViewById<SwitchCompat>(R.id.pinnedSwitcher)

    companion object {
        fun create(
            parent: ViewGroup,
            glide: RequestManager,
            accountTokenManager: AccountTokenManager,
            fromExchange: Boolean
        ): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_token_select, parent, false)
            return InfoVH(view, glide, accountTokenManager, fromExchange)
        }
    }

    fun bind(n: NormalTokenVO) {

        n.normalTokenInfo?.let { info ->
            tokenName.text = info.uniqueName()
            tokenFamily.text = info.familyName()

            if (info.tokenCode != TokenInfoCenter.EthEthTokenCodes.tokenCode) {
                tokenAddr.visibility = View.VISIBLE
                tokenAddr.text = info.tokenAddress ?: ""
            } else {
                tokenAddr.visibility = View.GONE
            }

            if (fromExchange) {
                pinnedSwitcher.isChecked =
                    accountTokenManager.pinnedExchangeTokenInfoList.find { it.tokenCode == info.tokenCode } != null
            } else {
                pinnedSwitcher.isChecked =
                    accountTokenManager.pinnedTokenInfoList.find { it.tokenCode == info.tokenCode } != null
            }

            val isInPermanent = TokenInfoCenter.isInPermanentTokenCode(info.tokenCode)
            mitemView.isEnabled = !isInPermanent
            pinnedSwitcher.visibility = if (isInPermanent) {
                View.GONE
            } else {
                View.VISIBLE
            }
            gatewayName.visibility = View.GONE
            info.gatewayInfo?.let { gatewayInfo ->
                gatewayName.visibility = View.VISIBLE
                gatewayName.setText(gatewayInfo.name ?: "")
                gatewayName.setTextColor(Color.parseColor("#993E4A59"))
            }

            mitemView.setOnClickListener {
                if (!pinnedSwitcher.isChecked) {
                    info.tokenCode?.let {
                        accountTokenManager.pinToken(info)
                        TokenInfoCenter.addUserSearchToken(info)
                        if (info.family() == TokenFamily.VITE) {
                            accountTokenManager.pinExchangeToken(info)
                        }
                    }
                } else {
                    info.tokenCode?.let {
                        accountTokenManager.unPinToken(it)
                        TokenInfoCenter.rmUserSearchToken(it)
                        if (info.family() == TokenFamily.VITE) {
                            accountTokenManager.unPinExchangeToken(it)
                        }
                    }
                }
                pinnedSwitcher.isChecked = !pinnedSwitcher.isChecked

            }

            tokenWidget.setup(info.icon)

        }

    }
}

private class TitleVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val titleText = itemView.findViewById<TextView>(R.id.titleText)

    companion object {
        fun create(parent: ViewGroup): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_token_title, parent, false)
            return TitleVH(view)
        }
    }

    fun bind(n: NormalTokenVO) {
        n.titleText?.let {
            titleText.setText(it)
        }
    }
}