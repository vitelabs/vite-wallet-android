package net.vite.wallet.account

import org.vitelabs.mobile.Mobile

class InValidMnemonicException(message: String) : Exception(message)

class CheckedMnemonic(
    val formatMnemonic: String,
    val primaryAddr: String,
    val lang: String
)

object MnemonicHelper {

    fun checkMnemonic(mnemonic: String): CheckedMnemonic {
        val supportLang = listOf("en", "zh-Hans")
        val words = mnemonic.split("\\s+")
        val oneSpaceMnemonic = words.fold(StringBuilder()) { acc, s ->
            acc.append(s).append(" ")
        }.toString().trim()


        for (item in supportLang) {
            kotlin.runCatching {
                Mobile.tryTransformMnemonic(oneSpaceMnemonic, item, "")
            }.getOrNull()?.let {
                return CheckedMnemonic(
                    formatMnemonic = oneSpaceMnemonic,
                    primaryAddr = it.hex,
                    lang = item
                )
            }
        }

        val alphas = mnemonic.fold(ArrayList<String>()) { acc, c ->
            if (!c.isWhitespace()) {
                acc.add(c.toString())
            }
            acc
        }
        val oneSpaceMnemonicAlphas = alphas.fold(StringBuilder()) { acc, s ->
            acc.append(s).append(" ")
        }.toString().trim()

        for (item in supportLang) {
            kotlin.runCatching {
                Mobile.tryTransformMnemonic(oneSpaceMnemonicAlphas, item, "")
            }.getOrNull()?.let {
                return CheckedMnemonic(
                    formatMnemonic = oneSpaceMnemonicAlphas,
                    primaryAddr = it.hex,
                    lang = item
                )
            }
        }

        throw InValidMnemonicException(mnemonic)
    }

}