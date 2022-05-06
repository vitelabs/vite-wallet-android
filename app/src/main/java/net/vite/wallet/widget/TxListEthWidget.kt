package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.Keep
import androidx.paging.PagedList
import kotlinx.android.synthetic.main.widget_banlance_tx_list.view.*
import net.vite.wallet.R
import net.vite.wallet.balance.ethtxlist.EthTxListAdapter
import net.vite.wallet.network.eth.EthTransaction

@Keep
class TxListEthWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val view = LayoutInflater.from(context).inflate(R.layout.widget_banlance_tx_list, this)

    var adapter = EthTxListAdapter()

    init {
        txMainList.adapter = adapter
    }

    fun submitList(l: PagedList<EthTransaction>) {
        adapter.submitList(l)
    }

    fun setOnRefreshListener(f: () -> Unit) {
        swipeRefresh.setOnRefreshListener { f() }
    }


    fun showEmpty(show: Boolean) {
        emptyGroup.visibility = if (show) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setRefreshEnable(enable: Boolean) {
        swipeRefresh.visibility = if (enable) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun setEmpty(text: String, onClickListener: () -> Unit) {
        emptyTxt.text = text
        emptyTxt.setTextColor(resources.getColor(R.color.viteblue))

        emptyTxt.setOnClickListener {
            onClickListener()
        }
        emptyImg.setOnClickListener { onClickListener() }
    }

    fun setNewAdapter(adapter: EthTxListAdapter) {
        this.adapter = adapter
        txMainList.adapter = adapter
    }
}