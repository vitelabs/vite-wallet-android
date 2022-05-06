/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package net.vite.wallet.security

import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.widget.ImageView
import android.widget.TextView
import net.vite.wallet.R
import net.vite.wallet.logt


class FingerprintUiHelper
internal constructor(
    private val mFingerprintManager: FingerprintManager,
    private val mIcon: ImageView, private val mErrorTextView: TextView, private val mCallback: Callback
) : FingerprintManager.AuthenticationCallback() {
    private var mCancellationSignal: CancellationSignal? = null

    private var mSelfCancelled: Boolean = false

    val isFingerprintAuthAvailable: Boolean
        get() = mFingerprintManager.isHardwareDetected && mFingerprintManager.hasEnrolledFingerprints()

    private val mResetErrorTextRunnable = Runnable {
        mErrorTextView.text = ""
        mIcon.setImageResource(R.mipmap.fingerprint)
    }

    fun startListening() {
        if (!isFingerprintAuthAvailable) {
            return
        }
        mCancellationSignal = CancellationSignal()
        mSelfCancelled = false

        mFingerprintManager
            .authenticate(null, mCancellationSignal, 0, this, null)
        mIcon.setImageResource(R.mipmap.fingerprint)
    }

    fun stopListening() {
        if (mCancellationSignal != null) {
            mSelfCancelled = true
            mCancellationSignal!!.cancel()
            mCancellationSignal = null
        }
    }

    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
        logt("onAuthenticationError $errMsgId + $errString")
        if (!mSelfCancelled) {
            showError(errString)
            mIcon.postDelayed({
                mCallback.onError()
            }, ERROR_TIMEOUT_MILLIS)
        }
    }

    override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence) {
        logt("onAuthenticationHelp $helpMsgId + $helpString")
    }

    override fun onAuthenticationFailed() {
        logt("onAuthenticationFailed")
        showError(
            mIcon.resources.getString(
                R.string.fingerprint_not_recognized
            )
        )
    }

    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
        mErrorTextView.removeCallbacks(mResetErrorTextRunnable)
        mIcon.setImageResource(R.drawable.ic_fingerprint_success)
        mIcon.postDelayed({ mCallback.onAuthenticated() }, SUCCESS_DELAY_MILLIS)
    }

    private fun showError(error: CharSequence) {
        mIcon.setImageResource(R.drawable.ic_fingerprint_error)
        mErrorTextView.text = error
        mErrorTextView.setTextColor(
            mErrorTextView.resources.getColor(R.color.warning_color, null)
        )
        mErrorTextView.removeCallbacks(mResetErrorTextRunnable)
        mErrorTextView.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT_MILLIS)
    }

    interface Callback {

        fun onAuthenticated()

        fun onError()
    }

    companion object {

        private val ERROR_TIMEOUT_MILLIS: Long = 1600
        private val SUCCESS_DELAY_MILLIS: Long = 1300
    }
}
