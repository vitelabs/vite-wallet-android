package net.vite.wallet

import net.vite.wallet.network.http.vitex.TokenFamily
import org.vitelabs.mobile.Mobile
import org.web3j.abi.datatypes.Address

fun isValidEthAddress(address: String): Boolean {
    if (address.length != 42) {
        return false
    }
    try {
        Address(address).toUint160()
        return true
//        val checkedAddress = Keys.toChecksumAddress(address)
//        return checkedAddress == address
    } catch (e: Exception) {
        return false
    }

}

fun isValidGrinContractAddress(address: String): Boolean {
    return address.startsWith("http") || Mobile.isValidAddress(address)
}

class Once<T>() {
    constructor(t1: T?) : this() {
        t = t1
    }

    fun get(): T? = t

    fun set(t1: T?) {
        t = t1
    }

    private var t: T? = null
        get() :T? {
            val t1 = field
            field = null
            return t1
        }
}

data class EncodeToUrlParams(
    val family: Int,
    val accountAddress: String,
    val tokenAddress: String? = null,
    val decimal: Int? = null,
    var amount: String? = null,
    var dataStr: String? = null
)

fun encodeToUrl(
    params: EncodeToUrlParams,
    isInEthCommunity: Boolean = false
): String {
    params.apply {
        val paramsKv = ArrayList<Pair<String, String>>()

        val base = when (family) {
            TokenFamily.VITE -> {
                tokenAddress?.let {
                    paramsKv.add("tti" to it)
                }
                amount?.let {
                    paramsKv.add("amount" to it)
                }
                dataStr?.let {
                    paramsKv.add("data" to it)
                }
                "vite:$accountAddress"
            }
            TokenFamily.ETH -> {
                val base = if (isInEthCommunity) {
                    decimal?.let { paramsKv.add("decimal" to it.toString()) }
                    tokenAddress?.let { paramsKv.add("contractAddress" to it) }
                    amount?.let { paramsKv.add("value" to it) }
                    "ethereum:$accountAddress"
                } else {
                    if (tokenAddress == null) {
                        amount?.let { paramsKv.add("value" to it) }
                        "ethereum:$accountAddress"
                    } else {
                        amount?.let { paramsKv.add("uint256" to it) }
                        paramsKv.add("address" to accountAddress)
                        "ethereum:$tokenAddress/transfer"
                    }
                }
                base
            }
            else -> ""
        }

        return if (paramsKv.isEmpty()) {
            base
        } else {
            var kvs = paramsKv.fold(StringBuilder("?")) { acc, pair ->
                acc.append("${pair.first}=${pair.second}&")
            }.toString()
            if (kvs.last() == '&') {
                kvs = kvs.substring(0, kvs.length - 1)
            }
            "$base$kvs"
        }.also {
            logt("urlsss   $it")
        }

    }


}
