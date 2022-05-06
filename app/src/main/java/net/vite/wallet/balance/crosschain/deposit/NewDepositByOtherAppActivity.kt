package net.vite.wallet.balance.crosschain.deposit

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import android.view.View
import com.google.zxing.client.android.Utils
import kotlinx.android.synthetic.main.activity_new_crosschain_charge_other.*
import net.vite.wallet.R
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.balance.crosschain.addMappedToken
import net.vite.wallet.balance.crosschain.deposit.list.DepositRecordsActivity
import net.vite.wallet.balance.crosschain.setMappedTokenSelected
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.network.http.gw.*
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.utils.dp2px
import net.vite.wallet.utils.showDialogMsg
import net.vite.wallet.utils.showToast


class NewDepositByOtherAppActivity : UnchangableAccountAwareActivity() {

    private val viteAddress by lazy {
        intent.getStringExtra("viteAddress")!!
    }

    private var firstEnterFlag: Boolean = false

    private var currentGwNormalTokenInfo: NormalTokenInfo = NormalTokenInfo.EMPTY
        set(value) {
            field = value
            chooseMainNetContentContainer.setMappedTokenSelected(value)
            if (value.family() == TokenFamily.ETH) {
                depositByVite.visibility = View.VISIBLE
                depositByVite.setOnClickListener {
                    DepositActivity.show(
                        this,
                        value.tokenCode!!,
                        vitechainNormalTokenInfo.tokenCode!!,
                        viteAddress
                    )
                }
            } else {
                depositByVite.visibility = View.GONE
            }
        }

    private val tokenAddress by lazy {
        intent.getStringExtra("tokenAddress")!!
    }

    private val vitechainNormalTokenInfo by lazy {
        TokenInfoCenter.findTokenInfo { it.tokenAddress == tokenAddress }!!
    }

    private val allMappedTokenInfos by lazy {
        vitechainNormalTokenInfo.allMappedToken()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstEnterFlag = true
        setContentView(R.layout.activity_new_crosschain_charge_other)

        currentGwNormalTokenInfo = vitechainNormalTokenInfo.gatewayInfo?.mappedToken ?: run {
            finish()
            return
        }

        overchainChargeRecord.setOnClickListener {
            DepositRecordsActivity.show(
                this,
                vitechainNormalTokenInfo.tokenAddress!!,
                viteAddress,
                currentGwNormalTokenInfo.url!!
            )
        }

        tokenWidget.setOnClickListener {
            overchainChargeRecord.performClick()
        }

        allMappedTokenInfos.forEach { tokenInfo ->
            chooseMainNetContentContainer.addMappedToken(tokenInfo) { selectedTokenInfo ->
                checkGWAvailableThenRefresh(selectedTokenInfo)
            }
        }

    }

    override fun onStart() {
        super.onStart()
        checkGWAvailableThenRefresh(currentGwNormalTokenInfo, true)
    }

    private fun onFirstEnter(labelName: String?) {
        val message: String = getString(R.string.crosschain_deposit_labelDesc, labelName)
        with(TextTitleNotifyDialog(this)) {
            setTitle(R.string.warm_notice)
            setMessage(message)
            setBottomLeft(R.string.i_see) { dialog ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun checkGWAvailableThenRefresh(
        selectedTokenInfo: NormalTokenInfo,
        forceRefresh: Boolean = false
    ) {
        if (selectedTokenInfo == currentGwNormalTokenInfo && !forceRefresh) return
        checkGWAvailable(selectedTokenInfo) { depositInfoResp ->
            currentGwNormalTokenInfo = selectedTokenInfo

            textView10Label.text = getString(
                R.string.crosschain_charge_scan_qrcode_desc,
                depositInfoResp.labelName.toString()
            )
            chargeAddressTagLabel.text = depositInfoResp.labelName.toString()

            depositInfoResp.minimumDepositAmount?.let {
                val spannableBuilder = SpannableStringBuilder()
                val tokenSymbol =
                    vitechainNormalTokenInfo.symbol!! + "(${currentGwNormalTokenInfo.standard()})"
                val minText =
                    currentGwNormalTokenInfo.amountText(it.toBigInteger(), 4) + tokenSymbol
                val originStr = getString(
                    R.string.crosschain_deposit_byother_hint,
                    minText,
                    tokenSymbol
                )
                val startSymbol = originStr.indexOf(tokenSymbol)
                val endSymbol = startSymbol + tokenSymbol.length
                spannableBuilder.append(originStr)
                    .setSpan(
                        TextAppearanceSpan(this, R.style.Text_highlight),
                        startSymbol,
                        endSymbol,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )

                run {
                    val start = originStr.indexOf(minText)
                    val end = start + minText.length
                    spannableBuilder.setSpan(
                        TextAppearanceSpan(this, R.style.Text_highlight),
                        start,
                        end,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
                run {
                    val start = originStr.lastIndexOf(minText)
                    val end = start + minText.length
                    spannableBuilder.setSpan(
                        TextAppearanceSpan(this, R.style.Text_highlight),
                        start,
                        end,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
                hintText.text = spannableBuilder
            }

            depositInfoResp.confirmationCount?.toString()?.let {
                hintText2.visibility = View.VISIBLE
                val spannableBuilder = SpannableStringBuilder()
                val originStr = getString(
                    R.string.crosschain_deposit_confirm_hint,
                    it
                )

                val start = originStr.indexOf(it)
                val end = start + it.length

                spannableBuilder.append(originStr)
                    .setSpan(
                        TextAppearanceSpan(this, R.style.Text_highlight),
                        start,
                        end,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )

                hintText2.text = spannableBuilder
            } ?: run {
                hintText2.visibility = View.GONE
            }

            if (depositInfoResp.needLabel()) {
                descContainer.visibility = View.VISIBLE

                depositInfoResp.label?.let {
                    chargeTargetAddressLabel.text = it

                    Utils.creteQrCodeImage(
                        it,
                        170.0F.dp2px().toInt(),
                        170.0F.dp2px().toInt()
                    )?.let {
                        qrImgLabel.setImageBitmap(it)
                    }

                    pasteAddrLabel.setOnClickListener {
                        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.let { cm ->
                            cm.setPrimaryClip(
                                ClipData.newPlainText(
                                    null,
                                    chargeTargetAddressLabel.text
                                )
                            )

                            showToast(R.string.copy_success)
                        }
                    }

                    logoImgLabel.setup(currentGwNormalTokenInfo.icon)
                }

                if (firstEnterFlag) {
                    onFirstEnter(depositInfoResp.labelName)
                    firstEnterFlag = false
                }
            }

            depositInfoResp.depositAddress?.let {
                chargeTargetAddress.text = it

                Utils.creteQrCodeImage(
                    it,
                    170.0F.dp2px().toInt(),
                    170.0F.dp2px().toInt()
                )?.let {
                    qrImg.setImageBitmap(it)
                }

                pasteAddr.setOnClickListener {
                    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.let { cm ->
                        cm.setPrimaryClip(ClipData.newPlainText(null, chargeTargetAddress.text))
                        showToast(R.string.copy_success)
                    }
                }

                logoImg.setup(currentGwNormalTokenInfo.icon)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun checkGWAvailable(
        selectedTokenInfo: NormalTokenInfo,
        onSuccess: (DepositInfoResp) -> Unit
    ) {
        cycleProgressBar.visibility = View.VISIBLE
        GwCrosschainApi.metaInfo(
            vitechainNormalTokenInfo.tokenAddress!!,
            selectedTokenInfo.url!!
        ).subscribe(
            {
                if (it.depositState != MetaInfoResp.OPEN) {
                    it.showCheckDialog(this) {
                        it.dismiss()
                    }
                    return@subscribe
                }

                GwCrosschainApi.depositInfo(
                    vitechainNormalTokenInfo.tokenAddress!!,
                    currentAccount.nowViteAddress()!!,
                    selectedTokenInfo.url
                ).subscribe({
                    cycleProgressBar.visibility = View.GONE
                    onSuccess(it)
                }, errorHandler)
            },
            errorHandler
        )
    }

    private val errorHandler: (Throwable) -> Unit = {
        cycleProgressBar.visibility = View.GONE
        if (it is OverChainApiException) {
            it.showErrorDialog(this)
        } else {
            it.showDialogMsg(this)
        }
    }

    companion object {
        fun show(context: Context, tokenAddress: String, viteAddress: String) {
            context.startActivity(Intent(context, NewDepositByOtherAppActivity::class.java).apply {
                putExtra("tokenAddress", tokenAddress)
                putExtra("viteAddress", viteAddress)
            })
        }
    }
}