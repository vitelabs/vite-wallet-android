package net.vite.wallet.contacts

import android.annotation.SuppressLint
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.account.AccountCenter
import java.io.File

@Keep
data class Contact(
    val address: String,
    val name: String,
    val createTime: Long,
    val familyName: String
) {
    companion object {
        val FAMILT_NAME_VITE = "VITE"
        val FAMILT_NAME_ETH = "ETH"
        val FAMILT_NAME_GRIN = "GRIN"

        val NowValidArray = arrayOf(FAMILT_NAME_VITE, FAMILT_NAME_ETH, FAMILT_NAME_GRIN)

        fun getIndex(name: String): Int {
            return when (name) {
                FAMILT_NAME_VITE -> 0
                FAMILT_NAME_ETH -> 1
                FAMILT_NAME_GRIN -> 2
                else -> 0
            }
        }
    }
}


object ContactCenter {

    var contacts = ArrayList<Contact>()

    fun loadAllContactsSync() {
        val temp = ArrayList<Contact>()
        try {
            AccountCenter.getCurrentAccount()?.getPrivateFileDir()?.let {
                val contactFile = File("${it.absoluteFile}/contact.json")
                val s = contactFile.readText()
                val type = object : TypeToken<List<Contact>>() {}.type
                temp.addAll(Gson().fromJson<List<Contact>>(s, type))
            }
        } catch (e: Exception) {

        }

        contacts.clear()
        contacts.addAll(temp)
    }

    fun loadAllContacts(): Completable {
        return Completable.fromCallable {
            loadAllContactsSync()
        }.subscribeOn(Schedulers.io())
    }

    fun add(c: Contact) {
        contacts.add(c)
        updateToFile()
    }


    fun delete(address: String) {
        val index = contacts.indexOfFirst { it.address == address }
        if (index != -1) {
            contacts.removeAt(index)
            updateToFile()
        }
    }


    @SuppressLint("CheckResult")
    private fun updateToFile() {
        Completable.fromCallable {
            try {
                AccountCenter.getCurrentAccount()?.getPrivateFileDir()?.let {
                    val contactFile = File("${it.absoluteFile}/contact.json")
                    contactFile.writeText(Gson().toJson(contacts))
                }
            } catch (e: Exception) {
            }
        }.subscribeOn(Schedulers.single()).subscribe { }
    }


}