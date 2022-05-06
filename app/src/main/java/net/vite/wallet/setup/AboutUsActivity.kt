package net.vite.wallet.setup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_about_us.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.network.http.configbucket.VersionAwareViewModel
import net.vite.wallet.network.http.configbucket.prepareVersionDialog
import net.vite.wallet.network.rpc.AsyncNet
import net.vite.wallet.utils.createBrowserIntent
import net.vite.wallet.utils.showToast
import java.util.concurrent.TimeUnit

class AboutUsActivity : UnchangableAccountAwareActivity() {


    val versionAwareVm by viewModels<VersionAwareViewModel>()

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
        val versionNameStr = application.packageManager.getPackageInfo(packageName, 0).versionName
        versionName.text = "${getString(R.string.version)}  $versionNameStr"

        github.setOnClickListener {
            startActivity(createBrowserIntent("https://github.com/vitelabs"))
        }

        medium.setOnClickListener {
            startActivity(createBrowserIntent("https://medium.com/vitelabs"))
        }

        discord.setOnClickListener {
            startActivity(createBrowserIntent("https://discordapp.com/invite/CsVY76q"))
        }

        twitter.setOnClickListener {
            startActivity(createBrowserIntent("https://twitter.com/vitelabs"))
        }

        telegram.setOnClickListener {
            startActivity(createBrowserIntent("https://t.me/vite_en"))
        }

        reddit.setOnClickListener {
            startActivity(createBrowserIntent("https://www.reddit.com/r/vitelabs"))
        }

        youtube.setOnClickListener {
            startActivity(createBrowserIntent("https://www.youtube.com/channel/UC8qft2rEzBnP9yJOGdsJBVg"))
        }

        forum.setOnClickListener {
            startActivity(createBrowserIntent("https://forum.vite.net"))
        }

        bitcointalk.setOnClickListener {
            startActivity(createBrowserIntent("https://bitcointalk.org/index.php?topic=5056409"))
        }

        facebook.setOnClickListener {
            startActivity(createBrowserIntent("https://www.facebook.com/vitelabs"))
        }

        viteOrg.setOnClickListener {
            startActivity(createBrowserIntent("https://www.vite.org/"))
        }
        viteNet.setOnClickListener {
            startActivity(createBrowserIntent("https://vite.net/"))
        }

        viteBlog.setOnClickListener {
            startActivity(createBrowserIntent("https://docs.vite.org/"))
        }

        contactUs.setOnClickListener {
            val i =
                Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:vitewallet@vitex.zendesk.com")
                ).apply {
                    putExtra(Intent.EXTRA_TEXT, "FROM-ANDROID-$versionNameStr")
                }
            startActivity(Intent.createChooser(i, getString(R.string.contact_us)))
        }

        versionTag.setOnClickListener {
            versionAwareVm.checkVersion()
        }

        versionAwareVm.networkState.observe(this, Observer {
            it.throwable?.showToast(this)
        })

        versionAwareVm.versionAwareLd.observe(this, Observer { newVersion ->
            newVersion?.code?.let { remoteCode ->
                val currentCode = packageManager.getPackageInfo(packageName, 0).versionCode
                if (remoteCode > currentCode) {
                    prepareVersionDialog(this, newVersion, currentCode).show()
                } else {
                    Toast.makeText(this, R.string.this_is_laster_version, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        disposable =
            AsyncNet.getSnapshotBlockHeight()
                .repeatWhen { it.delay(5, TimeUnit.SECONDS) }
                .retryWhen { it.delay(5, TimeUnit.SECONDS) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    it.resp?.let { height ->
                        if (height != "") {
                            currentSbHeight.text = height
                        }
                    }
                }, { exc ->
                    exc.printStackTrace()
                })
    }

    override fun onStop() {
        disposable?.dispose()
        super.onStop()
    }
}
