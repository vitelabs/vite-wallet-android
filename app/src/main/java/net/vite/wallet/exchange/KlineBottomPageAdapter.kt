package net.vite.wallet.exchange

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter


class KlineBottomPageAdapter(
    fragmentManager: FragmentManager,
    val fragments: ArrayList<Fragment>
) : FragmentPagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getCount() = fragments.size

}