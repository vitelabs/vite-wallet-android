package net.vite.wallet.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.client.android.CaptureActivity
import io.reactivex.disposables.CompositeDisposable
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.setup.LanguageUtils
import net.vite.wallet.utils.addTo
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.vep.QrcodeParser
import net.vite.wallet.viteappuri.ViteAppUri
import net.vite.wallet.vitebridge.UrlOpenHelper
import org.apache.log4j.Logger
import java.net.URL

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {

    private var compositeDisposable = CompositeDisposable()
    val logger = Logger.getLogger(this.javaClass)

    companion object {
        var lastFcmData: Bundle? = null
            get() {
                val a = field
                field = null
                return a
            }

        var openUri: ViteAppUri? = null
            get() {
                val a = field
                field = null
                return a
            }

    }

    private var nowScanRequestCode = 6589

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtils.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkH5Params()
        checkOpenUriParams()
        logger.info("$localClassName onCreate")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkH5Params()
        checkOpenUriParams()
    }

    override fun onStart() {
        super.onStart()
        checkH5Params()
        checkOpenUriParams()
        logger.info("$localClassName onStart")
    }

    override fun onResume() {
        super.onResume()
        checkH5Params()
        checkOpenUriParams()
        logger.info("$localClassName onResume")
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.dispose()
        logger.info("$localClassName onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.info("$localClassName onDestroy")
    }

    private fun checkOpenUriParams() {
        openUri?.action(this)
    }

    private fun checkH5Params() {
        if (AccountCenter.isLogin()) {
            try {
                val url = lastFcmData?.getString("inner_url") ?: ""
                URL(url)
                UrlOpenHelper.open(this, url)?.addTo(compositeDisposable)
            } catch (e: Exception) {
            }
        }
    }


    open fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        when (val result = qrcodeParseResult.result) {
            is String -> {
                if (result.isNotEmpty()) {
                    AlertDialog.Builder(this@BaseActivity).setMessage(
                        result
                    ).setPositiveButton(R.string.button_ok) { dialog, _ ->
                        dialog.dismiss()
                    }.create().show()
                }
            }
            is ViteAppUri -> {
                result.action(this)
            }
            is URL -> {
                UrlOpenHelper.open(this, qrcodeParseResult.rawData)?.addTo(compositeDisposable)
            }
        }
    }

    fun scanQrcode(requestCode: Int = 6589): Int {
        nowScanRequestCode = requestCode
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(Intent(this, CaptureActivity::class.java), nowScanRequestCode)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 9898)
        }
        return nowScanRequestCode
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 9898) {
            if (grantResults.size == 1) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.open_camera_failed, Toast.LENGTH_SHORT).show()
                } else {
                    startActivityForResult(
                        Intent(this, CaptureActivity::class.java),
                        nowScanRequestCode
                    )
                }
            }
            return
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == nowScanRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                val s = data?.getStringExtra("result") ?: ""

                ViteAppUri.createOrNull(s)?.let {
                    onScanResult(QrcodeParseResult(it, s, requestCode))
                    return
                }

                onScanResult(
                    QrcodeParseResult(QrcodeParser.parse(s), s, requestCode)
                )
            } else {
                onScanResult(QrcodeParseResult("", "", requestCode, true))
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}