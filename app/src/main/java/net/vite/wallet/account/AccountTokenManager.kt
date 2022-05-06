package net.vite.wallet.account

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.ExternalPriceCenter
import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.balance.DexAccountFundInfoPoll
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import net.vite.wallet.network.http.vitex.PinnedTokenInfo
import net.vite.wallet.network.http.vitex.TokenFamily
import net.vite.wallet.network.http.vitex.toCurrencyText
import net.vite.wallet.network.rpc.GetAccountFundInfoResp
import java.io.File
import java.math.BigDecimal


class AccountTokenManager(val accountProfile: AccountProfile) {
    val pinnedTokenInfoList = ArrayList<PinnedTokenInfo>()
    val pinnedExchangeTokenInfoList = ArrayList<PinnedTokenInfo>()

    fun getPinnedTokenCodes(): List<String> {
        return pinnedTokenInfoList.map { it.tokenCode }
    }

    fun getPinnedTokenCodeValues(): BigDecimal {
        return pinnedTokenInfoList.fold(BigDecimal.ZERO) { acc, s ->
            acc.add(
                TokenInfoCenter.getTokenInfoInCache(s.tokenCode)?.balanceValue() ?: BigDecimal.ZERO
            )
        }
    }

    fun getPinnedExchangeTokenCodeBtcValues(): BigDecimal {
        return pinnedTokenInfoList.fold(BigDecimal.ZERO) { acc, s ->
            val normalTokenInfo = TokenInfoCenter.getTokenInfoInCache(s.tokenCode)
            acc.add(normalTokenInfo?.balanceValueBTC() ?: BigDecimal.ZERO)
        }
    }

    fun loadLastPinnedExchangeTokenCodesSync() {
        pinnedExchangeTokenInfoList.clear()
        try {
            val lastPinnedStr =
                File("${accountProfile.getPrivateFileDir().absolutePath}/pinnedExchangeToken.json").readText()
            val type = object : TypeToken<List<PinnedTokenInfo>>() {}.type
            val list = Gson().fromJson<List<PinnedTokenInfo>>(lastPinnedStr, type)
            pinnedExchangeTokenInfoList.addAll(list)

            try {
                var latestIndex = 0
                TokenInfoCenter.permanentPinnedTokenInfos.filter {
                    it.family == TokenFamily.VITE
                }.forEach { permanentPinnedTokenInfo ->

                    val index =
                        pinnedExchangeTokenInfoList.indexOfFirst { it.tokenCode == permanentPinnedTokenInfo.tokenCode }
                    if (index == -1) {
                        pinnedExchangeTokenInfoList.add(latestIndex + 1, permanentPinnedTokenInfo)
                    } else {
                        latestIndex = index
                    }
                }
            } catch (e: Exception) {

            }
        } catch (e: Exception) {
            pinnedExchangeTokenInfoList.addAll(TokenInfoCenter.permanentPinnedTokenInfos.filter {
                it.family == TokenFamily.VITE
            })
        }
    }

    @SuppressLint("CheckResult")
    private fun asyncUpdatePinnedExchangeTokenInfoList() {
        Completable.fromCallable {
            try {
                val j = Gson().toJson(pinnedExchangeTokenInfoList)
                File("${accountProfile.getPrivateFileDir().absolutePath}/pinnedExchangeToken.json").writeText(
                    j
                )
            } catch (e: Exception) {
            }
        }.subscribeOn(Schedulers.single()).subscribe {
        }
    }


    fun unPinExchangeToken(tokenCode: String) {
        val index = pinnedExchangeTokenInfoList.indexOfFirst { it.tokenCode == tokenCode }
        if (index != -1) {
            pinnedExchangeTokenInfoList.removeAt(index)
        }
        asyncUpdatePinnedExchangeTokenInfoList()
    }

    fun pinExchangeToken(tokenInfo: NormalTokenInfo) {

        if (tokenInfo.family() != TokenFamily.VITE) {
            return
        }

        if (pinnedExchangeTokenInfoList.find { it.tokenCode == tokenInfo.tokenCode } != null) {
            return
        }

        val np = PinnedTokenInfo.from(tokenInfo)

        val lastIndex = pinnedExchangeTokenInfoList.indexOfLast {
            it.family == tokenInfo.family()
        }
        if (lastIndex == -1 || lastIndex == pinnedExchangeTokenInfoList.size - 1) {
            pinnedExchangeTokenInfoList.add(np)
        } else {
            pinnedExchangeTokenInfoList.add(lastIndex + 1, np)
        }
        asyncUpdatePinnedExchangeTokenInfoList()
    }

    fun pinExchangeToken(tokenInfo: PinnedTokenInfo) {

        if (tokenInfo.family != TokenFamily.VITE) {
            return
        }

        if (pinnedExchangeTokenInfoList.find { it.tokenCode == tokenInfo.tokenCode } != null) {
            return
        }

        val lastIndex = pinnedExchangeTokenInfoList.indexOfLast {
            it.family == tokenInfo.family
        }
        if (lastIndex == -1 || lastIndex == pinnedExchangeTokenInfoList.size - 1) {
            pinnedExchangeTokenInfoList.add(tokenInfo)
        } else {
            pinnedExchangeTokenInfoList.add(lastIndex + 1, tokenInfo)
        }
        asyncUpdatePinnedExchangeTokenInfoList()
    }

    fun loadLastPinnedTokenCodesSync() {
        pinnedTokenInfoList.clear()
        try {
            val lastPinnedStr =
                File("${accountProfile.getPrivateFileDir().absolutePath}/pinnedToken.json").readText()
            val type = object : TypeToken<List<PinnedTokenInfo>>() {}.type
            val list = Gson().fromJson<List<PinnedTokenInfo>>(lastPinnedStr, type)
            pinnedTokenInfoList.addAll(list)

            try {
                var latestIndex = 0
                TokenInfoCenter.permanentPinnedTokenInfos.forEach { permanentPinnedTokenInfo ->
                    val index =
                        pinnedTokenInfoList.indexOfFirst { it.tokenCode == permanentPinnedTokenInfo.tokenCode }
                    if (index == -1) {
                        pinnedTokenInfoList.add(latestIndex + 1, permanentPinnedTokenInfo)
                    } else {
                        latestIndex = index
                    }
                }
            } catch (e: Exception) {

            }

        } catch (e: Exception) {
            pinnedTokenInfoList.addAll(TokenInfoCenter.permanentPinnedTokenInfos)
        }
    }

    fun unPinToken(tokenCode: String) {
        val index = pinnedTokenInfoList.indexOfFirst { it.tokenCode == tokenCode }
        if (index != -1) {
            pinnedTokenInfoList.removeAt(index)
        }
        asyncUpdatePinnedTokenInfoList()
    }

    fun pinToken(tokenInfo: NormalTokenInfo) {
        if (pinnedTokenInfoList.find { it.tokenCode == tokenInfo.tokenCode } != null) {
            return
        }

        val np = PinnedTokenInfo.from(tokenInfo)

        val lastIndex = pinnedTokenInfoList.indexOfLast {
            it.family == tokenInfo.family()
        }
        if (lastIndex == -1 || lastIndex == pinnedTokenInfoList.size - 1) {
            pinnedTokenInfoList.add(np)
        } else {
            pinnedTokenInfoList.add(lastIndex + 1, np)
        }
        asyncUpdatePinnedTokenInfoList()
    }


    @SuppressLint("CheckResult")
    private fun asyncUpdatePinnedTokenInfoList() {
        Completable.fromCallable {
            try {
                val j = Gson().toJson(pinnedTokenInfoList)
                File("${accountProfile.getPrivateFileDir().absolutePath}/pinnedToken.json").writeText(
                    j
                )
            } catch (e: Exception) {
            }
        }.subscribeOn(Schedulers.single()).subscribe {
        }
    }


}
