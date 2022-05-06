package net.vite.wallet.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_recent_transactions.view.*
import net.vite.wallet.R
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.loge
import net.vite.wallet.logt
import net.vite.wallet.network.http.vitex.*
import java.math.RoundingMode
import java.util.*

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [OrderFragment.OnListFragmentInteractionListener] interface.
 */
class OrderFragment : Fragment() {

    private var myOrders: MutableList<Order>? = null
    private var sellListener: View.OnClickListener? = null
    private var buyListener: View.OnClickListener? = null
    private var pairSymbol: String? = null
    private var rootView: View? = null
    private var hasCreatedView = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            pairSymbol = it.getString(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_order_list, container, false)

        hasCreatedView = true

        pullDepthdata()
        return rootView
    }

    fun updatePairSymbol(pairSymbol: String) {
        this.pairSymbol = pairSymbol
        val listView = rootView?.findViewById<RecyclerView>(R.id.list)
        listView?.let {
            it.adapter = OrderRecyclerViewAdapter(mutableListOf())
        }
        pullDepthdata()
    }

    fun pullDepthdata() {
        VitexApi.getDepth(pairSymbol!!).subscribe({
            updateUI(it)
        }, {
            loge(it)
        })

        val tradeTokenSymbol = pairSymbol?.substring(0, pairSymbol?.indexOf("_") ?: 0)
        val quoteTokenSymbol =
            pairSymbol?.substring((pairSymbol?.indexOf("_") ?: 0) + 1, pairSymbol?.length ?: 1)

        tradeTokenSymbol ?: return
        quoteTokenSymbol ?: return
        val address  = AccountCenter.currentViteAddress()?: return

        VitexApi.getOpenOrders(
            address,
            tradeTokenSymbol!!,
            quoteTokenSymbol!!,
            null,
            0,
            100
        ).subscribe({
            myOrders = it.order ?: mutableListOf()
        }, {
            loge(it)
            logt("xirtam VitexApi.getOpenOrders error ${it.message}")
        })
    }

    fun updateData(depthList: DepthList?) {
        updateUI(depthList)
    }

    private fun updateUI(it: DepthList?) {
        if (hasCreatedView) {

            var buyMineLastIndex = -1
            var sellMineLastIndex = -1

            val buy1Price =
                if (it?.bids?.size == 0) Float.MAX_VALUE else it?.bids?.get(0)?.price?.toFloat()
                    ?: 0.0f

            val pricePrecision =
                TickerStatisticsCenter.symbolMap[pairSymbol!!]?.pricePrecision ?: 0

            val asks = mutableListOf<Depth>()
            asks.addAll(it?.asks ?: emptyList())

            asks.sortBy { it.price.toDouble() }

            val sell1Price = if (asks.size == 0) 0.0f else asks[0].price.toFloat()

            val buyRangeMax =
                TickerStatisticsCenter.getMiningOrderMiningSetting(pairSymbol!!)?.buyRangeMax?.toDoubleOrNull()
                    ?: 0.0
            val sellRangeMax =
                TickerStatisticsCenter.getMiningOrderMiningSetting(pairSymbol!!)?.sellRangeMax?.toDoubleOrNull()
                    ?: 0.0
            it?.let {
                var buyBiggestQuantity = .0f
                for (i in 0 until ((it.bids?.size ?: 1) - 1)) {
                    //buy
                    val quantity = it.bids?.get(i)?.quantity?.toFloat() ?: 0f
                    val price = it.bids?.get(i)?.price?.toFloat() ?: 0f
                    buyBiggestQuantity =
                        Math.max(quantity, buyBiggestQuantity)

                    if (buyMineLastIndex == -1) {
                        if (price.toBigDecimal() < (sell1Price - buyRangeMax * sell1Price).toBigDecimal()
                                .setScale(pricePrecision, RoundingMode.DOWN)
                        ) {
                            buyMineLastIndex = i
                        }
                    }
                }

                var sellBiggestQuantity = .0f
                for (i in 0 until (asks.size - 1)) {
                    //sell

                    val quantity = asks[i].quantity.toFloat()
                    val price = asks[i].price.toFloat()
                    sellBiggestQuantity =
                        Math.max(quantity, sellBiggestQuantity)

                    if (sellMineLastIndex == -1) {
                        if (price.toBigDecimal() > (buy1Price + sellRangeMax * buy1Price).toBigDecimal()
                                .setScale(pricePrecision, RoundingMode.UP)
                        ) {
                            sellMineLastIndex = i
                        }
                    }
                }

                var COUNT = Math.max(asks.size, it.bids?.size ?: 0)

                val ITEMS: MutableList<OrderItem> = ArrayList()

                for (i in 0 until COUNT) {
                    val bidItem = if (i < it.bids?.size ?: 0) it.bids?.get(i) else null
                    val askItem = if (i < asks.size) asks[i] else null

                    ITEMS.add(
                        OrderItem(
                            i,
                            bidItem?.quantityView()?: "",
                            askItem?.quantityView()?: "",
                            bidItem?.price ?: "",
                            askItem?.price ?: "",
                            if (buyBiggestQuantity == 0.0f) 0.0f else (bidItem?.quantity?.toFloat()
                                ?: 0.0f) / buyBiggestQuantity,
                            if (sellBiggestQuantity == 0.0f) 0.0f else (askItem?.quantity?.toFloat()
                                ?: 0.0f) / sellBiggestQuantity,
                            i < buyMineLastIndex,
                            i < sellMineLastIndex,
                            i == buyMineLastIndex - 1,
                            i == sellMineLastIndex - 1,
                            hasBuyAvatar = hasSamePrice(bidItem?.price),
                            hasSellAvatar = hasSamePrice(askItem?.price)
                        )
                    )
                }

//                val listView = rootView?.findViewById<RecyclerView>(R.id.list)
//                listView?.let {
//                    if (it.adapter == null) {
//                        it.adapter = OrderRecyclerViewAdapter(ITEMS, null)
//                    } else {
//                        (it.adapter as OrderRecyclerViewAdapter).updateDatas(ITEMS)
//                    }
////                    listView.adapter?.notifyDataSetChanged()
//                }

                rootView?.list?.adapter = OrderRecyclerViewAdapter(ITEMS)
                rootView?.list?.adapter?.notifyDataSetChanged()
            }

        }
    }

    fun hasSamePrice(price: String?): Boolean {
        return (!price.isNullOrEmpty()) && myOrders?.find {
            it.price == price
        } != null
    }

    fun setGlobalListeners(buyListener: View.OnClickListener, sellListener: View.OnClickListener) {
        this.buyListener = buyListener
        this.sellListener = sellListener
    }

    companion object {

        // Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // Customize parameter initialization
        @JvmStatic
        fun newInstance(symbolPair: String) =
            OrderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_COLUMN_COUNT, symbolPair)
                }
            }
    }
}



