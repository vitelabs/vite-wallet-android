package net.vite.wallet.contacts

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

import kotlinx.android.synthetic.main.activity_contacts.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity

class ContactsActivity : UnchangableAccountAwareActivity() {

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)


        val adapter = ContactPageAdapter(supportFragmentManager)
        pager.adapter = adapter
        tabLayout.setupWithViewPager(pager)
        tabLayout.getTabAt(0)?.setText(R.string.all)
        tabLayout.getTabAt(1)?.text = "VITE"
        tabLayout.getTabAt(2)?.text = "ETH"
        tabLayout.getTabAt(3)?.text = "GRIN"

        var currentPosition = 0
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                currentPosition = position
            }
        })


        val addFunc = {

            val type = when (currentPosition) {
                0 -> Contact.FAMILT_NAME_VITE
                1 -> Contact.FAMILT_NAME_VITE
                2 -> Contact.FAMILT_NAME_ETH
                3 -> Contact.FAMILT_NAME_GRIN
                else -> Contact.FAMILT_NAME_VITE
            }
            ContactEditActivity.show(this, null, type)
        }


        addContact.setOnClickListener {
            addFunc()
        }
        addContactBottom.setOnClickListener {
            addFunc()
        }
    }

    override fun onStart() {
        super.onStart()
        if (ContactCenter.contacts.isEmpty()) {
            emptyGroup.visibility = View.VISIBLE
            tabLayout.visibility = View.GONE
            pager.visibility = View.GONE
        } else {
            emptyGroup.visibility = View.GONE
            tabLayout.visibility = View.VISIBLE
            pager.visibility = View.VISIBLE
        }
    }
}


class ContactPageAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
    private val contactListFragments = ArrayList<ContactListFragment>()

    init {
        contactListFragments.add(ContactListFragment.new(ContactListFragment.AllContact))
        contactListFragments.add(ContactListFragment.new(ContactListFragment.ViteContact))
        contactListFragments.add(ContactListFragment.new(ContactListFragment.EthContact))
        contactListFragments.add(ContactListFragment.new(ContactListFragment.GrinContact))
    }

    override fun getItem(position: Int): Fragment {
        return contactListFragments[position]
    }

    override fun getCount() = contactListFragments.size

}