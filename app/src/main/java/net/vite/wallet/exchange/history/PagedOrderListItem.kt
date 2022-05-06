package net.vite.wallet.exchange.history

import net.vite.wallet.network.http.vitex.Order

class PagedOrderListItem(
    val offset: Int,
    val orders: List<Order>
)
