package net.vite.wallet.balance.conversion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_convert_eth_vite.*
import net.vite.wallet.ExternalPriceCenter
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.ViteConfig
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.balance.txsend.EthSendViewModel
import net.vite.wallet.contacts.readonly.ReadOnlyContactListActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.LeftAlignTextTitleNotifyDialog
import net.vite.wallet.dialog.LogoTitleNotifyDialog
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.eth.EthNet
import net.vite.wallet.network.http.gw.GwConvertBindBody
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.security.IdentityVerify
import net.vite.wallet.utils.*
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.vep.ViteUrlTransferParams
import org.vitelabs.mobile.Mobile
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Sign
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.min


class EthViteConversionTxActivity : UnchangableAccountAwareActivity() {
    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, EthViteConversionTxActivity::class.java))
        }
    }

    private lateinit var ethVm: EthSendViewModel
    private lateinit var gwViewModel: GwViewModel
    private lateinit var balanceVm: BalanceViewModel
    private lateinit var identityVerify: IdentityVerify
    private var costEthString = ""
    private var costEth = BigDecimal.ZERO
    private var nowGasPrice = BigInteger.ZERO
    private var lastTransferParams: EthNet.EthSendParams? = null
    private var exchangeToAddress: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convert_eth_vite)
        val nowAddr = currentAccount.nowEthAddress()
        if (nowAddr == null) {
            showToast("empty login address")
            finish()
            return
        }

        val tokenCode = TokenInfoCenter.EthViteTokenCodes.tokenCode
        val normalTokenInfo = TokenInfoCenter.getTokenInfoInCache(tokenCode)

        if (normalTokenInfo == null) {
            showToast("empty token info")
            finish()
            return
        }

        val symbol = normalTokenInfo.symbol
        if (symbol == null) {
            showToast("empty token symbol")
            finish()
            return
        }

        val decimal = normalTokenInfo.decimal
        if (decimal == null) {
            showToast("empty token decimal")
            finish()
            return
        }

        moneyAmountEditTxt.customRightText = getString(R.string.convert_all)
        moneyAmountEditTxt.colorStr = "#007AFF"
        moneyAmountEditTxt.setOnRightTextClickListener {
            moneyAmountEditTxt.setText(normalTokenInfo.balanceText(8))
        }

        moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789.")

        convertRecord.setOnClickListener {
            startActivity(createBrowserIntent("${NetConfigHolder.netConfig.remoteEtherscanPrefix}/address/$nowAddr#tokentxns"))
        }

        val tokenAddress = normalTokenInfo.tokenAddress
        if (tokenAddress == null) {
            showToast("empty token tokenAddress")
            finish()
            return
        }

        identityVerify = IdentityVerify(this)


        initProfileDetail(nowAddr, normalTokenInfo)


        balanceVm = ViewModelProviders.of(this)[BalanceViewModel::class.java]
        ethVm = ViewModelProviders.of(this)[EthSendViewModel::class.java]
        gwViewModel = ViewModelProviders.of(this)[GwViewModel::class.java]


        gwViewModel.network.observe(this, Observer {
            cycleProgressBar.visibility = it.progressVisible
        })

        balanceVm.rtEthAccInfo.observe(this, Observer {
            myBalance.text = normalTokenInfo.balanceText(8)
        })
        ethVm.gasPriceLd.observe(this, Observer {
            val priceGv =
                Convert.fromWei(it.toBigDecimal(), Convert.Unit.GWEI).toBigInteger().toInt()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                gasPriceSeekBar.min = priceGv / 2
            }

            gasPriceSeekBar.max = priceGv * 3

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                gasPriceSeekBar.setProgress(priceGv, true)
            } else {
                gasPriceSeekBar.setProgress(priceGv)
            }

            refreshPrice(priceGv, normalTokenInfo)
        })

        ethVm.netWorkLd.observe(this, Observer {
            cycleProgressBar.visibility = it.progressVisible
            if (it.isError()) {
                TextTitleNotifyDialog(this).apply {
                    setMessage(R.string.eth_send_last_failed)
                    setCancelable(true)
                    setBottom(R.string.confirm) {
                        it.dismiss()
                    }
                    show()
                }
            }
        })

        ethVm.signTxLd.observe(this, Observer { result ->
            result?.let {
                if (nowAddr != it.address) {
                    return@Observer
                }

                val s =
                    "{\"pub_key\":\"${it.pubKey}\",\"eth_tx_hash\":\"${it.txHash}\",\"eth_addr\":\"${it.address}\"," +
                            "\"vite_addr\":\"${exchangeToAddress}\",\"value\":\"${lastTransferParams?.amount?.toString()!!}\"}"

                lastTransferParams?.privateKey?.hexToBytes()?.let { prikeyByte ->
                    val signData = Sign.signMessage(
                        Hash.sha3(s.toByteArray()),
                        ECKeyPair.create(prikeyByte),
                        false
                    )
                    val size = signData.r.size + signData.s.size + 1
                    if (size != 65) {
                        return@Observer
                    }
                    val signDataBytes = ByteArray(size)
                    System.arraycopy(signData.r, 0, signDataBytes, 0, signData.r.size)
                    System.arraycopy(signData.s, 0, signDataBytes, signData.r.size, signData.s.size)
                    signDataBytes[64] = (signData.v - 27.toByte()).toByte()


                    val body = GwConvertBindBody(
                        ethAddr = it.address,
                        ethTxHash = it.txHash,
                        pubKey = it.pubKey,
                        signature = "0x${signDataBytes.toHex()}",
                        viteAddr = exchangeToAddress,
                        value = lastTransferParams?.amount?.toString()!!
                    )
                    gwViewModel.bind(body)
                }
            }
        })


        gwViewModel.bindResultLd.observe(this, Observer { isSuccess ->
            if (isSuccess) {
                lastTransferParams?.let { ethVm.sendErc20Token(it) }
            } else {
                showToast(R.string.convert_failed_by_bind)
            }
        })

        ethVm.sendTxLd.observe(this, Observer {
            it?.let { ethSendTransaction ->
                if (ethSendTransaction.transactionHash != null) {
                    LogoTitleNotifyDialog(this@EthViteConversionTxActivity).apply {
                        enableTopImage(true)
                        setMessage(getString(R.string.eth_transfer_req_commited))
                        setCancelable(false)
                        setBottom(getString(R.string.confirm)) {
                            this.dismiss()
                            finish()
                        }
                        show()
                    }
                    return@Observer
                }
                val err = ethSendTransaction.error
                if (ethSendTransaction.error != null) {
                    showToast(err.message ?: getString(R.string.failed))
                    return@Observer
                }
                showToast(R.string.failed)
            }
        })


        myAddressBtn.setOnClickListener {
            ReadOnlyContactListActivity.show(this, ReadOnlyContactListActivity.MY_ADDRESS, 1001)
        }

        toAddrEditTxt.onClickRightDrawableListener = {
            transferToExtra.visibility = if (transferToExtra.visibility == View.GONE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        toAddrEditTxt.setText(currentAccount.nowViteAddress() ?: "")

        gasPriceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, gwei: Int, fromUser: Boolean) {
                refreshPrice(gwei, normalTokenInfo)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        refreshPrice(1, normalTokenInfo)

        moneyAmountEditTxt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.toString()?.toBigDecimalOrNull()?.multiply(
                    ExternalPriceCenter.getPrice(tokenCode)
                )?.toCurrencyText(2)?.let {
                    transferValueTxt.text = it
                    transferValueTxt.visibility = View.VISIBLE
                } ?: run {
                    transferValueTxt.visibility = View.GONE
                }
            }
        })
        transferBtn.setOnClickListener {
            if (!hasNetWork()) {
                showToast(R.string.net_work_error)
                return@setOnClickListener
            }

            val inputAddrTxt = toAddrEditTxt.editableText.toString()

            if (!Mobile.isValidAddress(inputAddrTxt)) {
                toAddrEditTxt.requestFocus()
                showToast(R.string.input_address_not_valid)
                return@setOnClickListener
            }
            exchangeToAddress = inputAddrTxt

            val amountTxt = moneyAmountEditTxt.editableText.toString()
            val amount = amountTxt.toBigDecimalOrNull()?.let { sendAmount ->
                val decimalsLength = if (amountTxt.indexOf(".") != -1) {
                    amountTxt.length - amountTxt.indexOf(".") - 1
                } else {
                    0
                }
                if (decimalsLength > min(8, decimal)) {
                    moneyAmountEditTxt.requestFocus()
                    showToast(
                        getString(
                            R.string.this_token_max_input_number_decimal,
                            symbol,
                            min(8, decimal)
                        )
                    )
                    return@setOnClickListener
                }

                val sendAmountBig =
                    sendAmount.multiply(BigDecimal.TEN.pow(decimal)).toBigInteger()

                if (sendAmountBig > normalTokenInfo.balance()) {
                    moneyAmountEditTxt.requestFocus()
                    showToast(R.string.balance_not_enough)
                    return@setOnClickListener
                }
                sendAmountBig.toString()

            } ?: run {
                moneyAmountEditTxt.requestFocus()
                showToast(R.string.please_input_correct_amount)
                return@setOnClickListener
            }


            identityVerify.verifyIdentity(
                BigDialog.Params(
                    bigTitle = getString(R.string.pay),
                    secondTitle = getString(R.string.receive_address),
                    firstButtonTxt = getString(R.string.confirm_pay),
                    tokenIcon = normalTokenInfo.icon,
                    tokenSymbol = symbol,
                    secondValue = EthNet.DestoryViteAddress,
                    amount = amountTxt.toBigDecimal().stripTrailingZeros().toPlainString(),
                    feeAmount = costEthString,
                    feeTokenSymbol = "ETH"
                ),
                {
                    lastTransferParams = EthNet.EthSendParams(
                        toAddr = EthNet.DestoryViteAddress,
                        amount = amount.toBigInteger(),
                        gasPrice = nowGasPrice,
                        gasLimit = normalTokenInfo.getGasLimit(),
                        privateKey = AccountCenter.getCurrentAccount()?.nowEthPrivateKey() ?: "",
                        erc20ContractAddr = normalTokenInfo.tokenAddress
                    )
                    ethVm.signErc20TransferTx(lastTransferParams!!)
                },
                {}
            )

        }


        scanBtn.setOnClickListener {
            scanQrcode()
        }

        contactBtn.setOnClickListener {
            ReadOnlyContactListActivity.show(this, ReadOnlyContactListActivity.VITE_CONTACT, 1001)
        }


        toAddrEditTxt.onClickRightDrawableListener = {
            transferToExtra.visibility = if (transferToExtra.visibility == View.GONE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }


        gasInfo.setOnClickListener {
            TextTitleNotifyDialog(this).apply {
                setMessage(getString(R.string.gas_info))
                setBottom(getString(R.string.button_ok))
                show()
            }
        }

        info4Exchange.setOnClickListener {
            showInfo4ExchangeDialog()
        }

        if (!ViteConfig.get().kvstore.getBoolean("hasShowedInfo4exchange", false)) {
            showInfo4ExchangeDialog()
            ViteConfig.get().kvstore.edit().putBoolean("hasShowedInfo4exchange", true).apply()
        }

    }

    private fun showInfo4ExchangeDialog() {
        LeftAlignTextTitleNotifyDialog(this@EthViteConversionTxActivity).apply {
            setTitle(getString(R.string.quick_understand_exchage))
            setMessage(getString(R.string.info4convert))
            setBottom(getString(R.string.i_see))
            show()
        }
    }

    override fun onStart() {
        super.onStart()
        ethVm.getGas()
    }

    private fun refreshPrice(gwei: Int, normalTokenInfo: NormalTokenInfo) {
        gasCostInGwei.text = "${gwei} Gwei"
        nowGasPrice = Convert.toWei(gwei.toBigDecimal(), Convert.Unit.GWEI).toBigInteger()
        costEth =
            Convert.fromWei(
                normalTokenInfo.getGasLimit().multiply(nowGasPrice).toBigDecimal(),
                Convert.Unit.ETHER
            )
        costEthString = costEth.toLocalReadableText(5)
        val priceString =
            costEth.multiply(
                ExternalPriceCenter.getPrice(TokenInfoCenter.EthEthTokenCodes.tokenCode)
            ).toCurrencyText(4)

        gasCost.text = "${costEthString}ether ≈ $priceString"
    }

    private fun initProfileDetail(nowAddr: String, normalTokenInfo: NormalTokenInfo) {
        myAddress.text = nowAddr
        myBalance.text = normalTokenInfo.balanceText(8)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        transferToExtra.visibility = View.GONE
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra("address")?.let {
                toAddrEditTxt.setText(it)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        transferToExtra.visibility = View.GONE
        if (qrcodeParseResult.result is ViteUrlTransferParams) {
            toAddrEditTxt.setText(qrcodeParseResult.result.toAddr)
        }

    }


}
