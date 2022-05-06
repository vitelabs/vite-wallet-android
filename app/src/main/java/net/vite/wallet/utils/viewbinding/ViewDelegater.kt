package net.vite.wallet.utils.viewbinding

import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
class ViewDelegater<in T, out V>(
    private val useCache: Boolean = true,
    private val init: (T, KProperty<*>) -> V,
) {
    private object NONE

    private var view: Any? = NONE
    operator fun getValue(thisRef: T, property: KProperty<*>): V {
        if (view == NONE || !useCache) {
            view = init(thisRef, property)
        }
        return view as V
    }
}