package net.vite.wallet

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import io.reactivex.plugins.RxJavaPlugins
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.balance.Onroad
import net.vite.wallet.balance.quota.PowCount
import net.vite.wallet.balance.walletconnect.session.VCFsmHolder
import net.vite.wallet.log.LogConfigurator
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.setup.LanguageUtils
import net.vite.wallet.utils.showToast
import org.apache.log4j.Level
import org.vitelabs.mobile.Mobile
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class ViteApplication : Application() {


    var activities = ArrayList<Activity>()

    fun getTopActivity(): Activity? {
        if (activities.isEmpty()) {
            return null
        }
        return activities.last()
    }

    fun appIsRunning(): Boolean {
        activities.forEach {
            if (it is BaseActivity && !it.isFinishing) {
                return true
            }
        }
        return false
    }

    @Volatile
    private var foregroundCount = 0


    override fun onCreate() {
        super.onCreate()
        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
        }
        Mobile.disableAllLog()

        ViteConfig.build(this, "")
        TokenInfoCenter.init()
        PowCount.fetchPowMaxLimit()
        try {
            if (currentProcessName() == BuildConfig.APPLICATION_ID) {
                initLog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityStarted(activity: Activity) {
                foregroundCount++
                if (foregroundCount == 1) {
                    VCFsmHolder.signManager?.appIsForeground = true
                }
            }

            override fun onActivityDestroyed(activity: Activity) {
                activities.remove(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityStopped(activity: Activity) {
                foregroundCount--
                if (foregroundCount == 0) {
                    VCFsmHolder.signManager?.appIsForeground = false
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activities.add(activity)
            }

        })
    }


    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            Onroad.stop()
            activities.forEach { it.finish() }
            exitProcess(0)
        }
    }

    fun restartApp() {
//        Onroad.stop()
        activities.forEach { it.finish() }
        startActivity(Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })

    }


    private fun currentProcessName(): String {
        var processName = ""
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var i = 0
        while (i < 5) {
            for (info in am.runningAppProcesses) {
                if (info.pid == android.os.Process.myPid()) {
                    processName = info.processName
                    break
                }
            }
            if (!TextUtils.isEmpty(processName)) {
                return processName
            }
            try {
                Thread.sleep(100)
            } catch (ex: InterruptedException) {
                ex.printStackTrace()
            }
            i++
        }
        return ""
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtils.onAttach(newBase))
    }

    private fun initLog() {
        kotlin.runCatching {
            with(LogConfigurator()) {
                filePattern =
                    "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] --> %m%n"
                //"%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %l --> %m%n"
                fileName = ViteConfig.get().viteRootLogName

                logCatPattern =
                    "[%t]--> %m%n"
                maxBackupSize = 16
                maxFileSize = 1 * 1024 * 1024
                isImmediateFlush = true
                isUseFileAppender = true
                isUseLogCatAppender = BuildConfig.DEBUG
                rootLevel = if (BuildConfig.DEBUG) {
                    Level.ALL
                } else {
                    Level.INFO
                }
                configure()
            }
        }
    }
}
