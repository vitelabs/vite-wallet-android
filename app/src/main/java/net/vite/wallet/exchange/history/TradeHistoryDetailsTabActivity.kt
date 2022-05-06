package net.vite.wallet.exchange.history

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import net.vite.wallet.R
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.constants.DexCancelContractAddress
import net.vite.wallet.network.rpc.BuildInContractEncoder
import net.vite.wallet.network.rpc.NormalTxParams
import net.vite.wallet.utils.hasNetWork
import net.vite.wallet.utils.hexToBytes
import net.vite.wallet.utils.showToast
import net.vite.wallet.utils.viewbinding.findMyView
import java.math.BigInteger
import kotlin.properties.Delegates

class TradeHistoryDetailsTabActivity : BaseTxSendActivity() {

    private val tab1 by findMyView<TextView>(R.id.tab1)
    private val tab2 by findMyView<TextView>(R.id.tab2)
    private val filters by findMyView<ImageView>(R.id.orders_filter)

    //    private val fragmentContainer by findMyView<FrameLayout>(R.id.fragmentContainer)
    private val openOrdersFragment by lazy { OpenOrderListFragment.newInstance() }
    private val historyOrdersFragment by lazy { HistoryOrderListFragment() }

    private var currentTab: Int by Delegates.observable(-1) { _, oldValue, newValue ->
        if (oldValue == newValue) return@observable
        tab1.select(newValue == 0, true)
        tab2.select(newValue == 1, false)
        filters.isVisible = newValue == 1
        when (newValue) {
            0 -> supportFragmentManager.beginTransaction()
                .apply {
                    if (supportFragmentManager.findFragmentByTag("12") != null) {
                        hide(historyOrdersFragment)
                    }
                    show(openOrdersFragment)
                }
                .commitAllowingStateLoss()
            1 -> supportFragmentManager.beginTransaction()
                .apply {
                    hide(openOrdersFragment)
                    if (supportFragmentManager.findFragmentByTag("12") != null) {
                        show(historyOrdersFragment)
                    } else {
                        add(R.id.fragmentContainer, historyOrdersFragment, "12")
                    }
                }
                .commitAllowingStateLoss()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade_history_details_tab)
        tab1.setOnClickListener {
            currentTab = 0
        }
        tab2.setOnClickListener {
            currentTab = 1
        }
        filters.setOnClickListener {
            historyOrdersFragment.onFilterIconClick()
        }
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, openOrdersFragment).commitAllowingStateLoss()
        currentTab = 0
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun TextView.select(isSelect: Boolean, isLeftTab: Boolean) {
        if (isSelect) {
            background = if (isLeftTab) {
                getDrawable(R.drawable.trader_history_bg_left_select)
            } else {
                getDrawable(R.drawable.trader_history_bg_right_select)
            }
            setTextColor(Color.WHITE)
        } else {
            background = if (isLeftTab) {
                getDrawable(R.drawable.trader_history_bg_left_unselect)
            } else {
                getDrawable(R.drawable.trader_history_bg_right_unselect)
            }
            setTextColor(Color.parseColor("#ff007aff"))
        }

    }

    fun doRevoke(orderId: String?, tradeTokenAddress: String) {
        if (!hasNetWork()) {
            showToast(R.string.net_work_error)
            return
        }

        orderId ?: run {
            showToast(R.string.can_find_order)
            return
        }

        lastSendParams = NormalTxParams(
            toAddr = DexCancelContractAddress,
            amountInSu = BigInteger.ZERO,
            data = BuildInContractEncoder.dexCancelOrderByTransactionHash(
                sendHash = orderId.hexToBytes()
            ),
            tokenId = tradeTokenAddress,
            accountAddr = currentAccount.nowViteAddress()!!
        )

        lastSendParams?.let { sendTx(it) }

    }
}