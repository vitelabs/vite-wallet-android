package net.vite.wallet.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_contact.*
import net.vite.wallet.R


class ContactListFragment : Fragment() {

    companion object {
        val AllContact = 0
        val ViteContact = 1
        val EthContact = 2
        val GrinContact = 3
        fun new(type: Int): ContactListFragment {
            return ContactListFragment().apply {
                arguments = Bundle().apply {
                    putInt("ContactType", type)
                }
            }
        }
    }

    private val contacts = ArrayList<Contact>()
    private val contactAdapter = ContactListAdapter(contacts)
    private var contactType = AllContact

    private fun refresh() {
        contacts.clear()
        contacts.addAll(
            when (contactType) {
                AllContact -> ContactCenter.contacts
                ViteContact -> ContactCenter.contacts.filter { it.familyName == Contact.FAMILT_NAME_VITE }
                EthContact -> ContactCenter.contacts.filter { it.familyName == Contact.FAMILT_NAME_ETH }
                GrinContact -> ContactCenter.contacts.filter { it.familyName == Contact.FAMILT_NAME_GRIN }
                else -> emptyList()
            }
        )

        val typename = when (contactType) {
            ViteContact -> Contact.FAMILT_NAME_VITE
            EthContact -> Contact.FAMILT_NAME_ETH
            GrinContact -> Contact.FAMILT_NAME_GRIN
            else -> "null"
        }

        if (contacts.isEmpty()) {
            emptyImg.visibility = View.VISIBLE
            emptyTxt.visibility = View.VISIBLE
            mainList.visibility = View.GONE
            emptyTxt.text = getString(R.string.none_contact_platform, typename)
        } else {
            mainList.visibility = View.VISIBLE
            emptyImg.visibility = View.GONE
            emptyTxt.visibility = View.GONE
            contactAdapter.notifyDataSetChanged()
        }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contactType = arguments?.getInt("ContactType", AllContact) ?: AllContact
        mainList.adapter = contactAdapter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contact, container, false)
    }
}


