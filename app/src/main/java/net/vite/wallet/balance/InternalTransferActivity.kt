package net.vite.wallet.balance

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_internal_transfer.*
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.TxEndStatus
import net.vite.wallet.TxSendSuccess
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.balance.tokenselect.LocalTokenSelectActivity
import net.vite.wallet.constants.DexContractAddress
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.rpc.BuildInContractEncoder
import net.vite.wallet.network.rpc.GetAccountFundInfoResp
import net.vite.wallet.network.rpc.NormalTxParams
import net.vite.wallet.network.toLocalReadableText
import net.vite.wallet.utils.hasNetWork
import net.vite.wallet.utils.showToast
import net.vite.wallet.widget.DecimalLimitTextWatcher
import java.math.BigDecimal
import java.math.BigInteger

class InternalTransferActivity : BaseTxSendActivity() {

    companion object {
        fun show(context: Context, tokenAddress: String, fromWallet: Boolean = true) {
            context.startActivity(
                Intent(context, InternalTransferActivity::class.java).apply {
                    putExtra("tokenAddress", tokenAddress)
                    putExtra("fromWallet", fromWallet)
                })
        }
    }

    lateinit var balanceViewModel: BalanceViewModel
    private lateinit var normalTokenInfo: NormalTokenInfo
    private var accountFundInfo: GetAccountFundInfoResp? = null
    private var isDexPost = true

    private var lockAnimation = false

    private var disposable: Disposable? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internal_transfer)
        val tokenAddress = intent.getStringExtra("tokenAddress")
        tokenAddress?.let { loadNewToken(it) }

        Thread {
            Thread.sleep(200)
            runOnUiThread {
                if (!intent.getBooleanExtra("fromWallet", true)) {
                    switchBtn.performClick()
                }
            }
        }.start()
    }

    private fun loadNewToken(tokenAddress: String) {
        progressCycle.visibility = View.VISIBLE
        disposable = TokenInfoCenter.queryViteToken(tokenAddress)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                progressCycle.visibility = View.GONE
                normalTokenInfo = it

                tokenSymbol.text = normalTokenInfo.uniqueName()

                val options = RequestOptions.circleCropTransform()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                Glide.with(this).load(it.icon).apply(options).into(itTokenIcon)

                accountFundInfo =
                    DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                        ?.get(normalTokenInfo.tokenAddress)
                setAvailableBalanceTxt()

                moneyAmountEditTxt.addTextChangedListener(DecimalLimitTextWatcher(normalTokenInfo.decimal!!))
                moneyAmountEditTxt.customRightText = normalTokenInfo.symbol ?: kotlin.run {
                    finish()
                    return@subscribe
                }
                switchBtn.setOnClickListener {
                    if (lockAnimation) {
                        return@setOnClickListener
                    }
                    val exchangeY = exchangeAccountTxt.y
                    val walletY = walletAccountTxt.y
                    val translationY = walletY - exchangeY

                    lockAnimation = true

                    val offset = if (isDexPost) {
                        translationY
                    } else {
                        0.0F
                    }
                    val animationTime = 400L
                    ObjectAnimator.ofFloat(
                        exchangeAccountTxt, "translationY", offset
                    ).apply {
                        duration = animationTime
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                lockAnimation = false
                            }
                        })
                        start()
                    }
                    ObjectAnimator.ofFloat(
                        walletAccountTxt, "translationY", -offset
                    ).apply {
                        duration = animationTime
                        start()
                    }

                    isDexPost = !isDexPost
                    setAvailableBalanceTxt()

                }
                moneyAllTxt.setOnClickListener {
                    moneyAmountEditTxt.setText(getCurrentAllBalance())
                }

                transferBtn.setOnClickListener {
                    doSend()
                }

                changeTokenSymbol.setOnClickListener {
                    startActivityForResult(
                        Intent(
                            this,
                            LocalTokenSelectActivity::class.java
                        ).apply {
                            putExtra(LocalTokenSelectActivity.FROM_WALLET, isDexPost)
                        }, 14351
                    )
                }

                balanceViewModel = ViewModelProviders.of(this)[BalanceViewModel::class.java]

                balanceViewModel.dexAccountFundInfo.observe(this, Observer {
                    accountFundInfo = DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
                        ?.get(normalTokenInfo.tokenAddress)
                    setAvailableBalanceTxt()
                })

                balanceViewModel.rtViteAccInfo.observe(this, Observer {
                    setAvailableBalanceTxt()
                })
            }, {
                progressCycle.visibility = View.GONE
                showToast("empty token info")
                finish()
            })
    }

    @SuppressLint("SetTextI18n")
    private fun setAvailableBalanceTxt() {
        availableBalanceTxt.text =
            "${getString(R.string.available)}${getCurrentAllBalance()} ${normalTokenInfo.symbol}"
    }

    override fun onStop() {
        super.onStop()
        disposable?.dispose()
        disposable = null
    }

    private fun doSend() {
        if (!hasNetWork()) {
            showToast(R.string.net_work_error)
            return
        }

        val amountTxt = moneyAmountEditTxt.editableText.toString()

        if (amountTxt.isEmpty()) {
            moneyAmountEditTxt.requestFocus()
            showToast(R.string.please_input_correct_amount)
            return
        }


        val sendAmountInBase = amountTxt.toBigDecimalOrNull() ?: kotlin.run {
            moneyAmountEditTxt.requestFocus()
            showToast(R.string.please_input_correct_amount)
            return
        }

        val sendAmountInSu = normalTokenInfo.baseToSmallestUnit(sendAmountInBase)

        if (isDexPost) {
            if (sendAmountInSu > normalTokenInfo.balance().toBigDecimal()) {
                moneyAmountEditTxt.requestFocus()
                showToast(R.string.balance_not_enough)
                return
            }
            if (normalTokenInfo.balance() == BigInteger.ZERO) {
                showToast(R.string.balance_not_enough)
                return
            }

            lastSendParams = NormalTxParams(
                toAddr = DexContractAddress,
                amountInSu = sendAmountInSu.toBigInteger(),
                data = BuildInContractEncoder.dexPost(),
                tokenId = normalTokenInfo.tokenAddress!!,
                accountAddr = currentAccount.nowViteAddress()!!
            )

            verifyIdentityDirectlyFinger(getString(R.string.transferTitle), {
                lastSendParams?.let { sendTx(it) }
            }, {})
        } else {
            val maxValue = accountFundInfo?.available?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (sendAmountInSu > maxValue) {
                moneyAmountEditTxt.requestFocus()
                showToast(R.string.balance_not_enough)
                return
            }
            if (maxValue == BigDecimal.ZERO) {
                showToast(R.string.exchange_balance_not_enough)
                return
            }

            lastSendParams = NormalTxParams(
                toAddr = DexContractAddress,
                data = BuildInContractEncoder.dexDexwithdraw(
                    normalTokenInfo.tokenAddress!!,
                    sendAmountInSu.toBigInteger()
                ),
                tokenId = normalTokenInfo.tokenAddress!!,
                accountAddr = currentAccount.nowViteAddress()!!,
                amountInSu = BigInteger.ZERO
            )

            verifyIdentityDirectlyFinger(
                getString(R.string.withdraw_to_wallet),
                { lastSendParams?.let { sendTx(it) } },
                {})

        }
    }

    private fun getCurrentAllBalance() =
        if (isDexPost) {
            normalTokenInfo.balanceText(8)
        } else {
            accountFundInfo?.getAvailableBase()?.toLocalReadableText(8) ?: "0"
        }

    override fun onTxEnd(status: TxEndStatus) {
        when (status) {
            is TxSendSuccess -> {
                baseTxSendFlow.handleTxEndStatus(status) {
                    finish()
                }
            }
            else -> {
                super.onTxEnd(status)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val tokenAddress = data?.getStringExtra("tokenAddress")
        tokenAddress?.let {
            loadNewToken(tokenAddress)
        }
    }
}
