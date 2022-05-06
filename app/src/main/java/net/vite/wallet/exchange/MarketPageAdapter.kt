package net.vite.wallet.exchange

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter


class MarketPageAdapter(
    fragmentManager: FragmentManager,
    val marketFragments: ArrayList<out Fragment>
) : FragmentPagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment {
        return marketFragments[position]
    }

    override fun getCount() = marketFragments.size

}