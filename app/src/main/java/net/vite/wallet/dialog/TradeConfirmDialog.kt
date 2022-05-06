package net.vite.wallet.dialog

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import net.vite.wallet.R
import net.vite.wallet.network.GlobalKVCache

class TradeConfirmDialog(val activity: Activity) : Dialog(activity, R.style.NormalAlertDialog) {

    class Params(
        var bigTitle: String? = null,
        var price: String? = null,
        var quantity: String? = null,
        var priceSymbol: String? = null,
        var quantitySymbol: String? = null,
        var firstButtonTxt: String? = null,
        var firstButtonOnClick: (() -> Unit)? = null,
        var secondButtonTxt: String? = null,
        var secondButtonOnClick: (() -> Unit)? = null,
        var balance: String? = null
    )

    fun setUiParams(p: Params) {
        p.apply {
            bigTitle?.let { setBigTitle(it) }
            price?.let { setPrice(it) }
            quantity?.let { setQuantity(it) }
            priceSymbol?.let { setPriceSymbol(it) }
            quantitySymbol?.let { setQuantitySymbol(it) }
            firstButtonTxt?.let { setFirstButton(it, firstButtonOnClick) }
            secondButtonTxt?.let { setFirstButton(it, secondButtonOnClick) }
        }
        return
    }

    private var titleTxt: TextView

    private var priceTxt: TextView
    private var priceSymbolTxt: TextView
    private var quantitySymbolTxt: TextView

    private var checkBox: CheckBox
    private var quantityTxt: TextView


    private var firstButton: TextView
    private var secondButton: TextView

    init {
        setContentView(R.layout.trade_confirm_dialog)
        titleTxt = findViewById(R.id.titleTxt)!!
        priceTxt = findViewById(R.id.priceTxt)!!
        priceSymbolTxt = findViewById(R.id.priceSymbolTxt)!!
        quantitySymbolTxt = findViewById(R.id.quantitySymbolTxt)!!
        quantityTxt = findViewById(R.id.quantityTxt)!!
        checkBox = findViewById(R.id.checkBox)!!
        checkBox.setOnCheckedChangeListener { compoundButton: CompoundButton, checked: Boolean ->
            if (checked) {
                GlobalKVCache.store("dontTellMe24h" to System.currentTimeMillis().toString())
            } else {
                GlobalKVCache.store("dontTellMe24h" to "")
            }
        }
        firstButton = findViewById(R.id.firstButton)!!
        secondButton = findViewById(R.id.secondButton)!!

    }

    fun setBigTitle(str: String) {
        titleTxt.text = str
    }

    fun setQuantity(str: String) {
        quantityTxt.text = str
    }

    fun setPrice(str: String) {
        priceTxt.text = str
    }

    fun setQuantitySymbol(str: String) {
        quantitySymbolTxt.text = str
    }

    fun setPriceSymbol(str: String) {
        priceSymbolTxt.text = str
    }

    fun setFirstButton(text: String, onclick: (() -> Unit)?) {
        firstButton.text = text
        firstButton.setOnClickListener { onclick?.invoke() }
    }

    fun setSecondButton(text: String, onclick: (() -> Unit)?) {
        secondButton.visibility = View.VISIBLE
        secondButton.text = text
        secondButton.setOnClickListener { onclick?.invoke() }
    }


}