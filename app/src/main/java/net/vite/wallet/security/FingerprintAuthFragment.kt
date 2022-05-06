package net.vite.wallet.security

import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_fp_auth.*
import net.vite.wallet.R


class FingerprintAuthFragment : DialogFragment(), FingerprintUiHelper.Callback {
    companion object {
        val FP_SUCCESS = 0
        val FP_CANCEL = 1
        val FP_ERROR = 2
    }

    var authResult: (resultCode: Int) -> Unit = { _ -> }

    var titleText: String? = null

    override fun onAuthenticated() {
        authResult(FP_SUCCESS)
        dismiss()
    }

    override fun onError() {
        authResult(FP_ERROR)
        dismiss()
    }

    private lateinit var mFingerprintUiHelper: FingerprintUiHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isCancelable = false
        return inflater.inflate(R.layout.dialog_fp_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        titleText?.let { title.text = it }
        mFingerprintUiHelper = FingerprintUiHelper(
            activity!!.getSystemService(FingerprintManager::class.java),
            fpIcon, fpStatus, this
        )

        bottomBtn.setOnClickListener {
            dismiss()
            authResult(FP_CANCEL)
        }
    }

    override fun onResume() {
        super.onResume()
        mFingerprintUiHelper.startListening()
    }

    override fun onPause() {
        super.onPause()
        mFingerprintUiHelper.stopListening()
    }
}