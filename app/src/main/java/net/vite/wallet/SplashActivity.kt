package net.vite.wallet

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_splash.*
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.LoginActivity
import net.vite.wallet.account.register.CreateAccountEntryActivity
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.network.http.configbucket.VersionAwareViewModel
import net.vite.wallet.network.http.configbucket.prepareVersionDialog
import net.vite.wallet.setup.LanguageUtils
import net.vite.wallet.viteappuri.ViteAppUri
import java.util.concurrent.atomic.AtomicBoolean

class SplashActivity : AppCompatActivity() {
    private var hasNewVersion = AtomicBoolean(false)
    private var animatorEnd = AtomicBoolean(false)
    private val hasWentToNext = AtomicBoolean(false)

    private var dialog: AlertDialog? = null

    private lateinit var versionAwareVm: VersionAwareViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (intent.extras?.containsKey("inner_url") == true) {
                BaseActivity.lastFcmData = intent.extras
            } else {
                if (intent.data != null) {
                    BaseActivity.openUri = ViteAppUri.createOrNull(intent.data!!)
                }
            }
        } catch (e: Exception) {
            loge(e)
        }

        if ((application as ViteApplication).appIsRunning()) {
            finish()
            return
        }

        setContentView(R.layout.activity_splash)

        versionAwareVm = ViewModelProviders.of(this)[VersionAwareViewModel::class.java]

        versionAwareVm.versionAwareLd.observe(this, Observer { newVersion ->
            val dontShowInSplash = newVersion.dontShowInSplash ?: false
            if (dontShowInSplash) {
                return@Observer
            }
            val remoteCode = newVersion?.code ?: return@Observer

            val omitVersionCode = ViteConfig.get().kvstore.getInt("omitVersionCode", -1)
            if (remoteCode == omitVersionCode) {
                return@Observer
            }

            val currentCode = packageManager.getPackageInfo(packageName, 0).versionCode
            if (remoteCode > currentCode) {
                hasNewVersion.set(true)
                if (dialog?.isShowing != true) {
                    dialog = prepareVersionDialog(
                        this,
                        newVersion,
                        currentCode
                    ) {
                        ViteConfig.get().kvstore.edit().putInt("omitVersionCode", remoteCode)
                            .apply()
                        gotoNextActivity()
                    }
                    dialog?.show()
                }
            } else {
                if (dialog?.isShowing == true) {
                    dialog?.dismiss()
                }
                hasNewVersion.set(false)
                if (animatorEnd.get()) {
                    gotoNextActivity()
                }
            }

        })
        showAnimation()
    }


    override fun onStart() {
        super.onStart()
        versionAwareVm.checkVersion()
    }


    private fun showAnimation() {
        val animator = ObjectAnimator.ofFloat(mainIcon, "alpha", 0f, 1f)
        animator.duration = 1200
        animator.start()
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                animatorEnd.set(true)
                if (!hasNewVersion.get()) {
                    gotoNextActivity()
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtils.onAttach(newBase))
    }

    fun gotoNextActivity() {
        if (hasWentToNext.get()) {
            return
        }
        hasWentToNext.set(true)
        if (AccountCenter.isLogin()) {
            startActivity(
                Intent(this@SplashActivity, MainActivity::class.java)
            )
        } else {
            val listAllEntropyFiles = AccountCenter.buildAccountRelation()
            if (listAllEntropyFiles.isNotEmpty()) {
                if (!ViteConfig.get().kvstore.getBoolean("needPasswordToOpen", false)
                    && AccountCenter.tryToAutoLogin()
                ) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                } else {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                }
            } else {
                startActivity(
                    Intent(
                        this@SplashActivity,
                        CreateAccountEntryActivity::class.java
                    )
                )
            }
        }
        finish()
    }


}
