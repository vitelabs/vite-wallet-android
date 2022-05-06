package net.vite.wallet.balance.txsend

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
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_transfer_eth.*
import net.vite.wallet.ExternalPriceCenter
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.contacts.readonly.ReadOnlyContactListActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.LogoTitleNotifyDialog
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.isValidEthAddress
import net.vite.wallet.network.amountInBase
import net.vite.wallet.network.eth.EthNet
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.security.IdentityVerify
import net.vite.wallet.utils.hasNetWork
import net.vite.wallet.utils.showToast
import net.vite.wallet.utils.toHex
import net.vite.wallet.vep.EthUrlTransferParams
import net.vite.wallet.vep.QrcodeParseResult
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.min


class EthTxActivity : UnchangableAccountAwareActivity() {

    companion object {
        fun show(context: Context, params: EthUrlTransferParams) {
            context.startActivity(Intent(context, EthTxActivity::class.java).apply {
                putExtra("EthUrlTransferParams", Gson().toJson(params))
            })
        }

        fun show(context: Context, tokenCode: String) {
            context.startActivity(Intent(context, EthTxActivity::class.java).apply {
                putExtra("tokenCode", tokenCode)
            })
        }
    }

    private lateinit var ethVm: EthSendViewModel
    private lateinit var balanceVm: BalanceViewModel
    private lateinit var identityVerify: IdentityVerify
    private var costEthString = ""
    private var costEth = BigDecimal.ZERO
    private var nowGasPrice = BigInteger.ZERO
    var isErc20 = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_eth)
        val nowAddr = currentAccount.nowEthAddress()
        if (nowAddr == null) {
            showToast("empty login address")
            finish()
            return
        }

        val tokenCode = intent.getStringExtra("tokenCode")
        val normalTokenInfo = if (tokenCode == null) {
            val v = intent.getStringExtra("EthUrlTransferParams")
            if (v == null) {
                showToast("not passed any value")
                finish()
                return
            }
            val ethUrlTransferParams = try {
                Gson().fromJson<EthUrlTransferParams>(v, EthUrlTransferParams::class.java)
            } catch (e: Exception) {
                showToast("url not recognized")
                finish()
                return
            }
            ethUrlTransferParams?.toAddr?.let {
                toAddrEditTxt.setText(it)
                toAddrEditTxt.isEnabled = false
            }


            val contactAddress = ethUrlTransferParams.contractAddress
            if (contactAddress == null) {
                isErc20 = false
                TokenInfoCenter.getEthEthTokenInfo()
            } else {
                isErc20 = true
                TokenInfoCenter.getTokenInfoIncacheByTokenAddr(contactAddress)
            }?.also {
                it.decimal
            }.also { tokeninfo ->
                ethUrlTransferParams?.amount?.let {
                    moneyAmountEditTxt.setText(
                        it.amountInBase(tokeninfo?.decimal ?: 0)
                            .toLocalReadableText(8)
                    )
                    moneyAmountEditTxt.isEnabled = false
                }

            }
        } else {
            isErc20 = TokenInfoCenter.EthEthTokenCodes.tokenCode != tokenCode
            TokenInfoCenter.getTokenInfoInCache(tokenCode)
        }

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

        if (decimal == 0) {
            moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789")
        } else {
            moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789.")
        }

        val tokenAddress = normalTokenInfo.tokenAddress
        if (tokenAddress == null && normalTokenInfo.tokenCode != TokenInfoCenter.EthEthTokenCodes.tokenCode) {
            showToast("empty token tokenAddress")
            finish()
            return
        }

        if (tokenAddress == null) {
            momoEditTxt.visibility = View.VISIBLE
        } else {
            momoEditTxt.visibility = View.GONE
        }

        identityVerify = IdentityVerify(this)


        coinName.text = getString(R.string.transfer_token_with_coin, symbol)

        tokenWidget.setup(normalTokenInfo.icon)

        initProfileDetail(nowAddr, normalTokenInfo)


        balanceVm = ViewModelProviders.of(this)[BalanceViewModel::class.java]
        ethVm = ViewModelProviders.of(this)[EthSendViewModel::class.java]

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

        ethVm.sendTxLd.observe(this, Observer { ethSendTransaction ->
            ethSendTransaction?.let { ethSendTransaction ->
                if (ethSendTransaction.transactionHash != null) {
                    LogoTitleNotifyDialog(this@EthTxActivity).apply {
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
                    ExternalPriceCenter.getPrice(tokenCode ?: "")
                )?.toCurrencyText(2)?.let {
                    transferValueTxt.text = it
                    transferValueTxt.visibility = View.VISIBLE
                } ?: kotlin.run {
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

            if (!isValidEthAddress(inputAddrTxt)) {
                toAddrEditTxt.requestFocus()
                showToast(R.string.input_address_not_valid)
                return@setOnClickListener
            }

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

                if (sendAmountBig == BigInteger.ZERO) {
                    moneyAmountEditTxt.requestFocus()
                    showToast(R.string.cant_send_zero)
                    return@setOnClickListener
                }
                sendAmountBig.toString()

            } ?: kotlin.run {
                moneyAmountEditTxt.requestFocus()
                showToast(R.string.please_input_correct_amount)
                return@setOnClickListener
            }

            verifyIdAndSend(normalTokenInfo, symbol, inputAddrTxt, amountTxt, amount)
        }

        scanBtn.setOnClickListener {
            scanQrcode()
        }

        contactBtn.setOnClickListener {
            ReadOnlyContactListActivity.show(this, ReadOnlyContactListActivity.ETH_CONTACT, 1001)
        }

        contactMyAddress.setOnClickListener {
            ReadOnlyContactListActivity.show(this, ReadOnlyContactListActivity.MY_ETH_ADDRESS, 1001)
            transferToExtra.visibility = View.GONE
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

    }

    private fun verifyIdAndSend(
        normalTokenInfo: NormalTokenInfo,
        symbol: String?,
        inputAddrTxt: String,
        amountTxt: String,
        amount: String
    ) {
        identityVerify.verifyIdentity(
            BigDialog.Params(
                bigTitle = getString(R.string.pay),
                secondTitle = getString(R.string.receive_address),
                firstButtonTxt = getString(R.string.confirm_pay),
                tokenIcon = normalTokenInfo.icon,
                tokenSymbol = symbol,
                secondValue = inputAddrTxt,
                amount = amountTxt.toBigDecimal().stripTrailingZeros().toPlainString(),
                feeAmount = costEthString,
                feeTokenSymbol = "ETH"
            ),
            {
                if (normalTokenInfo.tokenCode == TokenInfoCenter.EthEthTokenCodes.tokenCode) {
                    ethVm.sendEth(
                        EthNet.EthSendParams(
                            toAddr = inputAddrTxt,
                            amount = amount.toBigInteger(),
                            gasPrice = nowGasPrice,
                            gasLimit = normalTokenInfo.getGasLimit(),
                            privateKey = currentAccount.nowEthPrivateKey() ?: "",
                            erc20ContractAddr = null,
                            momo = momoEditTxt.text.toString().toByteArray().toHex()
                        )
                    )
                } else {
                    ethVm.sendErc20Token(
                        EthNet.EthSendParams(
                            toAddr = inputAddrTxt,
                            amount = amount.toBigInteger(),
                            gasPrice = nowGasPrice,
                            gasLimit = normalTokenInfo.getGasLimit(),
                            privateKey = currentAccount.nowEthPrivateKey() ?: "",
                            erc20ContractAddr = normalTokenInfo.tokenAddress!!
                        )
                    )
                }
            },
            {}
        )
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
//        moneyAmountEditTxt.customRightText = normalTokenInfo.symbol ?: ""
        moneyAmountEditTxt.customRightText = getString(R.string.all)
        moneyAmountEditTxt.colorStr = "#007AFF"
        moneyAmountEditTxt.setOnRightTextClickListener {

            var value = normalTokenInfo.smallestToBaseUnit(normalTokenInfo.balance().toBigDecimal())
                .minus(if (isErc20) BigDecimal.ZERO else costEth)
//            var value = normalTokenInfo.smallestToBaseUnit(normalTokenInfo.balance().toBigDecimal())
//                .minus(if (isErc20) costEth else BigDecimal.ZERO)
            value = if (value < BigDecimal.ZERO) BigDecimal.ZERO else value
            moneyAmountEditTxt.setText(
                value.toLocalReadableText(8)
            )
        }
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        transferToExtra.visibility = View.GONE
        if (qrcodeParseResult.result is EthUrlTransferParams) {
            toAddrEditTxt.setText(qrcodeParseResult.result.toAddr ?: "")
        }
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

}
