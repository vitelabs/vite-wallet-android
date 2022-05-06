package net.vite.wallet.balance.walletconnect.taskdetail

import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.constants.WcNameItem

class WCConfirmInfo constructor(
    title: String,
    val listItems: List<WCConfirmItemInfo>,
    var extraData: Any? = null
) {
    val title = if (title.isEmpty()) {
        ViteConfig.get().context.getString(R.string.transactions)
    } else {
        title
    }
}


class WCConfirmItemInfo(
    val title: String,
    val text: String,
    val textColor: Int? = null,
    val backgroundColor: Int? = null
) {
    companion object {
        fun create(item: WcNameItem, text: String) =
            WCConfirmItemInfo(
                title = item.name?.getTitle() ?: "",
                text = text,
                textColor = item.style?.getTextColor(),
                backgroundColor = item.style?.getBackgroundColor()
            )

        fun create(item: WcNameItem, text: WcNameItem) =
            WCConfirmItemInfo(
                title = item.name?.getTitle() ?: "",
                text = text.name?.getTitle() ?: "",
                textColor = item.style?.getTextColor(),
                backgroundColor = item.style?.getBackgroundColor()
            )

        fun create(title: String, text: String) =
            WCConfirmItemInfo(
                title = title,
                text = text
            )
    }
}






