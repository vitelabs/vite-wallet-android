package net.vite.wallet.vitebridge

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.Keep
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_h5_web.*
import net.vite.wallet.*
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.balance.InternalTransferActivity
import net.vite.wallet.balance.quota.FastPledgeQuotaFlow
import net.vite.wallet.balance.quota.PledgeQuotaActivity
import net.vite.wallet.balance.walletconnect.taskdetail.toNormalTxParams
import net.vite.wallet.constants.DexCancelContractAddress
import net.vite.wallet.constants.DexContractAddress
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.exchange.wiget.SwitchExchangeFragment
import net.vite.wallet.network.rpc.AccountBlock
import net.vite.wallet.network.rpc.NormalTxParams
import net.vite.wallet.network.rpc.RpcException
import net.vite.wallet.utils.*
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.vep.decodeViteTransferUrl
import org.json.JSONObject
import org.vitelabs.mobile.Mobile
import org.walletconnect.Session
import wendu.dsbridge.CompletionHandler
import wendu.dsbridge.DWebView
import java.math.BigDecimal

@Keep
class H5WebActivity : BaseTxSendActivity() {
    companion object {
        const val QuickPledgeQuotaRequestCode = 111
        const val SendTxByUrlRequestCode = 112
        const val SendTxRequestCode = 113
        const val ScanQrcodeDefaultRequest = 6974

        internal fun show(context: Context, url: String) {
            logi("show H5WebActivity with url $url")
            context.startActivity(Intent(context, H5WebActivity::class.java).apply {
                putExtra("url", url)
            })
        }
    }


    private var lastSendTxByUriHandler: CompletionHandler<JsBridgeResp<in AccountBlock>>? = null
    private var lastSendTxHandler: CompletionHandler<JsBridgeResp<in AccountBlock>>? = null
    private var lastFastPledgeHandler: CompletionHandler<JsBridgeResp<Any>>? = null
    private var lastSwitchMarketHandler: CompletionHandler<JsBridgeResp<Any>>? = null
    private var lastScanHandler: CompletionHandler<JsBridgeResp<JsonObject>>? = null

    private var mWebView: DWebView? = null
    private var downloadUrl: String? = null

    private val fastPledgeQuotaFlow = FastPledgeQuotaFlow(this)


    override fun onBackPressed() {
        if (h5Web.canGoBack()) {
            h5Web.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_h5_web)
        if (savedInstanceState != null) {
            (applicationContext as ViteApplication).restartApp()
            return
        }
        closeText.setOnClickListener {
            finish()
        }

        mWebView = h5Web

        DWebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        h5Web.addJavascriptObject(WalletJsApi(), "wallet")
        h5Web.addJavascriptObject(BridgeApi(), "bridge")
        h5Web.addJavascriptObject(AppApi(), "app")

        val rawUa = h5Web.getSettings().userAgentString
        val appInfo = AppInfo.nowAppInfo(this)
        h5Web.settings.userAgentString =
            "$rawUa Vite/${JsBridgeVersion.NowVersion.versionCode}/Wallet/${appInfo.versionCode}/en}"


        h5Web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                this@H5WebActivity.rrTextBtn.visibility = View.GONE
                initShareBtn()
                if (WhiteHostManager.isWhiteHost(url ?: "") || BuildConfig.DEBUG) {
                    h5Web.addJavascriptObject(PrivateApi(), "pri")
                } else {
                    h5Web.removeJavascriptObject("pri")
                }
            }
        }
        h5Web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    loadProgress.visibility = View.GONE
                } else {
                    loadProgress.visibility = View.VISIBLE
                    loadProgress.progress = newProgress
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let {
                    h5Title.text = title
                }
            }
        }

        h5Web.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                download(url)
            } else {
                downloadUrl = url
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
            }
        }

        val url = intent.getStringExtra("url")!!
        h5Web.loadUrl(url)

        refreshBtn.setOnClickListener {
            h5Web.clear()
//            h5Web.reload()
            h5Web.loadUrl(url)
        }

        rrTextBtn.setOnClickListener { onRRClickListener() }
        rrImgBtn.setOnClickListener { onRRClickListener() }

        fastPledgeQuotaFlow.onCreate()
        initShareBtn()
    }


    private fun download(url: String?) {
        try {
            val request = DownloadManager.Request(
                Uri.parse(url)
            )
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "vite")
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, R.string.downloading, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {

        }
    }


    fun onRRClickListener() {
        h5Web.callHandler("nav.RRBtnClick", null)
    }


    override fun onStart() {
        super.onStart()
        try {
            mWebView!!.callHandler("page.onShow", null)
        } catch (e: Exception) {
        }
    }

    private fun initShareBtn() {
        rrImgBtn.visibility = View.VISIBLE
        rrImgBtn.setImageResource(R.mipmap.share_h5)
        rrImgBtn.setOnClickListener {
            val url = mWebView?.url ?: return@setOnClickListener
            startActivity(Intent.createChooser(createBrowserIntent(url), getString(R.string.share)))
        }
    }

    private fun setRRBase64Img(rrImgBase64: String) {
        val plainBase64 = rrImgBase64.removePrefix("data:image/png;base64,")
        val imgBytes = Base64.decode(plainBase64, Base64.NO_WRAP)
        val bitMap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)

        rrTextBtn.visibility = View.GONE
        rrImgBtn.visibility = View.VISIBLE
        rrImgBtn.setOnClickListener { this@H5WebActivity.onRRClickListener() }

        rrImgBtn.setImageBitmap(bitMap)
    }

    fun onExchangeMarketItemSwitched(symbol: String) {
        lastSwitchMarketHandler?.complete(JsBridgeResp(data = mapOf("symbol" to symbol)))
        lastSwitchMarketHandler = null
        kotlin.runCatching {
            supportFragmentManager?.popBackStackImmediate()
        }

    }

    override fun onTxEnd(status: TxEndStatus) {
        if (status.requestCode == QuickPledgeQuotaRequestCode) {
            fastPledgeQuotaFlowOnTxEnd(status)
            return
        }

        val resultHandler =
            if (status.requestCode == SendTxByUrlRequestCode) {
                lastSendTxByUriHandler
            } else {
                lastSendTxHandler
            }

        when (status) {
            is TxSendSuccess -> {
                status.powProfile?.let {
                    showPowProfileDialog(it)
                } ?: kotlin.run {
                    Toast.makeText(this, R.string.tx_send_success, Toast.LENGTH_SHORT).show()
                }
                resultHandler?.complete(JsBridgeResp(status.accountBlock, 0, ""))
            }
            is TxSendFailed -> {
                status.throwable.showToast(this)
                if (status.throwable is RpcException) {
                    resultHandler?.complete(
                        JsBridgeResp(
                            null,
                            status.throwable.code,
                            status.throwable.shownMessage(this)
                        )
                    )
                } else {
                    resultHandler?.complete(JsBridgeResp.NetWorkError)
                }
            }
            is TxSendCancel -> {
                resultHandler?.complete(JsBridgeResp(null, 4004, "user cancel ${status.where}"))
            }
        }

        if (status.requestCode == SendTxByUrlRequestCode) {
            lastSendTxByUriHandler = null
        } else {
            lastSendTxHandler = null
        }
    }

    @Keep
    inner class BridgeApi {
        @JavascriptInterface
        fun version(o: Any, completionHandler: CompletionHandler<JsBridgeResp<JsBridgeVersion>>) {
            completionHandler.complete(JsBridgeResp(JsBridgeVersion.NowVersion))
        }
    }

    @Keep
    inner class AppApi {
        @JavascriptInterface
        fun scan(o: Any, completionHandler: CompletionHandler<JsBridgeResp<JsonObject>>) {
            if (lastScanHandler != null) {
                completionHandler.complete(JsBridgeResp(null, 4001, "last scan not complete"))
                return
            }
            lastScanHandler = completionHandler
            runOnUiThread {
                scanQrcode(ScanQrcodeDefaultRequest)
            }
        }


        @JavascriptInterface
        fun info(o: Any, completionHandler: CompletionHandler<JsBridgeResp<AppInfo>>) {
            completionHandler.complete(JsBridgeResp(AppInfo.nowAppInfo(this@H5WebActivity)))
        }

        @JavascriptInterface
        fun language(o: Any, completionHandler: CompletionHandler<JsBridgeResp<String>>) {
            completionHandler.complete(
                JsBridgeResp(
                    if (this@H5WebActivity.isChinese()) {
                        "zh"
                    } else {
                        "en"
                    }
                )
            )
        }

        @JavascriptInterface
        fun setWebTitle(setTitleReq: Any, completionHandler: CompletionHandler<JsBridgeResp<Any>>) {
            if (setTitleReq !is JSONObject) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }
            val title = setTitleReq.get("title")
            if (title !is String) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }
            runOnUiThread {
                this@H5WebActivity.h5Title.text = title
            }
            completionHandler.complete(JsBridgeResp(""))
        }


        @JavascriptInterface
        fun setRRButton(o: Any, completionHandler: CompletionHandler<JsBridgeResp<Any>>) {
            if (o !is JSONObject) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }
            completionHandler.complete(JsBridgeResp.SuccessResp)

            runOnUiThread {
                try {
                    val rrText = o.getString("title")
                    if (!rrText.isNullOrEmpty()) {
                        this@H5WebActivity.rrTextBtn.visibility = View.VISIBLE
                        this@H5WebActivity.rrImgBtn.visibility = View.GONE
                        this@H5WebActivity.rrTextBtn.text = rrText
                        return@runOnUiThread
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val rrImgBase64 = o.getString("img")
                    if (!rrImgBase64.isNullOrEmpty()) {
                        setRRBase64Img(rrImgBase64)
                        return@runOnUiThread
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

        @JavascriptInterface
        fun share(shardReq: Any, completionHandler: CompletionHandler<JsBridgeResp<Any>>) {
            if (shardReq !is JSONObject) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }
            val url = shardReq.get("url")
            if (url !is String) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }
            runOnUiThread {
                this@H5WebActivity.startActivity(
                    Intent.createChooser(
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, url)
                            type = "text/plain"
                        },
                        getString(R.string.share)
                    )
                )
            }

            completionHandler.complete(JsBridgeResp(""))
        }
    }

    @Keep
    inner class WalletJsApi {
        @JavascriptInterface
        fun currentAddress(o: Any, completionHandler: CompletionHandler<JsBridgeResp<in String>>) {
            AccountCenter.currentViteAddress()?.let {
                completionHandler.complete(JsBridgeResp(it))
            } ?: completionHandler.complete(JsBridgeResp.LoginError)
        }


        @JavascriptInterface
        fun sendTxByURI(
            uriReq: Any,
            completionHandler: CompletionHandler<JsBridgeResp<in AccountBlock>>
        ) {
            if (lastSendTxByUriHandler != null) {
                completionHandler.complete(JsBridgeResp(null, 4001, "last tx not complete"))
                return
            }

            if (uriReq !is JSONObject) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }

            val address = uriReq.get("address")
            if (address !is String) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }
            if (address != AccountCenter.currentViteAddress()) {
                completionHandler.complete(JsBridgeResp.AddressNotMatch)
                return
            }

            val uri = uriReq.get("uri")
            if (uri !is String) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }

            val params = try {
                decodeViteTransferUrl(uri)
            } catch (e: Exception) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }

            if (params.toAddr == DexCancelContractAddress || params.toAddr == DexContractAddress) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }

            val tokenId = params.tti
            val amount = params.amount?.toBigDecimal() ?: BigDecimal.ZERO
            val data = params.data?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

            lastSendTxByUriHandler = completionHandler

            runOnUiThread {
                val nowAddr = AccountCenter.currentViteAddress() ?: kotlin.run {
                    lastSendTxByUriHandler?.complete(JsBridgeResp.LoginError)
                    lastSendTxByUriHandler = null
                    return@runOnUiThread
                }
                val tokenInfo =
                    TokenInfoCenter.getTokenInfoIncacheByTokenAddr(tokenId) ?: kotlin.run {
                        lastSendTxByUriHandler?.complete(
                            JsBridgeResp(
                                null,
                                4002,
                                "not found the tokenInfo which corresponding to the tokenId"
                            )
                        )
                        lastSendTxByUriHandler = null
                        return@runOnUiThread
                    }
                val amountInMin = amount.multiply(BigDecimal.TEN.pow(tokenInfo.decimal ?: 0))
                    .stripTrailingZeros().toPlainString()

                if (amountInMin.contains('.')) {
                    lastSendTxByUriHandler?.complete(
                        JsBridgeResp(
                            null,
                            4003,
                            "amount`s decimal error"
                        )
                    )
                    lastSendTxByUriHandler = null
                    return@runOnUiThread
                }

                lastSendParams = NormalTxParams(
                    accountAddr = nowAddr,
                    toAddr = params.toAddr,
                    tokenId = params.tti,
                    amountInSu = amountInMin.toBigInteger(),
                    data = data
                )

                val bigTitle = getString(
                    if (params.isFunctionCall()) {
                        R.string.contract_tx_title
                    } else {
                        R.string.pay
                    }
                )

                val secondTitle = getString(
                    if (params.isFunctionCall()) {
                        R.string.contract_address
                    } else {
                        R.string.receive_address
                    }
                )

                val dialogParams = BigDialog.Params(
                    firstButtonTxt = getString(R.string.confirm),
                    bigTitle = bigTitle,
                    secondTitle = secondTitle,
                    secondValue = params.toAddr,
                    amount = params.amount,
                    tokenSymbol = tokenInfo.symbol ?: ""
                )

                verifyIdentity(dialogParams, {
                    lastSendParams?.let {
                        sendTx(it, SendTxByUrlRequestCode)
                    }
                }, {
                    lastSendTxByUriHandler?.complete(
                        JsBridgeResp(
                            null,
                            4004,
                            "user cancel in check id"
                        )
                    )
                    lastSendTxByUriHandler = null
                })
            }

        }
    }

    private fun fastPledgeQuotaFlowOnTxEnd(status: TxEndStatus) {
        when (status) {
            is TxSendSuccess -> {
                showToast(R.string.tx_send_success)
                lastFastPledgeHandler?.complete(JsBridgeResp(mapOf("isSuccessed" to true), 0, ""))
                lastFastPledgeHandler = null
            }
            is TxSendFailed -> {
                status.throwable.showToast(this@H5WebActivity)
                if (status.throwable is RpcException) {
                    lastSendTxByUriHandler?.complete(
                        JsBridgeResp(
                            null,
                            status.throwable.code,
                            status.throwable.shownMessage(this@H5WebActivity)
                        )
                    )
                } else {
                    lastFastPledgeHandler?.complete(
                        JsBridgeResp(
                            mapOf("isSuccessed" to false),
                            0,
                            "TxSendFailed"
                        )
                    )
                }
                lastFastPledgeHandler = null
            }
            is TxSendCancel -> {
                lastFastPledgeHandler?.complete(
                    JsBridgeResp(
                        mapOf("isSuccessed" to false),
                        0,
                        "user cancel ${status.where}"
                    )
                )
                lastFastPledgeHandler = null
            }

            is FastPledgeQuotaFlow.UserCancelStatus -> {
                lastFastPledgeHandler?.complete(
                    JsBridgeResp(
                        mapOf("isSuccessed" to false),
                        0,
                        "user cancel"
                    )
                )
                lastFastPledgeHandler = null
            }
        }
    }

    @Keep
    inner class PrivateApi {
        @JavascriptInterface
        fun readVitexInviteCode(
            o: Any,
            completionHandler: CompletionHandler<JsBridgeResp<JsonObject>>
        ) {
            val result = JsonObject()
            AccountCenter.dexInviteManager?.getInviteCode()?.let {
                result.addProperty("code", it)
            }
            completionHandler.complete(JsBridgeResp(result))
        }

        @JavascriptInterface
        fun saveVitexInviteCode(
            code: Any,
            completionHandler: CompletionHandler<JsBridgeResp<Any>>
        ) {
            if (code !is JSONObject) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }
            val codeStr = code.optString("code", "")
            if (codeStr.isNotEmpty()) {
                AccountCenter.dexInviteManager?.saveInviteCode(codeStr)
            } else {
                completionHandler.complete(JsBridgeResp.InvalidParam)
            }
        }

        val fragment = SwitchExchangeFragment()

        @JavascriptInterface
        fun addFavPair(
            addFavPair: Any,
            completionHandler: CompletionHandler<JsBridgeResp<Any>>
        ) {
            if (addFavPair !is JSONObject) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }
            val fav = addFavPair.optString("symbol", "")
            if (fav.isNotEmpty()) {
                AccountCenter.getCurrentAccountFavExchangePairManager()?.addFav(fav)
                completionHandler.complete(JsBridgeResp.SuccessResp)
            } else {
                completionHandler.complete(JsBridgeResp.InvalidParam)
            }
        }


        @JavascriptInterface
        fun deleteFavPair(
            favPair: Any,
            completionHandler: CompletionHandler<JsBridgeResp<Any>>
        ) {
            if (favPair !is JSONObject) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }
            val fav = favPair.optString("symbol", "")
            AccountCenter.getCurrentAccountFavExchangePairManager()?.deleteFav(fav)
            completionHandler.complete(JsBridgeResp.SuccessResp)
        }


        @JavascriptInterface
        fun getAllFavPairs(
            o: Any,
            completionHandler: CompletionHandler<JsBridgeResp<Any>>
        ) {

            val list = AccountCenter.getCurrentAccountFavExchangePairManager()?.favPairs
                ?: emptyList<String>()
            completionHandler.complete(JsBridgeResp(data = list))
        }

        @JavascriptInterface
        fun transferAsset(
            tti: Any,
            completionHandler: CompletionHandler<JsBridgeResp<Any>>
        ) {
            runOnUiThread {
                if (tti !is JSONObject) {
                    completionHandler.complete(JsBridgeResp.InvalidParam)
                    return@runOnUiThread
                }
                val ttiStr = tti.optString("tokenId", "")
                if (!Mobile.isValidTokenTypeId(ttiStr)) {
                    completionHandler.complete(JsBridgeResp.InvalidParam)
                    return@runOnUiThread
                }
                completionHandler.complete(JsBridgeResp.SuccessResp)
                InternalTransferActivity.show(this@H5WebActivity, ttiStr)
            }
        }


        @JavascriptInterface
        fun switchPair(
            o: Any,
            completionHandler: CompletionHandler<JsBridgeResp<Any>>
        ) {
            lastSwitchMarketHandler = completionHandler
            runOnUiThread {
                if (fragment.isAdded) {
                    return@runOnUiThread
                }
                val ft = supportFragmentManager.beginTransaction()
                ft.add(R.id.rootContainer, fragment, "imei1")
                ft.addToBackStack(null)
                ft.commit()
            }
        }

        @JavascriptInterface
        fun sendTx(
            sendReq: Any,
            completionHandler: CompletionHandler<JsBridgeResp<in AccountBlock>>
        ) {
            if (lastSendTxHandler != null) {
                completionHandler.complete(JsBridgeResp(null, 4001, "last tx not complete"))
                return
            }

            if (sendReq !is JSONObject) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }

            val block = sendReq.getJSONObject("block")
            val viteBlock =
                kotlin.runCatching {
                    Session.MethodCall.ViteBlock(
                        block.getString("toAddress").also {
                            if (!Mobile.isValidAddress(it)) {
                                throw Exception("invalid toAddress")
                            }
                        },
                        block.getString("tokenId").also {
                            if (!Mobile.isValidTokenTypeId(it)) {
                                throw Exception("invalid tokenId")
                            }
                        },
                        block.getString("amount").also {
                            it.toBigDecimal()
                        },
                        kotlin.runCatching { block.getString("fee") }.getOrNull()?.also {
                            it.toBigDecimal()
                        },
                        kotlin.runCatching {
                            block.getString("data")?.let {
                                Base64.decode(it, Base64.NO_WRAP)
                            }
                        }.getOrNull()
                    )
                }.getOrNull() ?: kotlin.run {
                    completionHandler.complete(JsBridgeResp.InvalidParam)
                    return
                }

            lastSendTxHandler = completionHandler

            runOnUiThread {
                val nowAddr = AccountCenter.currentViteAddress() ?: kotlin.run {
                    lastSendTxHandler?.complete(JsBridgeResp.LoginError)
                    lastSendTxHandler = null
                    return@runOnUiThread
                }

                lastSendParams = viteBlock.toNormalTxParams(nowAddr)
                val title = kotlin.runCatching {
                    if (Mobile.isContactAddress(lastSendParams!!.toAddr)) {
                        getString(R.string.contract_tx_title)
                    } else {
                        getString(R.string.pay)
                    }
                }.getOrDefault(getString(R.string.pay))

                verifyIdentityDirectlyFinger(title, {
                    sendTx(lastSendParams!!, SendTxRequestCode)
                }, {
                    lastSendTxHandler?.complete(
                        JsBridgeResp(
                            null,
                            4004,
                            "user cancel in check id"
                        )
                    )
                    lastSendTxHandler = null
                })
            }

        }


        @JavascriptInterface
        fun quicklyGetQuota(o: Any, completionHandler: CompletionHandler<JsBridgeResp<Any>>) {
            if (lastFastPledgeHandler != null) {
                completionHandler.complete(JsBridgeResp(null, 4001, "last tx not complete"))
                return
            }
            lastFastPledgeHandler = completionHandler
            runOnUiThread {
                fastPledgeQuotaFlow.show(::fastPledgeQuotaFlowOnTxEnd, QuickPledgeQuotaRequestCode)
            }
        }


        @JavascriptInterface
        fun open(o: Any, completionHandler: CompletionHandler<JsBridgeResp<Any>>) {
            if (o !is JSONObject) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }

            val url = o.get("url")
            if (url !is String) {
                completionHandler.complete(JsBridgeResp.InvalidParam)
                return
            }

            if (url == "https://x.vite.net/walletQuota") {
                PledgeQuotaActivity.show(this@H5WebActivity)
                return
            }

            show(this@H5WebActivity, url)

        }
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        if (qrcodeParseResult.requestCode == ScanQrcodeDefaultRequest) {
            val result = JsonObject()
            if (qrcodeParseResult.isCancel) {
            } else {
                result.addProperty("text", qrcodeParseResult.rawData)
            }
            lastScanHandler?.complete(JsBridgeResp(result))
            lastScanHandler = null
        }
    }

    @SuppressLint("CheckResult")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001) {
            downloadUrl?.let {
                kotlin.runCatching { download(it) }
            }
        }
    }
}

