package net.vite.wallet

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.account.register.RememberMnemonicActivity
import net.vite.wallet.balance.BalanceFragment
import net.vite.wallet.balance.walletconnect.session.VcRequestTask
import net.vite.wallet.balance.walletconnect.session.fsm.VCState
import net.vite.wallet.balance.walletconnect.taskdetail.buildin.DexPost
import net.vite.wallet.dexassets.DexAssetsTabFragment
import net.vite.wallet.dialog.BigDialog
import net.vite.wallet.exchange.ExchangeFragment
import net.vite.wallet.exchange.MarketVM
import net.vite.wallet.exchange.SpecialMarketCenter
import net.vite.wallet.exchange.TickerStatisticsCenter
import net.vite.wallet.exchange.net.ws.ExchangeWsHolder
import net.vite.wallet.me.ExportMnemonicActivity
import net.vite.wallet.network.GlobalKVCache
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.http.operation.OperationApi
import net.vite.wallet.network.http.operation.VCConfigsResp
import net.vite.wallet.network.http.vitex.VitexApi
import net.vite.wallet.nut.NutFragment
import net.vite.wallet.trade.TradeFragment
import net.vite.wallet.utils.addTo
import net.vite.wallet.vep.QrcodeParseResult
import net.vite.wallet.vep.ViteBridgeUrlTransferParams
import net.vite.wallet.viteappuri.ViteAppUri
import net.vite.wallet.vitebridge.H5WebActivity
import java.lang.ref.WeakReference
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class MainActivity : UnchangableAccountAwareActivity() {
    lateinit var balanceFragment: BalanceFragment
    lateinit var nutFragment: NutFragment
    lateinit var dexAssetsFragment: DexAssetsTabFragment
    lateinit var exchangeFragment: ExchangeFragment
    lateinit var tradeFragment: TradeFragment

    private val vm by viewModels<GetAnnouncementVM>()
    val marketVm by viewModels<MarketVM>()
    val fragmentList = ArrayList<Fragment>()
    var currentIndex = -1
    private var compositeDisposable = CompositeDisposable()

    companion object {
        var sRef: WeakReference<MainActivity>? = null

        fun toTradeFragment(context: Context, isBuy: Boolean, symbol: String) {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                putExtra("isBuy", isBuy)
                putExtra("symbol", symbol)
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
    }

    private var dialog: AlertDialog? = null
    private var mnemonicNoticeDialog: BigDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        sRef = WeakReference(this)

        if (savedInstanceState != null) {
            finish()
            startActivity(Intent(this, SplashActivity::class.java))
            return
        }

        dexAssetsFragment = DexAssetsTabFragment()
        nutFragment = NutFragment()
        balanceFragment = BalanceFragment()
        tradeFragment = TradeFragment.newInstance("VITE_BTC-000")
        exchangeFragment = ExchangeFragment()

        fragmentList.add(balanceFragment)
        fragmentList.add(nutFragment)
        fragmentList.add(exchangeFragment)
        fragmentList.add(tradeFragment)
        fragmentList.add(dexAssetsFragment)

        bottomNavigationView.isItemHorizontalTranslationEnabled = false
        bottomNavigationView.itemIconTintList = null
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.walletTab -> {
                    showContent(0)
                }
                R.id.nutTab -> {
                    showContent(1)
                }
                R.id.marketTab -> {
                    showContent(2)
                }
                R.id.buyCoinTab -> {
                    showContent(3)
                }
                R.id.assetTab -> {
                    showContent(4)
                }
            }
            true
        }



        vm.announcementLd.observe(this, Observer {
            if (!it.isCancel) {
                if (dialog?.isShowing != true) {
                    dialog = AlertDialog.Builder(this@MainActivity)
                        .setTitle(it.title)
                        .setMessage(it.message)
                        .setCancelable(false)
                        .create()
                    dialog?.show()
                }
            } else {
                if (dialog?.isShowing == true) {
                    dialog?.dismiss()
                }
            }
        })

        val dexInviteCode = intent.getStringExtra("dex_invite_code")
        if (!dexInviteCode.isNullOrEmpty()) {
            AccountCenter.dexInviteManager?.saveInviteCode(dexInviteCode)
        }

        val mnemonic = intent.getStringExtra("showMnemonic")
        if (!mnemonic.isNullOrEmpty()) {
            val mnemonicPassphrase = intent.getStringExtra("MnemonicPassphrase") ?: ""
            ExportMnemonicActivity.show(
                this,
                mnemonicTxt = mnemonic,
                passphrase = mnemonicPassphrase
            )
        }

        OperationApi.pullVCConfigs().applyIoScheduler().retryWhen { it.delay(5, TimeUnit.SECONDS) }
            .subscribe({
                GlobalKVCache.store("vcconfigs" to Gson().toJson(it))
                loadVCConfigs(it)
            }, {
                logi("request pullVCConfigs error ${it.message}")
                val vcconfigs =
                    Gson().fromJson(GlobalKVCache.read("vcconfigs"), VCConfigsResp::class.java)
                loadVCConfigs(vcconfigs)
            }).addTo(compositeDisposable)

        showContent(0)

        addPV()

        VitexApi.getHideSymbols().retryWhen { it.delay(1, TimeUnit.MINUTES) }.subscribe({
            SpecialMarketCenter.updateHidden(it)
        }, {
            logi("request hidden markets error ${it.message}")
            loge(it)
        }).addTo(compositeDisposable)

        VitexApi.getMarketsClosed().retryWhen { it.delay(5, TimeUnit.SECONDS) }.subscribe({
            SpecialMarketCenter.updateClosed(it)
        }, {
            logi("request getMarketsClosed error ${it.message}")
            loge(it)
        }).addTo(compositeDisposable)

        VitexApi.getMiningSetting().retryWhen { it.delay(5, TimeUnit.SECONDS) }.subscribe({
            TickerStatisticsCenter.miningSettingCacheMap = it
        }, {
            logt("xirtam VitexApi.getMiningSetting error ${it.message}")
            loge(it)
        }).addTo(compositeDisposable)

        TokenInfoCenter.pollViteXTokens()

    }

    private fun addPV() {
        Thread {
            try {
                val text =
                    URL("https://x.vite.net/logo.png?timestamp=${System.currentTimeMillis()}").readText()
            } catch (e: Exception) {
                loge(e, "pv")
            }
        }.start()
    }

    private fun loadVCConfigs(vcconfigs: VCConfigsResp) {
        val remoteSupportList = java.util.ArrayList<Pair<String, String>>()
        vcconfigs.data.dexPostContractPairs.forEach {
            if ("tti_3d3bd4c43620ad1a3bcc630a/tti_80f3751485e4e83456059473".length == it.length) {
                remoteSupportList.add(
                    it.substring(0, it.indexOf("/")) to it.substring(it.indexOf("/") + 1)
                )
            }
        }

        DexPost.remoteSupportList = remoteSupportList

        val autoSignContracts = ArrayList<String>()
        vcconfigs.data.contract.forEach {
            if ("vite_0000000000000000000000000000000000000006e82b8ba657.e7f03bc7".length == it.length) {
                val hash =
                    it.substring("vite_0000000000000000000000000000000000000006e82b8ba657.".length)
                autoSignContracts.add(hash)
            }
        }

        VcRequestTask.autoSignContracts = autoSignContracts
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v: View? = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }


    fun showMnemonicNoticeDialog() {
        if (mnemonicNoticeDialog?.isShowing == true) {
            return
        }
        mnemonicNoticeDialog = BigDialog(this)

        mnemonicNoticeDialog?.apply {
            setBigTitle(getString(R.string.mnemonic_not_backup))
            setSecondTitle(getString(R.string.please_input_password_to_backup))
            setPasswordEnable(true)
            setFirstButton(getString(R.string.goto_backup)) {
                try {
                    val mnemonicTxt = Wallet.instance.extractMnemonic(
                        currentAccount.entropyStore, getPwdInputText()
                    )
                    dismiss()
                    RememberMnemonicActivity.show4Backup(
                        context = this@MainActivity,
                        mnemonicText = mnemonicTxt,
                        accountName = AccountCenter.getCurrentAccount()?.name ?: "",
                        passphrase = getPwdInputText()
                    )
                } catch (e: Exception) {
                    Toast.makeText(activity, R.string.password_error, Toast.LENGTH_SHORT).show()
                }
            }
            setSecondButton(getString(R.string.temprory_not_backup)) {
                dismiss()
            }
            show()
        }
    }


    fun onExchangeMarketItemSwitchedFromDex(symbol: String) {
        bottomNavigationView.selectedItemId = R.id.buyCoinTab
        tradeFragment.setPair(symbol)
    }

    fun onExchangeMarketItemSwitched(symbol: String) {
        kotlin.runCatching {
            supportFragmentManager.popBackStackImmediate()
        }
        tradeFragment.onExchangeMarketItemSwitched(symbol)
    }

    override fun onScanResult(qrcodeParseResult: QrcodeParseResult) {
        when (qrcodeParseResult.result) {
            is String -> {
                super.onScanResult(qrcodeParseResult)
            }
            is ViteAppUri -> {
                super.onScanResult(qrcodeParseResult)
            }
            is ViteBridgeUrlTransferParams -> {
                super.onScanResult(qrcodeParseResult)
            }
            is URL -> {
                H5WebActivity.show(this, qrcodeParseResult.rawData)
            }
            else -> {
                balanceFragment.onScanQrResult(qrcodeParseResult)
            }
        }
    }



    private fun showContent(index: Int) {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        if (currentIndex != -1) {
            ft.hide(fragmentList[currentIndex])
        }
        currentIndex = index
        val tag = index.toString()
        val cacheFragment = fm.findFragmentByTag(tag)
        if (cacheFragment != null) {
            ft.show(cacheFragment)
        } else {
            ft.add(R.id.fragmentContainer, fragmentList[index], tag)
        }
        ft.commit()
    }


    override fun onStart() {
        super.onStart()
        if (currentAccount.sharedPreferences()?.getBoolean("hasnotBackupMnemonic", false) == true) {
            showMnemonicNoticeDialog()
        }
        vm.getAnnouncement()
        marketVm.getAll24HPriceChangeByCategory(false)
        marketVm.getAllTokenExchangeRate(false)
        Thread {
            kotlin.runCatching {
                NetConfigHolder.pullRemoteConfig()
                ExchangeWsHolder.connect()
            }
        }.start()
    }


    override fun wcSessionStatusAwareFun(state: VCState) {
        super.wcSessionStatusAwareFun(state)
        runOnUiThread { balanceFragment.wcSessionStatusAwareFun(state) }
    }

    override fun onDestroy() {
        super.onDestroy()
        sRef?.clear()
        sRef = null
        compositeDisposable.dispose()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val isBuy = intent?.getBooleanExtra("isBuy", false)
        val symbol = intent?.getStringExtra("symbol")

        if (symbol != null) {
            bottomNavigationView.selectedItemId = R.id.buyCoinTab
            tradeFragment.setIsSell(isBuy != true)
            tradeFragment.setPair(symbol)
        }
    }
}
