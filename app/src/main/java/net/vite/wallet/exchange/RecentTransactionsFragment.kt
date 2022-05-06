package net.vite.wallet.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_recent_transactions.view.*
import net.vite.wallet.R
import net.vite.wallet.loge
import net.vite.wallet.network.http.vitex.TradeList
import net.vite.wallet.network.http.vitex.VitexApi

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [RecentTransactionsFragment.OnListFragmentInteractionListener] interface.
 */
class RecentTransactionsFragment : Fragment() {

    private var rtAdapter: RecentTransactionsViewAdapter? = null
    private var hasCreatedView: Boolean = false
    private var rootView: View? = null
    private var pairSymbol: String? = null
    private var sellListener: View.OnClickListener? = null
    private var buyListener: View.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//
        arguments?.let {
            pairSymbol = it.getString(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_recent_transactions, container, false)

        hasCreatedView = true

        pullTradeData()
        return rootView
    }

    private fun pullTradeData() {
        VitexApi.getTrades(pairSymbol!!).subscribe({
            rtAdapter = RecentTransactionsViewAdapter(it.trade ?: emptyList())
            rootView?.list?.adapter = rtAdapter
            TradeListCenter.symbolMap[pairSymbol!!] = it
        }, {
            loge(it)
        })
    }

    fun updatePairSymbol(pairSymbol: String) {
        this.pairSymbol = pairSymbol
        val listView = rootView?.findViewById<RecyclerView>(R.id.list)
        listView?.let {
            it.adapter = OrderRecyclerViewAdapter(mutableListOf())
        }
        pullTradeData()
    }

    fun setGlobalListeners(buyListener: View.OnClickListener, sellListener: View.OnClickListener) {
        this.buyListener = buyListener
        this.sellListener = sellListener
    }

    fun updateData(tradeList: TradeList?) {
        updateUI(tradeList)
    }

    private fun updateUI(tradeList: TradeList?) {
        if (hasCreatedView) {
            rootView?.list?.adapter = RecentTransactionsViewAdapter(tradeList?.trade ?: emptyList())
            rootView?.list?.adapter?.notifyDataSetChanged()
        }
    }

    companion object {

        // Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // Customize parameter initialization
        @JvmStatic
        fun newInstance(pairSymbol: String) =
            RecentTransactionsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_COLUMN_COUNT, pairSymbol)
                }
            }
    }
}
