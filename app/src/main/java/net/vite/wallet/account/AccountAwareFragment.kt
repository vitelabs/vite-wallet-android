package net.vite.wallet.account

import android.os.Bundle
import androidx.fragment.app.Fragment

open class AccountAwareFragment : Fragment() {

    lateinit var currentAccount: AccountProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentAccount = AccountCenter.getCurrentAccount() ?: run {
            activity?.finish()
            return
        }
    }

    fun nowViteAddress() = currentAccount.nowViteAddress()

}