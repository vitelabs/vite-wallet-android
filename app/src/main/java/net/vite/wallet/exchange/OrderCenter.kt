package net.vite.wallet.exchange

import android.annotation.SuppressLint
import net.vite.wallet.network.http.vitex.Order
import java.util.concurrent.ConcurrentHashMap

object OrderCenter {

    val symbolMap = ConcurrentHashMap<String, MutableList<Order>>()

    @SuppressLint("CheckResult")
    private fun updateMaps(order: Order) {
        symbolMap[order.symbol!!] ?: run {
            symbolMap[order.symbol!!] = mutableListOf()
        }
        when (order.status) {
            0 -> symbolMap[order.symbol!!]?.add(order)
            1 -> {
                val findOrder = symbolMap[order.symbol!!]?.find {
                    it.orderId == order.orderId
                }
                symbolMap[order.symbol!!]?.remove(findOrder)
                symbolMap[order.symbol!!]?.add(order)
            }
            2, 3 -> symbolMap[order.symbol!!]?.remove(order)
            else -> {

            }
        }

        symbolMap[order.symbol!!]?.sortByDescending {
            it.createTime
        }
    }

    fun onNewMessageArrived(order: Order) {
        updateMaps(order)
    }
}