package net.vite.wallet.vep

import androidx.annotation.Keep
import net.vite.wallet.isValidEthAddress
import java.math.BigInteger


class InvalidEthUrlException(msg: String) : Exception(msg)


@Keep
data class EthUrlTransferParams(
    var toAddr: String? = null,
    var contractAddress: String? = null,
    var amount: BigInteger? = null,
    var decimal: Int? = null
)


fun decodeEthErc20CommunityUrl(url: String): EthUrlTransferParams {
    val scheme = url.split(":")
    if (scheme.size != 2 || scheme[0] != "ethereum") {
        throw InvalidEthUrlException("Decode : error")
    }
    val addrWithParams = scheme[1].split("?")

    if (addrWithParams.size > 2) {
        throw InvalidEthUrlException("Decode ? error")
    }

    var toAddress: String? = null
    if (isValidEthAddress(addrWithParams[0])) {
        toAddress = addrWithParams[0]
    } else {
        throw InvalidEthUrlException("Address not valid ${addrWithParams[0]}")
    }

    if (addrWithParams.size == 1) {
        return EthUrlTransferParams(
            toAddr = toAddress
        )
    }

    val paramsMap = addrWithParams[1].split("&").map {
        val kv = it.split("=")
        if (kv.size != 2) {
            throw InvalidEthUrlException("Decode kv error")
        }
        kv[0] to kv[1]
    }.toMap()

    var amount: BigInteger? = null
    var contractAddress: String? = null
    var decimal: Int? = null
    paramsMap.forEach {
        if (it.key == "decimal") {
            if (it.value.toIntOrNull() == null) {
                throw InvalidEthUrlException("decimal format error")
            }
            decimal = it.value.toInt()
        } else if (it.key == "value") {
            val value = it.value.toBigIntegerOrNull()
            if (value == null) {
                throw InvalidEthUrlException("value format error")
            } else {
                amount = value
            }
        } else if (it.key == "contractAddress") {
            if (!isValidEthAddress(it.value)) {
                throw InvalidEthUrlException("contractAddress format error")
            } else {
                contractAddress = it.value
            }
        } else {
            throw InvalidEthUrlException("Unknown KEY ${it.key}")
        }
    }

    return EthUrlTransferParams(
        toAddr = toAddress,
        contractAddress = contractAddress,
        amount = amount,
        decimal = decimal
    )

}

fun decodeEthTransferUrl(url: String): EthUrlTransferParams {
    val scheme = url.split(":")
    if (scheme.size != 2 || scheme[0] != "ethereum") {
        throw InvalidEthUrlException("Decode : error")
    }

    val addrWithFuncWithParams = scheme[1].split("?")

    if (addrWithFuncWithParams.size > 2) {
        throw InvalidEthUrlException("Decode ? error")
    }

    val addrWithFunc = addrWithFuncWithParams[0].split("/")
    if (addrWithFunc.size > 2) {
        throw InvalidEthUrlException("Decode / error")
    }

    val address = addrWithFunc[0]
    if (!isValidEthAddress(address)) {
        throw InvalidEthUrlException("Address not valid $address")
    }
    var contractAddress: String? = null
    var toAddress: String? = null
    var isErc20Transfer = false

    if (addrWithFunc.size == 2) {
        if (addrWithFunc[1] != "transfer") {
            throw InvalidEthUrlException("unspport this func ${addrWithFunc[1]}")
        }
        isErc20Transfer = true
        contractAddress = address
    } else {
        toAddress = address
    }

    if (addrWithFuncWithParams.size == 1) {
        if (toAddress != null) {
            return EthUrlTransferParams(toAddr = toAddress)
        }
        if (contractAddress != null) {
            throw InvalidEthUrlException("unsupport this func invoke")
        }
    }

    val paramsMap = addrWithFuncWithParams[1].split("&").map {
        val kv = it.split("=")
        if (kv.size != 2) {
            throw InvalidEthUrlException("Decode kv error")
        }
        kv[0] to kv[1]
    }.toMap()


    var amount: BigInteger? = null
    var decimal: Int? = null
    paramsMap.forEach {
        if (it.key == "decimal" || it.key == "decimals") {
            decimal = it.value.toIntOrNull()
            if (decimal == null) {
                throw InvalidEthUrlException("decimal format error")
            }
        } else if (it.key == "value") {
            if (isErc20Transfer) {
                throw InvalidEthUrlException("erc20 transfer not support value")
            }
            val value = it.value.toBigIntegerOrNull()
            if (value == null) {
                throw InvalidEthUrlException("value format error")
            } else {
                amount = value
            }
        } else if (it.key == "uint256") {
            if (!isErc20Transfer) {
                throw InvalidEthUrlException("eth transfer not support uint256")
            }
            val value = it.value.toBigIntegerOrNull()
            if (value == null) {
                throw InvalidEthUrlException("value format error")
            } else {
                amount = value
            }
        } else if (it.key == "address") {
            if (!isValidEthAddress(it.value)) {
                throw InvalidEthUrlException("contractAddress format error")
            } else {
                toAddress = it.value
            }
        } else {
            throw InvalidEthUrlException("Unknown KEY ${it.key}")
        }
    }

    if (toAddress == null) {
        throw InvalidEthUrlException("address is null")
    }

    return EthUrlTransferParams(
        toAddr = toAddress,
        contractAddress = contractAddress,
        amount = amount,
        decimal = decimal
    )
}
