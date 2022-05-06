package net.vite.wallet.dexassets

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_token_balance_detail_vite.*
import net.vite.wallet.MainActivity
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.AccountAwareFragment
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.balance.InternalTransferActivity
import net.vite.wallet.balance.crosschain.CrosschainNoticeActivity
import net.vite.wallet.balance.tokenselect.LocalTokenSelectActivity
import net.vite.wallet.dexassets.utils.BooleanPreferenceProperty
import net.vite.wallet.dexassets.utils.DexAssetCalcUtils
import net.vite.wallet.dexassets.utils.IntPreferenceProperty
import net.vite.wallet.me.ViteAddressManagementActivity
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.utils.viewbinding.findMyView
import net.vite.wallet.widget.TokenIconWidget

@Parcelize
enum class AssetsAccountType : Parcelable {
    WalletAccount,
    DexAccount
}

class DexAssetsDetailsFragment : AccountAwareFragment() {
    val balanceViewModel by activityViewModels<BalanceViewModel>()
    val dexAssetsDetailsViewModel by activityViewModels<DexAssetsDetailsViewModel>()
    val tokenList by findMyView<RecyclerView>(R.id.tokenList)
    val cycleProgressBar by findMyView<View>(R.id.cycleProgressBar)

    companion object {
        private const val sortAmount = 1
        private const val sortAZ = 2
        private const val sortZA = 3

        private const val TRANSFER = 13541
        private const val DEPOSIT = 13521
        private const val WITHDRAW = 13531

        private const val REQUEST_EXCHANGE_SYMBOL = 231

        fun newInstance(type: AssetsAccountType) = DexAssetsDetailsFragment().apply {
            arguments = Bundle().apply {
                putParcelable("AssetsAccountType", type)
            }
        }
    }


    private var showSmall by BooleanPreferenceProperty("DexAssetsDetailsFragmentShowSmall")
    private var sortOrder by IntPreferenceProperty("DexAssetsDetailsFragmentOrder", sortAmount)

    val type by lazy {
        arguments?.getParcelable<AssetsAccountType>("AssetsAccountType")
            ?: throw Exception("non account type")
    }

    val adapter by lazy { DexAssetsAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.dex_assets_details_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tokenList.adapter = adapter
        dexAssetsDetailsViewModel.rtDexTokens.observe(viewLifecycleOwner) {
            processTokens()
        }

        balanceViewModel.rtViteAccInfo.observe(viewLifecycleOwner) {
            processTokens()
        }
    }

    private fun NormalTokenInfo.sortKey() = when (this.uniqueName()) {
        "VITE" -> "@1"
        "VX" -> "@2"
        "BTC-000" -> "@3"
        "ETH-000" -> "@4"
        "USDT-000" -> "@5"
        else -> this.uniqueName()
    }


    fun processTokens() {
        val tokens = TokenInfoCenter.getDexTokens()
        val p1Tokens = if (!showSmall) {
            tokens.filter {
                val valueBtc = when (type) {
                    AssetsAccountType.DexAccount -> it.dexAllBalanceValueBTC()
                    AssetsAccountType.WalletAccount -> it.balanceValueBTC()
                }
                valueBtc > "0.00005".toBigDecimal()
            }
        } else {
            tokens
        }

        val p2Tokens = p1Tokens.sortedWith { o1, o2 ->
            when (sortOrder) {
                sortAmount -> {
                    val o1valueBtc = when (type) {
                        AssetsAccountType.DexAccount -> o1.dexAllBalanceValueBTC()
                        AssetsAccountType.WalletAccount -> o1.balanceValueBTC()
                    }
                    val o2valueBtc = when (type) {
                        AssetsAccountType.DexAccount -> o2.dexAllBalanceValueBTC()
                        AssetsAccountType.WalletAccount -> o2.balanceValueBTC()
                    }
                    val c = o2valueBtc.compareTo(o1valueBtc)
                    if (c == 0) {
                        o1.sortKey().compareTo(o2.sortKey())
                    } else {
                        c
                    }
                }
                sortZA -> {
                    (o2.symbol ?: "").compareTo(o1.symbol ?: "")
                }
                sortAZ -> {
                    (o1.symbol ?: "").compareTo(o2.symbol ?: "")
                }
                else -> 1
            }

        }

        adapter.dexTokens = p2Tokens
        adapter.notifyDataSetChanged()
    }

    private var nowAddr: String = ""
    override fun onStart() {
        super.onStart()
        val addr = currentAccount.nowViteAddress() ?: kotlin.run {
            activity?.finish()
            return
        }
        if (nowAddr != addr) {
            nowAddr = addr
            adapter.notifyDataSetChanged()
        }
    }

    inner class DexAssetsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var dexTokens: List<NormalTokenInfo> = listOf()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                R.layout.item_dex_assets_overview -> {
                    DexAssetsOverviewItem(
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_dex_assets_overview, parent, false)
                    )
                }
                else -> {
                    DexAssetsItem(
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_dex_assets, parent, false),
                        Glide.with(this@DexAssetsDetailsFragment)
                    )
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is DexAssetsItem -> holder.bind(dexTokens[position - 1].tokenCode!!)
                is DexAssetsOverviewItem -> holder.update(type)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (position) {
                0 -> {
                    R.layout.item_dex_assets_overview
                }
                else -> {
                    R.layout.item_dex_assets
                }
            }
        }

        override fun getItemCount(): Int {
            return dexTokens.size + 1
        }
    }

    inner class DexAssetsOverviewItem(val view: View) : RecyclerView.ViewHolder(view) {
        val tabValueBtc by findMyView<TextView>(R.id.valueBtc)
        val tabValueLegend by findMyView<TextView>(R.id.valueLegend)
        val button0 by findMyView<TextView>(R.id.button0)
        val button1 by findMyView<TextView>(R.id.button1)
        val button2 by findMyView<TextView>(R.id.button2)
        val address by findMyView<TextView>(R.id.address)
        val isShowMicroBalance by findMyView<Button>(R.id.isShowMicroBalance)
        val isShowMicroBalanceTxt by findMyView<View>(R.id.isShowMicroBalanceTxt)
        val addressContainer by findMyView<View>(R.id.addressContainer)
        val sort by findMyView<ImageView>(R.id.sort)

        fun updateWallet() {
            button0.visibility = View.VISIBLE
            button1.visibility = View.VISIBLE
            button2.visibility = View.VISIBLE
            tabValueBtc.text = DexAssetCalcUtils.walletValueBTC().toLocalReadableText(8)
            tabValueLegend.text = "≈${DexAssetCalcUtils.walletValueLegend().toCurrencyText()}"
        }

        fun updateDex() {
            button1.visibility = View.GONE
            button2.visibility = View.GONE
            tabValueBtc.text = DexAssetCalcUtils.dexValueBTC().toLocalReadableText(8)
            tabValueLegend.text = "≈${DexAssetCalcUtils.dexValueLegend().toCurrencyText()}"
        }

        fun update(type: AssetsAccountType) {
            button0.setText(R.string.dex_assets_button0)
            button0.setOnClickListener {
                startActivityForResult(
                    Intent(
                        requireActivity(),
                        LocalTokenSelectActivity::class.java
                    ).apply {
                        putExtra(
                            LocalTokenSelectActivity.FROM_WALLET,
                            type == AssetsAccountType.WalletAccount
                        )
                    },
                    TRANSFER
                )
            }
            button1.setText(R.string.dex_assets_button1)
            button1.setOnClickListener {
                startActivityForResult(
                    Intent(
                        requireActivity(),
                        LocalTokenSelectActivity::class.java
                    ).apply {
                        putExtra(
                            LocalTokenSelectActivity.FROM_WALLET,
                            type == AssetsAccountType.WalletAccount
                        )
                        putExtra(LocalTokenSelectActivity.OnlyShowCrossChainToken, true)
                    },
                    DEPOSIT
                )
            }
            button2.setText(R.string.dex_assets_button2)
            button2.setOnClickListener {
                startActivityForResult(
                    Intent(
                        requireActivity(),
                        LocalTokenSelectActivity::class.java
                    ).apply {
                        putExtra(
                            LocalTokenSelectActivity.FROM_WALLET,
                            type == AssetsAccountType.WalletAccount
                        )
                        putExtra(LocalTokenSelectActivity.OnlyShowCrossChainToken, true)
                    },
                    WITHDRAW
                )
            }
            address.text = AccountCenter.currentViteAddress() ?: ""
            addressContainer.setOnClickListener {
                startActivity(Intent(requireContext(), ViteAddressManagementActivity::class.java))
            }
            if (showSmall) {
                isShowMicroBalance.setBackgroundResource(R.mipmap.dex_assets_checkbox_unchecked)
            } else {
                isShowMicroBalance.setBackgroundResource(R.mipmap.dex_assets_checkbox_checked)
            }
            isShowMicroBalance.setOnClickListener {
                showSmall = !showSmall
                processTokens()
            }
            isShowMicroBalanceTxt.setOnClickListener {
                isShowMicroBalance.performClick()
            }
            sort.setImageResource(
                when (sortOrder) {
                    sortAmount -> R.mipmap.dex_sort_default
                    sortAZ -> R.mipmap.dex_sort_az
                    sortZA -> R.mipmap.dex_sort_za
                    else -> R.mipmap.dex_sort_default
                }
            )
            sort.setOnClickListener {
                sortOrder = when (sortOrder) {
                    sortAmount -> sortAZ
                    sortAZ -> sortZA
                    sortZA -> sortAmount
                    else -> sortAmount
                }
                processTokens()
            }
            when (type) {
                AssetsAccountType.WalletAccount -> updateWallet()
                AssetsAccountType.DexAccount -> updateDex()
            }
        }
    }


    inner class DexAssetsItem(val view: View, val glide: RequestManager) :
        RecyclerView.ViewHolder(view) {
        private val tokenWidget = view.findViewById<TokenIconWidget>(R.id.tokenWidget)
        private val tokenName = view.findViewById<TextView>(R.id.tokenName)
        private val balanceAmount = view.findViewById<TextView>(R.id.balanceAmount)
        private val tokenValue = view.findViewById<TextView>(R.id.tokenValue)


        @SuppressLint("SetTextI18n")
        fun bind(tokenCode: String) {
            val normalTokenInfo = TokenInfoCenter.getTokenInfoInCache(tokenCode) ?: kotlin.run {
                tokenName.text = "loading..."
                return
            }
            tokenName.text = normalTokenInfo.uniqueName()
            view.setOnClickListener {
                ViteDexTokenDetailActivity.actionShow(
                    this@DexAssetsDetailsFragment,
                    normalTokenInfo.tokenCode!!,
                    type == AssetsAccountType.DexAccount, REQUEST_EXCHANGE_SYMBOL
                )
            }

            if (type == AssetsAccountType.WalletAccount) {
                balanceAmount.text = normalTokenInfo.balanceText(4)
                tokenValue.text = "≈${
                    normalTokenInfo.balanceValue().toCurrencyText()
                }"
            } else {
                balanceAmount.text = normalTokenInfo.dexAllBalance().toLocalReadableText(4)
                tokenValue.text = "≈${
                    normalTokenInfo.dexAllBalanceValue().toCurrencyText()
                }"
            }


            tokenWidget.setup(normalTokenInfo.icon, false)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        if (requestCode == REQUEST_EXCHANGE_SYMBOL) {
            data?.getStringExtra("symbol")?.let {
                (requireActivity() as MainActivity).onExchangeMarketItemSwitchedFromDex(it)
            }
            return
        }
        val tokenAddress = data?.getStringExtra("tokenAddress") ?: return
        val tokenCode =
            TokenInfoCenter.getTokenInfoIncacheByTokenAddr(tokenAddress)?.tokenCode ?: return
        if (requestCode == TRANSFER) {
            InternalTransferActivity.show(
                requireContext(),
                tokenAddress,
                type == AssetsAccountType.WalletAccount
            )
        } else if (requestCode == WITHDRAW) {
            CrosschainNoticeActivity.show(
                requireActivity(),
                tokenCode,
                CrosschainNoticeActivity.REQUEST_WITHDRAW
            )
        } else if (requestCode == DEPOSIT) {
            CrosschainNoticeActivity.show(
                requireActivity(),
                tokenCode,
                CrosschainNoticeActivity.REQUEST_DEPOSIT
            )
        }
    }

}


