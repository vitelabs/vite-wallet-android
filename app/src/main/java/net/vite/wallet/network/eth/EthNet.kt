package net.vite.wallet.network.eth

import androidx.annotation.Keep
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.utils.toHex
import net.vite.wallet.utils.toSafeByteArray
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.contracts.eip20.generated.ERC20
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.io.IOException
import java.math.BigInteger

object EthNet {
    val DestoryViteAddress = "0x1111111111111111111111111111111111111111"
    var web3: Web3j? = null

    @Volatile
    private var currentUrl: String? = null

    fun getWeb3j(): Web3j {
        if (currentUrl == NetConfigHolder.netConfig.getEthNodeUrl()) {
            return web3!!
        }
        synchronized(DestoryViteAddress) {
            if (currentUrl != NetConfigHolder.netConfig.getEthNodeUrl()) {
                currentUrl = NetConfigHolder.netConfig.getEthNodeUrl()
                web3 = Web3j.build(HttpService(currentUrl, EthOkHttpClient.create()))
            }
        }
        return web3!!
    }

    fun getTokenBalance(accAddr: String, contractAddr: String = ""): BigInteger? {
        return try {
            if (contractAddr != "") {
                ERC20.load(
                    contractAddr,
                    getWeb3j(),
                    ReadonlyTransactionManager(getWeb3j(), accAddr),
                    DefaultGasProvider()
                ).balanceOf(accAddr).send()
            } else {
                getWeb3j().ethGetBalance(accAddr, DefaultBlockParameterName.LATEST).send().balance
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getGasPrice(): BigInteger? {
        return try {
            getWeb3j().ethGasPrice().send().gasPrice
        } catch (e: Exception) {
            null
        }
    }

    @Keep
    data class EthSendParams(
        val toAddr: String,
        val amount: BigInteger,
        val gasPrice: BigInteger,
        val gasLimit: BigInteger,
        val privateKey: String,
        val erc20ContractAddr: String?,
        val momo: String? = ""
    )


    @Keep
    data class EthSignResult(
        val txHash: String,
        val address: String,
        val pubKey: String
    )

    @Throws(IOException::class)
    fun signErc20TransferTx(
        params: EthSendParams
    ): EthSignResult {
        val f = Function(
            "transfer",
            arrayListOf(Address(params.toAddr), Uint256(params.amount)) as List<Type<Any>>,
            mutableListOf(TypeReference.create(Bool::class.java)) as List<TypeReference<*>>?
        )
        val rawTransactionManager =
            RawTransactionManager(getWeb3j(), Credentials.create(params.privateKey))
        val credentials = Credentials.create(params.privateKey)
        val blocksignature = rawTransactionManager.sign(
            RawTransaction.createTransaction(
                getNonce(credentials.address),
                params.gasPrice,
                params.gasLimit,
                params.erc20ContractAddr,
                BigInteger.ZERO,
                FunctionEncoder.encode(f)
            )
        )
        val txHashLocal = Hash.sha3(blocksignature)
        val hexPubKey = "0x04${credentials.ecKeyPair.publicKey.toSafeByteArray().toHex()}"

        val ethAddress = Keys.toChecksumAddress(credentials.address)
        return EthSignResult(
            txHash = txHashLocal,
            pubKey = hexPubKey,
            address = ethAddress
        )
    }

    @Throws(IOException::class)
    fun getNonce(address: String): BigInteger {
        val ethGetTransactionCount = getWeb3j().ethGetTransactionCount(
            address, DefaultBlockParameterName.PENDING
        ).send()

        return ethGetTransactionCount.transactionCount
    }

    fun sendErc20Tx(
        params: EthSendParams
    ): EthSendTransaction? {
        val f = Function(
            "transfer",
            arrayListOf(Address(params.toAddr), Uint256(params.amount)) as List<Type<Any>>,
            mutableListOf(TypeReference.create(Bool::class.java)) as List<TypeReference<*>>?
        )

        val rawTransactionManager =
            RawTransactionManager(getWeb3j(), Credentials.create(params.privateKey))
        return rawTransactionManager.sendTransaction(
            params.gasPrice,
            params.gasLimit,
            params.erc20ContractAddr,
            FunctionEncoder.encode(f),
            BigInteger.ZERO
        )
    }

    fun sendEth(
        params: EthSendParams
    ): EthSendTransaction? {
        val rawTransaction =
            RawTransactionManager(getWeb3j(), Credentials.create(params.privateKey))
        return rawTransaction.sendTransaction(
            params.gasPrice,
            params.gasLimit,
            params.toAddr,
            params.momo,
            params.amount
        )
    }


}