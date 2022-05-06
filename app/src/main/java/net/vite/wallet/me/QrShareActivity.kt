package net.vite.wallet.me

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.zxing.client.android.Utils
import kotlinx.android.synthetic.main.activity_qrcode.*
import net.vite.wallet.EncodeToUrlParams
import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.encodeToUrl
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.utils.base64UrlSafeEncode
import net.vite.wallet.utils.dp2px
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal
import kotlin.math.min

class QrShareActivity : UnchangableAccountAwareActivity() {
    companion object {
        fun show(context: Context, address: String, normalTokenInfo: NormalTokenInfo) {
            context.startActivity(Intent(context, QrShareActivity::class.java).apply {
                putExtra("normalTokenInfo", Gson().toJson(normalTokenInfo))
                putExtra("address", address)
            })
        }
    }

    private fun getMaxIndexStr(s: String): String {
        for (i in 0..s.length) {
            val ss = s.substring(0, i)
            if (base64UrlSafeEncode(ss.toByteArray()).length <= 120) {
                return ss
            }
        }
        return ""
    }

    private var isInEthCommunity = false


    fun createAndSetQrImg(params: EncodeToUrlParams) {
        Utils.creteQrCodeImage(
            encodeToUrl(params, isInEthCommunity),
            170.0F.dp2px().toInt(),
            170.0F.dp2px().toInt()
        )?.let {
            qrImg.setImageBitmap(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)

        currentAccount = AccountCenter.getCurrentAccount() ?: run {
            finish()
            return
        }

        val tokenInfo = Gson().fromJson(intent.getStringExtra("normalTokenInfo"), NormalTokenInfo::class.java)
        val accountAddress = intent.getStringExtra("address")!!
        walletName.text = currentAccount.name

        logoImg.setup(tokenInfo.icon)

        if (tokenInfo.family() == TokenFamily.VITE) {
            rootContainer.setBackgroundResource(R.drawable.widget_balance_detail_vite_bg)
            val name = currentAccount.getAddressName(accountAddress)
            addressTxt.text = "$name\n\n$accountAddress"
            remarkEditTxt.visibility = View.VISIBLE
        }

        if (tokenInfo.family() == TokenFamily.ETH) {
            rootContainer.setBackgroundResource(R.drawable.widget_balance_detail_eth_bg)
            addressTxt.text = accountAddress
            remarkEditTxt.visibility = View.GONE
        }


        val urlParams = EncodeToUrlParams(
            family = tokenInfo.family(),
            accountAddress = accountAddress,
            tokenAddress = tokenInfo.tokenAddress,
            decimal = tokenInfo.decimal
        )
        createAndSetQrImg(urlParams)


        remarkEditTxt.filters = arrayOf(
            InputFilter { source, start, end, dest, dstart, dend ->
                val desCount = base64UrlSafeEncode(dest.toString().toByteArray()).length
                val sourceCount = base64UrlSafeEncode(source.toString().toByteArray()).length
                if (desCount > 120) {
                    Toast.makeText(this, R.string.remark_text_too_long, Toast.LENGTH_SHORT).show()
                    ""
                } else {
                    val count = desCount + sourceCount
                    if (count <= 120) {
                        null
                    } else {
                        Toast.makeText(this, R.string.remark_text_too_long, Toast.LENGTH_SHORT).show()
                        getMaxIndexStr(source.toString())
                    }
                }
            }
        )

        remarkEditTxt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s == null) {
                    urlParams.dataStr = null
                } else {
                    urlParams.dataStr = base64UrlSafeEncode(s.toString().toByteArray())
                }

                if (urlParams.dataStr?.toByteArray()?.size ?: 0 > 120) {
                    Toast.makeText(this@QrShareActivity, R.string.remark_text_too_long, Toast.LENGTH_SHORT).show()
                    return
                }

                createAndSetQrImg(urlParams)
            }

        })

        shareBtn.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                shareImg()
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET
                    ), 1
                )
            }
        }

        copyAddrBtn.setOnClickListener {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.let { cm ->
                cm.setPrimaryClip(ClipData.newPlainText(null, accountAddress))
                Toast.makeText(this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
            }
        }

        givenAmount.text = getString(R.string.please_send_me_this, tokenInfo.symbol ?: "")

        assignMoney.setOnClickListener {
            BigDialog(this).apply {
                setBigTitle(
                    getString(R.string.input_money_amount)
                )
                textInput.visibility = View.VISIBLE
                textInput.inputType = EditorInfo.TYPE_NUMBER_FLAG_DECIMAL
                inputLayout.visibility = View.VISIBLE
                inputLayout.hint = getString(R.string.money_amount)
                if (tokenInfo.decimal!!.toInt() == 0) {
                    textInput.keyListener = DigitsKeyListener.getInstance("0123456789")
                } else {
                    textInput.keyListener = DigitsKeyListener.getInstance("0123456789.")
                }
                inputLayout.isPasswordVisibilityToggleEnabled = false


                setFirstButton(getString(R.string.confirm)) {
                    val amountTxt = textInput.editableText.toString()
                    val amountBigDecimal = amountTxt.toBigDecimalOrNull()
                    if (amountBigDecimal == null) {
                        Toast.makeText(
                            this@QrShareActivity,
                            R.string.please_input_correct_amount,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setFirstButton
                    }

                    val decimalsLength = if (amountTxt.indexOf(".") != -1) {
                        amountTxt.length - amountTxt.indexOf(".") - 1
                    } else {
                        0
                    }

                    if (decimalsLength > min(8, tokenInfo.decimal)) {
                        Toast.makeText(
                            this@QrShareActivity, getString(
                                R.string.this_token_max_input_number_decimal,
                                tokenInfo.uniqueName(),
                                min(8, tokenInfo.decimal)
                            ), Toast.LENGTH_SHORT
                        ).show()
                        return@setFirstButton
                    }

                    if (tokenInfo.family() == TokenFamily.ETH) {
                        urlParams.amount =
                            amountTxt.toBigDecimalOrNull()?.multiply(
                                BigDecimal.TEN.pow(
                                    tokenInfo.decimal ?: 0
                                )
                            )?.toBigInteger()?.toString()

                    } else {
                        urlParams.amount = amountTxt
                    }


                    dismiss()
                    createAndSetQrImg(urlParams)
                    this@QrShareActivity.givenAmount.text = "$amountTxt ${tokenInfo.symbol}"
                }
                show()
            }
        }

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                shareImg()
            }
        }
    }

    fun shareImg() {
        try {
            val imgDirPath = ViteConfig.get().viteExternalRootPath + "/qrShareImg"
            File(imgDirPath).mkdirs()
            val imgName = "${AccountCenter.currentViteAddress() ?: "0"}.png"
            val f = File(imgDirPath, imgName)
            val b = getBitmap()
            val bs = ByteArrayOutputStream()
            b.compress(Bitmap.CompressFormat.PNG, 100, bs)
            f.writeBytes(bs.toByteArray())
            bs.close()
            val i = Intent(Intent.ACTION_SEND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(
                        this@QrShareActivity,
                        "$packageName.fileprovider", f
                    )
                )
                type = "image/png"
            }
            startActivity(Intent.createChooser(i, getString(R.string.share_qr_code)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getBitmap(): Bitmap {
        val ov = assignMoney.visibility
        backIcon.visibility = View.GONE
        shareBtn.visibility = View.GONE
        assignMoney.visibility = View.GONE

        val b = Bitmap.createBitmap(rootContainer.width, rootContainer.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        rootContainer.background.draw(c)
        rootContainer.draw(c)

        backIcon.visibility = View.VISIBLE
        shareBtn.visibility = View.VISIBLE
        assignMoney.visibility = ov
        return b
    }

}