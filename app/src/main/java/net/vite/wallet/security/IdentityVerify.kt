package net.vite.wallet.security

import androidx.fragment.app.FragmentActivity
import net.vite.wallet.R
import net.vite.wallet.Wallet
import net.vite.wallet.account.AccountAwareFragment
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.utils.canUseFingerprint
import net.vite.wallet.utils.showToast

class IdentityVerify {

    val activity: FragmentActivity
    val acountAwareFunc: () -> String

    constructor(accountAwareActivity: UnchangableAccountAwareActivity) {
        this.activity = accountAwareActivity
        acountAwareFunc = {
            accountAwareActivity.currentAccount.entropyStore
        }
    }

    constructor(accountAwareFragment: AccountAwareFragment) {
        this.activity = accountAwareFragment.activity!!
        acountAwareFunc = {
            accountAwareFragment.currentAccount.entropyStore
        }
    }


    fun verifyIdentity(params: BigDialog.Params?, onVerifyIdSuccess: () -> Unit, onClose: () -> Unit) {
        verifyIdentityCloseAware(params, false, onVerifyIdSuccess, onClose)
    }

    fun verifyIdentityDirectlyFinger(title: String, onVerifyIdSuccess: () -> Unit, onClose: () -> Unit) {
        if (activity.canUseFingerprint()) {
            showFingerCheck(BigDialog.Params(bigTitle = title), onVerifyIdSuccess, onClose)
        } else {
            verifyIdentityCloseAware(BigDialog.Params(bigTitle = title), true, onVerifyIdSuccess, onClose)
        }
    }


    private fun showFingerCheck(params: BigDialog.Params, onVerifyIdSuccess: () -> Unit, onClose: () -> Unit) {
        FingerprintAuthFragment().apply {
            titleText = params.bigTitle
            authResult = { resultCode ->
                when (resultCode) {
                    FingerprintAuthFragment.FP_CANCEL -> verifyIdentityCloseAware(
                        params,
                        true,
                        onVerifyIdSuccess,
                        onClose
                    )
                    FingerprintAuthFragment.FP_ERROR -> verifyIdentityCloseAware(
                        params,
                        true,
                        onVerifyIdSuccess,
                        onClose
                    )
                    FingerprintAuthFragment.FP_SUCCESS -> onVerifyIdSuccess()
                }
            }
            show(this@IdentityVerify.activity.supportFragmentManager, "FingerprintAuthFragment")
        }
    }


    fun verifyIdentityCloseAware(
        params: BigDialog.Params?,
        usePasswordOnly: Boolean,
        onVerifyIdSuccess: () -> Unit,
        onClose: () -> Unit
    ) {
        if (params == null) {
            return
        }
        BigDialog(activity).apply {
            setUiParams(params)
            setPasswordEnable(usePasswordOnly || !activity.canUseFingerprint())
            setFirstButtonClickListener {
                if (activity.canUseFingerprint() && !usePasswordOnly) {
                    dismiss()
                    showFingerCheck(params, onVerifyIdSuccess, onClose)
                } else {
                    if (Wallet.checkPassphrase(acountAwareFunc(), getPwdInputText())) {
                        onVerifyIdSuccess()
                        dismiss()
                    } else {
                        activity.showToast(R.string.password_error)
                    }
                }
            }
            closeListener = onClose
            show()
        }
    }

}