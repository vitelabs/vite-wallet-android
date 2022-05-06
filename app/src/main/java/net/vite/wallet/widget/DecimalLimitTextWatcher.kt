package net.vite.wallet.widget

import android.text.Editable
import android.text.TextWatcher
import kotlin.math.min

class DecimalLimitTextWatcher(val tokenDecimalLength: Int, val maxLength: Int = 8) : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        val max = min(maxLength, tokenDecimalLength)
        val input = s?.toString() ?: ""
        val decimalsLength = if (input.indexOf(".") != -1) {
            input.length - input.indexOf(".") - 1
        } else {
            0
        }
        if (decimalsLength > max) {
            s?.delete(input.indexOf(".") + max + 1, input.length)
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }
}