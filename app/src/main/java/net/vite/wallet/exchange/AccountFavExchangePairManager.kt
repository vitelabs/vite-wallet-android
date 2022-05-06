package net.vite.wallet.exchange

import android.annotation.SuppressLint
import androidx.annotation.Keep
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.account.AccountProfile
import net.vite.wallet.network.http.vitex.TickerStatistics
import java.io.File

class AccountFavExchangePairManager(val accountProfile: AccountProfile) {
    private val favsymbolsFile = File(accountProfile.getPrivateFileDir(), "favsymbols.json")
    private val searchHistoryFile = File(accountProfile.getPrivateFileDir(), "searchHistory.json")

    @Keep
    private data class SymbolsPairList(
        val symbols: List<String>
    )

    val favPairs = ArrayList<String>()
    var favHasLoaded = false
    val historyPairs = ArrayList<String>()
    var historyHasLoaded = false
        private set

    fun addSearchHistory(pairSymbol: String) {
        if (historyPairs.indexOf(pairSymbol) != -1) {
            return
        }
        historyPairs.add(pairSymbol)
        asyncStoreToFile(searchHistoryFile, historyPairs)
    }


    fun deleteAllSearchHistory() {
        historyPairs.clear()
        asyncStoreToFile(searchHistoryFile, historyPairs)
    }

    fun deleteFav(pairSymbol: String) {
        val index = favPairs.indexOf(pairSymbol)
        if (index == -1) {
            return
        }
        favPairs.removeAt(index)
        asyncStoreToFile(favsymbolsFile, favPairs)
    }

    fun addFav(pairSymbol: String) {
        if (favPairs.indexOf(pairSymbol) != -1) {
            return
        }
        if (TickerStatistics.retrievePairsFromSymbolOrNull(pairSymbol) != null) {
            favPairs.add(pairSymbol)
            asyncStoreToFile(favsymbolsFile, favPairs)
        }
    }

    fun getSearchHistory(): ArrayList<String> {
        if (historyHasLoaded) {
            return historyPairs
        }
        return try {
            historyPairs.clear()
            historyPairs.addAll(
                Gson().fromJson(
                    searchHistoryFile.readText(),
                    SymbolsPairList::class.java
                ).symbols
            )
            historyHasLoaded = true
            historyPairs
        } catch (e: Exception) {
            historyPairs
        }

    }

    fun getFav(): ArrayList<String> {
        if (favHasLoaded) {
            return favPairs
        }
        return try {
            favPairs.clear()
            favPairs.addAll(
                Gson().fromJson(
                    favsymbolsFile.readText(),
                    SymbolsPairList::class.java
                ).symbols
            )
            favHasLoaded = true
            favPairs
        } catch (e: Exception) {
            favPairs
        }

    }


    @SuppressLint("CheckResult")
    private fun asyncStoreToFile(file: File, list: List<String>) {
        Completable.fromCallable {
            file.writeText(
                Gson().toJson(
                    SymbolsPairList(
                        symbols = list
                    )
                )
            )
        }.subscribeOn(Schedulers.single()).subscribe {
        }
    }

}
