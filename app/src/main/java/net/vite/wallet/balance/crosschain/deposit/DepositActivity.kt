package net.vite.wallet.balance.crosschain.deposit

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_crosschain_deposite.*
import net.vite.wallet.ExternalPriceCenter
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.BalanceViewModel
import net.vite.wallet.balance.crosschain.GwCrosschainVM
import net.vite.wallet.balance.crosschain.deposit.list.DepositRecordsActivity
import net.vite.wallet.balance.poll.EthAccountInfoPoll
import net.vite.wallet.balance.txsend.EthSendViewModel
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.LogoTitleNotifyDialog
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.network.eth.EthNet
import net.vite.wallet.network.http.gw.MetaInfoResp
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.security.IdentityVerify
import net.vite.wallet.utils.hasNetWork
import net.vite.wallet.utils.showToast
import net.vite.wallet.widget.DecimalLimitTextWatcher
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger


class DepositActivity : UnchangableAccountAwareActivity() {
    companion object {
        fun show(context: Context, ethTokenCode: String, viteTokenCode: String, viteAddr: String) {
            context.startActivity(Intent(context, DepositActivity::class.java).apply {
                putExtra("ethTokenCode", ethTokenCode)
                putExtra("viteTokenCode", viteTokenCode)
                putExtra("viteAddress", viteAddr)
            })
        }
    }

    private val crosschainVM by viewModels<GwCrosschainVM>()
    private val balanceVm by viewModels<BalanceViewModel>()
    private val ethVm by viewModels<EthSendViewModel>()

    private var viteNormalTokenInfo: NormalTokenInfo = NormalTokenInfo.EMPTY
    private var ethchainNormalTokenInfo: NormalTokenInfo = NormalTokenInfo.EMPTY

    private lateinit var viteAddress: String
    private lateinit var nowEthAddr: String


    private val identityVerify: IdentityVerify by lazy {
        IdentityVerify(this)
    }


    private var gasCostEthString = ""
    private var gasCostInEth = BigDecimal.ZERO
    private var nowGasPriceInWei = BigInteger.ZERO

    private var depositToAddress: String? = null
    private var minimumDepositAmountInSU: BigDecimal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crosschain_deposite)
        val viteTokenCode = intent.getStringExtra("viteTokenCode") ?: kotlin.run {
            finish()
            return
        }
        val ethTokenCode = intent.getStringExtra("ethTokenCode") ?: kotlin.run {
            finish()
            return
        }
        viteAddress = intent.getStringExtra("viteAddress") ?: kotlin.run {
            finish()
            return
        }

        viteNormalTokenInfo = TokenInfoCenter.getTokenInfoInCache(viteTokenCode) ?: kotlin.run {
            finish()
            return
        }

        ethchainNormalTokenInfo =
            viteNormalTokenInfo.allMappedToken().find { it.tokenCode == ethTokenCode }
                ?: kotlin.run {
                    finish()
                    return
                }

        nowEthAddr = currentAccount.nowEthAddress() ?: kotlin.run {
            showToast("empty eth viteAddress")
            finish()
            return
        }

        val tokenAddress = ethchainNormalTokenInfo.tokenAddress
        if (tokenAddress == null &&
            ethchainNormalTokenInfo.tokenCode != TokenInfoCenter.EthEthTokenCodes.tokenCode
        ) {
            showToast("empty token tokenAddress")
            finish()
            return
        }

        sourceChainAddress.text = nowEthAddr
        sourceChainBalance.text = ethchainNormalTokenInfo.balanceText(8)
        myViteAddress.text = viteAddress

        bindCrosschainVMObserver()
        bindEthVmObserver()

        setupTopLayout()
        setupMoneyAmountEditTxt()
        setupGas()

        balanceVm.rtEthAccInfo.observe(this, {
            sourceChainBalance.text = ethchainNormalTokenInfo.balanceText(8)
        })

        refreshGasPrice(1)
        ethVm.getGas()

        transferBtn.setOnClickListener {
            if (!hasNetWork()) {
                showToast(R.string.net_work_error)
                return@setOnClickListener
            }

            if (minimumDepositAmountInSU == null) {
                return@setOnClickListener
            }
            if (depositToAddress == null) {
                return@setOnClickListener
            }

            val amountTxt = moneyAmountEditTxt.editableText.toString()

            val sendAmountInBase = amountTxt.toBigDecimalOrNull() ?: kotlin.run {
                moneyAmountEditTxt.requestFocus()
                showToast(R.string.please_input_correct_amount)
                return@setOnClickListener
            }

            val sendAmountInSU = ethchainNormalTokenInfo.baseToSmallestUnit(sendAmountInBase)

            if (sendAmountInSU < minimumDepositAmountInSU) {
                showToast(
                    getString(
                        R.string.deposit_amount_too_low,
                        ethchainNormalTokenInfo.amountTextWithSymbol(
                            sendAmountInSU.toBigInteger(),
                            3
                        )
                    )
                )
                moneyAmountEditTxt.requestFocus()
                return@setOnClickListener
            }

            if (sendAmountInSU > ethchainNormalTokenInfo.balance().toBigDecimal()) {
                moneyAmountEditTxt.requestFocus()
                showToast(R.string.balance_not_enough)
                return@setOnClickListener
            }

            if (Convert.toWei(
                    gasCostInEth,
                    Convert.Unit.ETHER
                ) > EthAccountInfoPoll.myEthBalanceInWei()
            ) {
                showToast(R.string.gas_not_enough)
                return@setOnClickListener
            }
            verifyIdAndSend(amountTxt, sendAmountInSU)
        }
    }

    private fun verifyIdAndSend(amountTxt: String, sendAmountInSU: BigDecimal) {
        val dialogParams = BigDialog.Params(
            bigTitle = getString(R.string.pay),
            secondTitle = getString(R.string.receive_address),
            firstButtonTxt = getString(R.string.confirm_pay),
            tokenIcon = ethchainNormalTokenInfo.icon,
            tokenSymbol = ethchainNormalTokenInfo.symbol ?: "",
            secondValue = depositToAddress,
            amount = amountTxt.toBigDecimal().stripTrailingZeros().toPlainString(),
            feeAmount = gasCostEthString,
            feeTokenSymbol = "ETH"
        )
        identityVerify.verifyIdentity(
            dialogParams,
            {
                if (ethchainNormalTokenInfo.tokenCode == TokenInfoCenter.EthEthTokenCodes.tokenCode) {
                    ethVm.sendEth(
                        EthNet.EthSendParams(
                            toAddr = depositToAddress!!,
                            amount = sendAmountInSU.toBigInteger(),
                            gasPrice = nowGasPriceInWei,
                            gasLimit = ethchainNormalTokenInfo.getGasLimit(),
                            privateKey = currentAccount.nowEthPrivateKey() ?: "",
                            erc20ContractAddr = null
                        )
                    )
                } else {
                    ethVm.sendErc20Token(
                        EthNet.EthSendParams(
                            toAddr = depositToAddress!!,
                            amount = sendAmountInSU.toBigInteger(),
                            gasPrice = nowGasPriceInWei,
                            gasLimit = ethchainNormalTokenInfo.getGasLimit(),
                            privateKey = currentAccount.nowEthPrivateKey() ?: "",
                            erc20ContractAddr = ethchainNormalTokenInfo.tokenAddress!!
                        )
                    )
                }
            }, {})
    }


    private fun setupGas() {
        gasPriceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, gwei: Int, fromUser: Boolean) {
                refreshGasPrice(gwei)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        gasInfo.setOnClickListener {
            TextTitleNotifyDialog(this).apply {
                setMessage(getString(R.string.gas_info))
                setBottom(getString(R.string.button_ok))
                show()
            }
        }
    }


    private fun bindCrosschainVMObserver() {
        crosschainVM.metaInfoLd.observe(this, Observer {
            cycleProgressBar.visibility = it.progressVisible
            if (it.isError()) {
                it.networkState.showBaseErrorDialog(this) {
                    it.dismiss()
                    finish()
                }
                return@Observer
            }

            if (it.resp?.depositState != MetaInfoResp.OPEN) {
                it.resp?.showCheckDialog(this) {
                    it.dismiss()
                    this@DepositActivity.finish()
                }
            } else {
                crosschainVM.depositInfo(
                    viteNormalTokenInfo.tokenAddress!!,
                    viteAddress,
                    ethchainNormalTokenInfo.url!!
                )
            }
        })

        crosschainVM.depositInfoLd.observe(this, Observer {
            cycleProgressBar.visibility = it.progressVisible
            if (it.isError()) {
                it.networkState.showBaseErrorDialog(this) {
                    it.dismiss()
                }
                return@Observer
            }

            minimumDepositAmountInSU =
                it.resp?.minimumDepositAmount?.toBigDecimalOrNull() ?: return@Observer
            depositToAddress = it.resp?.depositAddress ?: return@Observer

            this.moneyAmountEditTxt.hint = getString(
                R.string.crosschain_deposit_amount_hint,
                ethchainNormalTokenInfo.amountTextWithSymbol(
                    minimumDepositAmountInSU!!.toBigInteger(),
                    3
                )
            )
        })
    }


    private fun bindEthVmObserver() {
        ethVm.gasPriceLd.observe(this, Observer {
            val priceGv =
                Convert.fromWei(it.toBigDecimal(), Convert.Unit.GWEI).toBigInteger().toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                gasPriceSeekBar.setProgress(priceGv, true)
            } else {
                gasPriceSeekBar.progress = priceGv
            }

            refreshGasPrice(priceGv)
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

        ethVm.sendTxLd.observe(this, Observer {
            it?.let { ethSendTransaction ->
                if (ethSendTransaction.transactionHash != null) {
                    LogoTitleNotifyDialog(this@DepositActivity).apply {
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
    }

    private fun refreshGasPrice(gwei: Int) {
        gasCostInGwei.text = "${gwei} Gwei"
        nowGasPriceInWei = Convert.toWei(gwei.toBigDecimal(), Convert.Unit.GWEI).toBigInteger()
        gasCostInEth =
            Convert.fromWei(
                ethchainNormalTokenInfo.getGasLimit().multiply(nowGasPriceInWei).toBigDecimal(),
                Convert.Unit.ETHER
            )
        gasCostEthString = gasCostInEth.toLocalReadableText(5)
        val priceString =
            gasCostInEth.multiply(
                ExternalPriceCenter.getPrice(TokenInfoCenter.EthEthTokenCodes.tokenCode)
            ).toCurrencyText(4)

        gasCost.text = "${gasCostEthString}ether ≈ $priceString"

        if (ethchainNormalTokenInfo.tokenCode == TokenInfoCenter.EthEthTokenCodes.tokenCode) {
            val gasCostInWei = Convert.toWei(gasCostInEth, Convert.Unit.ETHER)
            val balanceInWei = ethchainNormalTokenInfo.balance().toBigDecimal()
            val userInputInEth =
                moneyAmountEditTxt.editableText.toString().toBigDecimalOrNull() ?: return
            val userInputInWei = Convert.toWei(userInputInEth, Convert.Unit.ETHER)
            val maxSendAmountInWei = balanceInWei - gasCostInWei
            if (maxSendAmountInWei <= BigDecimal.ZERO) {
                moneyAmountEditTxt.setText("0")
            } else if (userInputInWei > maxSendAmountInWei) {
                moneyAmountEditTxt.setText(
                    Convert.fromWei(
                        maxSendAmountInWei,
                        Convert.Unit.ETHER
                    ).toLocalReadableText(8)
                )
            }
        }
    }


    private fun setupTopLayout() {
        infor.setOnClickListener {
            with(TextTitleNotifyDialog(this)) {
                setTitle(R.string.crosschain_about_deposit_title)
                setBottom(R.string.i_see)
                setMessage(
                    getString(
                        R.string.crosschain_deposit_what_is_by_vite,
                        ethchainNormalTokenInfo.symbol!!,
                        ethchainNormalTokenInfo.symbol!!
                    )
                )
                show()
            }
        }

        overchainChargeRecord.setOnClickListener {
            DepositRecordsActivity.show4Result(
                this,
                viteNormalTokenInfo.tokenAddress!!,
                viteAddress,
                ethchainNormalTokenInfo.url!!,
                1
            )
        }
        tokenWidget.setOnClickListener {
            overchainChargeRecord.performClick()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
    }


    private fun setupMoneyAmountEditTxt() {
        moneyAmountEditTxt.customRightText = getString(R.string.deposit_all)
        moneyAmountEditTxt.colorStr = "#007AFF"
        moneyAmountEditTxt.setOnRightTextClickListener {
            moneyAmountEditTxt.setText(getAvailableAllAmount())
        }

        if (ethchainNormalTokenInfo.decimal != 0) {
            moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789.")
        } else {
            moneyAmountEditTxt.keyListener = DigitsKeyListener.getInstance("0123456789")
        }

        moneyAmountEditTxt.addTextChangedListener(DecimalLimitTextWatcher(ethchainNormalTokenInfo.decimal!!))
        moneyAmountEditTxt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.toString()?.toBigDecimalOrNull()?.multiply(
                    ExternalPriceCenter.getPrice(ethchainNormalTokenInfo.tokenCode ?: "")
                )?.toCurrencyText(2)?.let {
                    transferValueTxt.text = it
                    transferValueTxt.visibility = View.VISIBLE
                } ?: kotlin.run {
                    transferValueTxt.visibility = View.GONE
                }

            }
        })
    }

    private fun getAvailableAllAmount(): String {
        return if (ethchainNormalTokenInfo.tokenCode == TokenInfoCenter.EthEthTokenCodes.tokenCode) {
            val gasCostInWei = Convert.toWei(gasCostInEth, Convert.Unit.ETHER)
            val balanceInWei = ethchainNormalTokenInfo.balance().toBigDecimal()
            val availableAllAmount = balanceInWei - gasCostInWei
            if (availableAllAmount < BigDecimal.ZERO) {
                "0"
            } else {
                Convert.fromWei(availableAllAmount, Convert.Unit.ETHER).toLocalReadableText(8)
            }
        } else {
            ethchainNormalTokenInfo.balanceText(8)
        }
    }


    override fun onStart() {
        super.onStart()
        crosschainVM.getOverchainMetaInfo(
            ethchainNormalTokenInfo.url!!,
            viteNormalTokenInfo.tokenAddress!!
        )
    }
}