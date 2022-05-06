package net.vite.wallet.dexassets.utils

import androidx.core.content.edit
import net.vite.wallet.ViteConfig
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class BooleanPreferenceProperty(val key: String) : ReadWriteProperty<Any, Boolean> {
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        ViteConfig.get().kvstore.edit { putBoolean(key, value) }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return ViteConfig.get().kvstore.getBoolean(key, true)
    }
}

class IntPreferenceProperty(val key: String, val defaultValue: Int = -1) :
    ReadWriteProperty<Any, Int> {
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        ViteConfig.get().kvstore.edit { putInt(key, value) }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return ViteConfig.get().kvstore.getInt(key, defaultValue)
    }
}