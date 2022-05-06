package net.vite.wallet.dexassets.utils

import net.vite.wallet.TokenInfoCenter
import net.vite.wallet.balance.DexAccountFundInfoPoll
import net.vite.wallet.balance.poll.ViteAccountInfoPoll
import java.math.BigDecimal

object DexAssetCalcUtils {
    fun getAllAssetValueBTC(): BigDecimal {
        val dex = dexValueBTC()
        val wallet = walletValueBTC()
        return dex.add(wallet)
    }

    fun getAllAssetValueLegend(): BigDecimal {
        val dex = dexValueLegend()
        val wallet = walletValueLegend()
        return dex.add(wallet)
    }

    fun dexValueLegend() = DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
        ?.values?.fold(BigDecimal.ZERO) { acc, item ->
            val total =
                TokenInfoCenter.getTokenInfoIncacheByTokenAddr(item.tokenInfo.tokenId ?: "")
                    ?.dexAllBalanceValue()
                    ?: BigDecimal.ZERO
            acc.add(total)
        } ?: BigDecimal.ZERO

    fun walletValueLegend() =
        ViteAccountInfoPoll.myLatestViteAccountInfo()?.balance?.tokenBalanceInfoMap?.values?.fold(
            BigDecimal.ZERO
        ) { acc, item ->
            val total =
                TokenInfoCenter.getTokenInfoIncacheByTokenAddr(
                    item.viteChainTokenInfo?.tokenId ?: ""
                )?.balanceValue()
                    ?: BigDecimal.ZERO
            acc.add(total)
        } ?: BigDecimal.ZERO

    fun walletValueBTC() =
        ViteAccountInfoPoll.myLatestViteAccountInfo()?.balance?.tokenBalanceInfoMap?.values?.fold(
            BigDecimal.ZERO
        ) { acc, item ->
            val total =
                TokenInfoCenter.getTokenInfoIncacheByTokenAddr(
                    item.viteChainTokenInfo?.tokenId ?: ""
                )?.balanceValueBTC() ?: BigDecimal.ZERO
            acc.add(total)
        } ?: BigDecimal.ZERO

    fun dexValueBTC(): BigDecimal {
        val dex = DexAccountFundInfoPoll.myLatestViteAccountFundInfo()
            ?.values?.fold(BigDecimal.ZERO) { acc, item ->
                val total =
                    TokenInfoCenter.getTokenInfoIncacheByTokenAddr(
                        item.tokenInfo.tokenId ?: ""
                    )?.dexAllBalanceValueBTC()
                        ?: BigDecimal.ZERO
                acc.add(total)
            } ?: BigDecimal.ZERO
        return dex
    }
}