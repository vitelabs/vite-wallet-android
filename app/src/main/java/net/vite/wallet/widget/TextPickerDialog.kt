package net.vite.wallet.widget

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_simple_text_picker.*
import net.vite.wallet.R

class TextPickerDialog : AlertDialog {
    val title: String
    val items: List<String>
    val onChooseListener: (name: String, dialog: AlertDialog) -> Unit

    constructor(
        context: Context,
        titleId: Int,
        items: List<String>,
        onChooseListener: (name: String, dialog: AlertDialog) -> Unit
    ) : super(context) {
        title = getContext().getString(titleId)
        this.items = items
        this.onChooseListener = onChooseListener
    }

    constructor(
        context: Context,
        title: String,
        items: List<String>,
        onChooseListener: (name: String, dialog: AlertDialog) -> Unit
    ) : super(context) {
        this.title = title
        this.items = items
        this.onChooseListener = onChooseListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_simple_text_picker)
        titleText.text = title
        accNameList.adapter = TextListAdapter(items) {
            onChooseListener.invoke(it, this)
        }
    }
}

private class TextListAdapter(val items: List<String>, val onClick: (name: String) -> Unit) :
    RecyclerView.Adapter<TextItemVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TextItemVH.create(parent, onClick)

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: TextItemVH, position: Int) {
        holder.acctext.text = items[position]
    }

}

private class TextItemVH(val view: View, val onClick: (name: String) -> Unit) : RecyclerView.ViewHolder(view) {
    private val container = view.findViewById<View>(R.id.accnameContainer)
    val acctext = view.findViewById<TextView>(R.id.accnameTxt)

    init {
        container.setOnClickListener {
            acctext.setTextColor(0x007aff)
            onClick(acctext.text.toString())
        }
    }

    companion object {
        fun create(parent: ViewGroup, onClick: (name: String) -> Unit): TextItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.dialog_simple_text_picker_item, parent, false)
            return TextItemVH(view, onClick)
        }
    }

}