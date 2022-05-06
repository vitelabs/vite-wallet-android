package net.vite.wallet.me

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_system_setup.*
import net.vite.wallet.*
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.LoginActivity
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.account.register.ModifyAccountPasswordActivity
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.log.ZipUtils
import net.vite.wallet.setup.CurrencyListActivity
import net.vite.wallet.setup.LanguageSetupActivity
import net.vite.wallet.setup.NodeSettingEntryActivity
import net.vite.wallet.setup.PoWSettingActivity
import net.vite.wallet.utils.showToast
import java.io.File


class SystemSetupActivity : UnchangableAccountAwareActivity() {
    companion object {
        fun showSecuritySetting(context: Context, showSecuritySetting: Boolean) {
            context.startActivity(Intent(context, SystemSetupActivity::class.java).apply {
                putExtra("showSecuritySetting", showSecuritySetting)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_setup)
        val showSecuritySetting = intent.getBooleanExtra("showSecuritySetting", false)
        openAppWithPassSetting.isVisible = showSecuritySetting
        useFingerPrintSetting.isVisible = showSecuritySetting
        modifyPassphrase.isVisible = showSecuritySetting
        deleteAccountBtn.isVisible = showSecuritySetting
        if (showSecuritySetting) {
            textViewtitle.setText(R.string.security_setup)
        }
        currencyUnitSetting.isVisible = !showSecuritySetting
        languageSetting.isVisible = !showSecuritySetting
        nodeSettings.isVisible = !showSecuritySetting
        powSettings.isVisible = !showSecuritySetting
        logBackBtn.isVisible = !showSecuritySetting


        switchShowDebugFloatView.isChecked = ViteConfig.get().needShowEnvSwitchBtn()
        showDebugFloatViewContainer.setOnClickListener {
            ViteConfig.get().showEnvSwitchBtn(!switchShowDebugFloatView.isChecked)
            switchShowDebugFloatView.isChecked = false
            finish()
        }

        currencyUnitSetting.setOnClickListener {
            startActivity(Intent(this@SystemSetupActivity, CurrencyListActivity::class.java))
        }

        languageSetting.setOnClickListener {
            startActivity(Intent(this@SystemSetupActivity, LanguageSetupActivity::class.java))
        }

        nodeSettings.setOnClickListener {
            startActivity(Intent(this@SystemSetupActivity, NodeSettingEntryActivity::class.java))
        }

        powSettings.setOnClickListener {
            startActivity(Intent(this@SystemSetupActivity, PoWSettingActivity::class.java))
        }

        openAppWithPassSwitcher.isChecked =
            ViteConfig.get().kvstore.getBoolean("needPasswordToOpen", false)
        openAppWithPassSetting.setOnClickListener {
            openAppWithPassSwitcher.isChecked = !openAppWithPassSwitcher.isChecked
            ViteConfig.get().kvstore.edit()
                .putBoolean("needPasswordToOpen", openAppWithPassSwitcher.isChecked).apply()
        }

        switcUseFingerPrint.isChecked = ViteConfig.get().kvstore.getBoolean("USEFingerprint", false)
        useFingerPrintSetting.setOnClickListener {
            val f = this.getSystemService(FingerprintManager::class.java)
            if (f == null || !f.isHardwareDetected) {
                showToast(R.string.not_support_fp)
            } else if (!f.hasEnrolledFingerprints()) {
                showToast(R.string.please_add_fp_in_system)
            } else {
                if (!switcUseFingerPrint.isChecked) {
                    checkId { _ ->
                        switcUseFingerPrint.isChecked = true
                        ViteConfig.get().kvstore.edit().putBoolean("USEFingerprint", true)
                            .apply()
                    }
                } else {
                    switcUseFingerPrint.isChecked = false
                    ViteConfig.get().kvstore.edit().putBoolean("USEFingerprint", false).apply()
                }
            }
        }

        deleteAccountBtn.setOnClickListener {
            TextTitleNotifyDialog(this).apply {
                setTitle(R.string.notice)
                setMessage(R.string.delete_account_hint)
                setBottom(R.string.confirm) {
                    it.dismiss()
                    checkId { _ ->
                        AccountCenter.removeCurrentAccount()
                        AccountCenter.deleteMyselfAndRestart()
                        AccountCenter.buildAccountRelation()
                        finish()
                        val intent = Intent(this@SystemSetupActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        logi("Delete account all ok")
                    }
                }
                show()
            }
        }

        modifyPassphrase.setOnClickListener {
            checkId { password ->
                ModifyAccountPasswordActivity.show(
                    this, password
                )
            }
        }

        logBackBtn.setOnClickListener {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
            )


        }
    }

    var disposable: Disposable? = null

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            disposable = Observable.fromCallable {
                kotlin.runCatching {
                    File(ViteConfig.get().viteExternalRootPath, "log").deleteRecursively()
                }
                val targetFileDir = File(ViteConfig.get().viteExternalRootPath, "vitelog")

                val oldFile = kotlin.runCatching {
                    targetFileDir.listFiles().find {
                        it.name.matches(Regex("^log-\\d{9}.zip"))
                    }
                }.getOrNull()?.let {
                    if (System.currentTimeMillis() - it.lastModified() < 1 * 60 * 1000) {
                        it
                    } else {
                        null
                    }
                }
                if (oldFile != null) {
                    return@fromCallable oldFile
                }

                kotlin.runCatching {
                    targetFileDir.deleteRecursively()
                }

                if (targetFileDir.isFile) {
                    targetFileDir.delete()
                }

                if (!targetFileDir.isDirectory) {
                    targetFileDir.mkdirs()
                }
                val targetFile = File(targetFileDir, "log-${System.currentTimeMillis()}.zip")

                ZipUtils.zipDirectory(File(ViteConfig.get().viteLogPath), targetFile)

                targetFile
            }.subscribeOn(Schedulers.io()).observeOn(
                AndroidSchedulers.mainThread()
            ).subscribe({
                val i = Intent(Intent.ACTION_SEND).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(
                        Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(
                            ViteConfig.get().context,
                            "$packageName.fileprovider", it!!
                        )
                    )
                    type = "application/zip"
                }
                startActivity(Intent.createChooser(i, getString(R.string.log_back_txt)))

            }, {
                it.printStackTrace()
                showToast(it.message ?: "failed:${getString(R.string.need_storage_permission)}")
            })

        } else {
            showToast(R.string.need_storage_permission)
        }
    }

    override fun onStart() {
        super.onStart()
        currentUnit.text = ViteConfig.get().currentCurrency()
    }

    private fun checkId(onSuccess: (password: String) -> Unit) {
        with(BigDialog(this)) {
            setBigTitle(getString(R.string.check_identity))
            setPasswordEnable(true)
            setFirstButton(getString(R.string.confirm)) {
                if (Wallet.checkPassphrase(
                        currentAccount.entropyStore,
                        getPwdInputText()
                    )
                ) {
                    onSuccess(getPwdInputText())
                    dismiss()
                } else {
                    showToast(R.string.password_error)
                }
            }
            show()
        }
    }

}