package net.vite.wallet.utils.viewbinding

import android.app.Activity
import android.app.Dialog
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
fun <V : View> Fragment.findMyView(id: Int): ViewDelegater<Fragment, V> =
    ViewDelegater(false) { t: Fragment, kProperty ->
        this.requireView().findViewById(id) as V? ?: ohShitViewNotFound(id, kProperty)
    }

fun <V : View> Activity.findMyView(id: Int): ViewDelegater<Activity, V> = must(id) {
    findViewById(id)
}

fun <V : View> Dialog.findMyView(id: Int): ViewDelegater<Dialog, V> = must(id) {
    findViewById(id)
}


fun <V : View> RecyclerView.ViewHolder.findMyView(id: Int): ViewDelegater<RecyclerView.ViewHolder, V> =
    must(id) {
        itemView.findViewById(id)
    }

@Suppress("UNCHECKED_CAST")
private fun <T, V : View> must(id: Int, query: T.(Int) -> View?) =
    ViewDelegater { t: T, kProperty ->
        t.query(id) as V? ?: ohShitViewNotFound(id, kProperty)
    }

private fun ohShitViewNotFound(id: Int, kProperty: KProperty<*>): Nothing =
    throw IllegalStateException("View $id for ${kProperty.name} not found")

