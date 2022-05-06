package net.vite.wallet.dexassets

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_dex_assets.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountAwareFragment
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.dexassets.utils.DexAssetCalcUtils
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.utils.viewbinding.findMyView
import kotlin.math.abs

class DexAssetsTabFragment : AccountAwareFragment() {

    val balanceViewModel by activityViewModels<BalanceViewModel>()
    val dexAssetsDetailsViewModel by activityViewModels<DexAssetsDetailsViewModel>()

    val allAssetValueBTC by findMyView<TextView>(R.id.allAssetValueBTC)
    val allAssetValueLegend by findMyView<TextView>(R.id.allAssetValueLegend)

    val tabs by findMyView<TabLayout>(R.id.tabs)

    val dexTitle by findMyView<TextView>(R.id.dexTitle)
    val expandView by findMyView<View>(R.id.expandView)
    val collapseToolbar by findMyView<CollapsingToolbarLayout>(R.id.collapseToolbar)
    val appBar by findMyView<AppBarLayout>(R.id.appBar)

    var lastOffset: Int? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dex_assets, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tv = TypedValue()
        if (requireActivity().theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            val actionBarHeight =
                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
            collapseToolbar.minimumHeight = dexTitle.height + actionBarHeight
        }
        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (verticalOffset == 0) {
                dexTitle.visibility = View.INVISIBLE
                expandView.visibility = View.VISIBLE
            } else if (abs(verticalOffset) == appBar.totalScrollRange) {
                expandView.visibility = View.INVISIBLE
            }
            val progress = abs(verticalOffset).toFloat() / appBar.totalScrollRange
            if (progress > 0.8F) {
                dexTitle.alpha = progress
                dexTitle.visibility = View.VISIBLE
            }
            expandView.alpha = 1 - progress
        })
        viewpager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(position: Int) = DexAssetsDetailsFragment.newInstance(
                when (position) {
                    0 -> AssetsAccountType.WalletAccount
                    1 -> AssetsAccountType.DexAccount
                    else -> throw Exception()
                }
            )
        }

        TabLayoutMediator(tabs, viewpager) { _, _ -> }.attach()
        tabs.getTabAt(0)?.setText(R.string.dex_tab_fund_account_title)
        tabs.getTabAt(1)?.setText(R.string.dex_tab_dex_account_title)

        balanceViewModel.dexAccountFundInfo.observe(viewLifecycleOwner) {
            update()
        }

        balanceViewModel.rtViteAccInfo.observe(viewLifecycleOwner) {
            update()
        }
        balanceViewModel.tokenPriceLd.observe(viewLifecycleOwner) {
            update()
        }

        dexAssetsDetailsViewModel.rtPrice.observe(viewLifecycleOwner) {
            update()
        }

        dexAssetsDetailsViewModel.vipStakeForSBPLD.observe(viewLifecycleOwner) {
            update()
        }

        dexAssetsDetailsViewModel.vipStakeInfoListLD.observe(viewLifecycleOwner) {
            update()
        }

        dexAssetsDetailsViewModel.miningStakingAmountLD.observe(viewLifecycleOwner) {
            update()
        }

        dexAssetsDetailsViewModel.vipStakeForFullNodeLD.observe(viewLifecycleOwner) {
            update()
        }

        balanceViewModel.rtPledgeTotalAmount.observe(viewLifecycleOwner, {
            update()
        })

        dexAssetsDetailsViewModel.pollPrice()

    }


    fun update() {
        allAssetValueBTC.text =
            DexAssetCalcUtils.getAllAssetValueBTC().toLocalReadableText(8)
        allAssetValueLegend.text =
            DexAssetCalcUtils.getAllAssetValueLegend().toCurrencyText(needApproximate = true)
    }

    override fun onStart() {
        super.onStart()
        currentAccount.nowViteAddress()?.let { addr ->
            dexAssetsDetailsViewModel.getStakeForFullNode(addr)
            dexAssetsDetailsViewModel.getStakeForSBP(addr)
            dexAssetsDetailsViewModel.dexGetVIPStakeInfoList(addr)
            dexAssetsDetailsViewModel.dexGetCurrentMiningStakingAmountByAddress(addr)
        }
    }
}
