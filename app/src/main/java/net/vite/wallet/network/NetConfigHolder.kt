package net.vite.wallet.network

import android.annotation.SuppressLint
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import net.vite.wallet.ENV_PRODUCT
import net.vite.wallet.ViteConfig
import net.vite.wallet.exchange.net.ws.ExchangeWsHolder
import net.vite.wallet.network.http.HttpClient
import net.vite.wallet.network.http.ViteNormalBaseResponse
import net.vite.wallet.utils.WhiteHostManager
import okhttp3.Request

@Keep
data class RemoteNetConfig(
    val groupName: String?,
    val hostNameList: List<String>?,
    val inWhite: Boolean?
)

@Keep
data class RemoteNetConfigMap(
    val WALLETAPI: RemoteNetConfig?,
    val ETH_NODE: RemoteNetConfig?,
    val DISCOVERYPAGE: RemoteNetConfig?,
    val GATEWAY: RemoteNetConfig?,
    val GRIN_WALLET_HTTP: RemoteNetConfig?,
    val ETH_EXPLORER: RemoteNetConfig?,
    val MVITEX: RemoteNetConfig?,
    val DEXPUSHSERVER: RemoteNetConfig?,
    val VITEX_API: RemoteNetConfig?,
    val WALLETCONFIG: RemoteNetConfig?,
    val EXPLORER: RemoteNetConfig?,
    val GROWTH: RemoteNetConfig?,
    val BUYCOIN: RemoteNetConfig?,
    val WALLETWSAPI: RemoteNetConfig?,
    val WALLET_CONFIG_NEW: RemoteNetConfig?
) {

    fun remoteViteUrl() = simpleGet(WALLETAPI)
    fun remoteViteWsUrl() = simpleGet(WALLETWSAPI)
    fun remoteEthUrl() = simpleGet(ETH_NODE)
    fun discoverPageUrl() = simpleGet(DISCOVERYPAGE)
    fun gwUrlBase() = simpleGet(GATEWAY)
    fun grinWalletHttpUrl() = simpleGet(GRIN_WALLET_HTTP)
    fun remoteEtherscanPrefix() = simpleGet(ETH_EXPLORER)
    fun vitexH5UrlPrefix() = simpleGet(MVITEX)
    fun vitexWsUrl() = simpleGet(DEXPUSHSERVER)
    fun vitexUrl() = simpleGet(VITEX_API)

    fun configBucketUrl() = simpleGet(WALLETCONFIG)

    fun explorerUrlPrefix() = simpleGet(EXPLORER)
    fun coinPurchaseUrlBase() = simpleGet(BUYCOIN)
    fun growthUrlBase() = simpleGet(GROWTH)
    fun walletConfigNew() = simpleGet(WALLET_CONFIG_NEW)

    private fun simpleGet(config: RemoteNetConfig?): String? {
        return kotlin.runCatching {
            val url = config?.hostNameList?.get(0)
            if (config?.inWhite == true && url != null) {
                WhiteHostManager.addWhiteUrl(url)
            }
            url
        }.getOrNull()
    }
}

data class CustomNodeSetting(
    val viteNodes: List<String> = emptyList(),
    val ethNodes: List<String> = emptyList(),
    val selectViteNode: String? = null,
    val selectEthNode: String? = null
)

data class CustomPowSetting(
    val powNodes: List<String> = emptyList(),
    val selectPowNode: String? = null
)

object NetConfigHolder {
    @Volatile
    lateinit var netConfig: NetConfig

    private val gson = Gson()

    fun init() {
        netConfig = NetConfig.getDefault()
        tryUseCustomNode()
    }

    fun deleteViteUrl(url: String) {
        val customNode = readCustomNodes()?.let {
            if (url == it.selectViteNode) {
                netConfig = netConfig.copy(
                    customEthUrl = null
                )
                it.copy(
                    selectEthNode = null,
                    ethNodes = it.viteNodes.toMutableList().apply { remove(url) }
                )
            } else {
                it.copy(ethNodes = it.viteNodes.toMutableList().apply { remove(url) })
            }
        } ?: CustomNodeSetting()
        ViteConfig.get().kvstore.edit().putString("node_custom_setting", gson.toJson(customNode))
            .apply()
    }

    fun deleteEthUrl(url: String) {
        val customNode = readCustomNodes()?.let {
            if (url == it.selectEthNode) {
                netConfig = netConfig.copy(
                    customEthUrl = null
                )
                it.copy(
                    selectEthNode = null,
                    ethNodes = it.ethNodes.toMutableList().apply { remove(url) }
                )
            } else {
                it.copy(ethNodes = it.ethNodes.toMutableList().apply { remove(url) })
            }
        } ?: CustomNodeSetting()
        ViteConfig.get().kvstore.edit().putString("node_custom_setting", gson.toJson(customNode))
            .apply()
    }

    fun addCustomViteNode(url: String) {
        val customNode = readCustomNodes()?.let {
            if (it.viteNodes.indexOf(url) == -1) {
                it.copy(viteNodes = it.viteNodes.toMutableList().apply { add(url) })
            } else {
                it
            }
        } ?: CustomNodeSetting(viteNodes = listOf(url))
        ViteConfig.get().kvstore.edit().putString("node_custom_setting", gson.toJson(customNode))
            .apply()
    }

    fun switchViteNode(url: String) {
        val customNode = readCustomNodes() ?: return
        ViteConfig.get().kvstore.edit()
            .putString("node_custom_setting", gson.toJson(customNode.copy(selectViteNode = url)))
            .apply()
        tryUseCustomNode()
    }

    fun addCustomEthNode(url: String) {
        val customNode = readCustomNodes()?.let {
            if (it.ethNodes.indexOf(url) == -1) {
                it.copy(ethNodes = it.ethNodes.toMutableList().apply { add(url) })
            } else {
                it
            }
        } ?: CustomNodeSetting(ethNodes = listOf(url))

        ViteConfig.get().kvstore.edit().putString("node_custom_setting", gson.toJson(customNode))
            .apply()
    }

    fun switchCustomEthNode(url: String) {
        val customNode = readCustomNodes() ?: return
        ViteConfig.get().kvstore.edit()
            .putString("node_custom_setting", gson.toJson(customNode.copy(selectEthNode = url)))
            .apply()
        tryUseCustomNode()
    }

    fun readCustomNodes(): CustomNodeSetting? {
        val customString = ViteConfig.get().kvstore.getString("node_custom_setting", "")
        if (customString.isNullOrEmpty()) return null
        return kotlin.runCatching { gson.fromJson(customString, CustomNodeSetting::class.java) }
            .getOrNull()
    }


    fun tryUseCustomNode() {
        val customNode = readCustomNodes()
        customNode?.selectEthNode?.let {
            netConfig = netConfig.copy(customEthUrl = it)
        }
        customNode?.selectViteNode?.let {
            netConfig = netConfig.copy(customViteUrl = it)
        }
    }

    fun tryUseCustomPow() {
        val customNode = readCustomPoWNodes()
        customNode?.selectPowNode?.let {
            netConfig = netConfig.copy(customPowUrl = it)
        }
    }

    private fun pull(url: String): Observable<RemoteNetConfigMap> {
        return Observable.fromCallable {
            try {
                val resp = HttpClient.normalOkClient.newCall(
                    Request.Builder()
                        .url(url)
                        .get()
                        .build()
                ).execute()

                val type = object : TypeToken<ViteNormalBaseResponse<RemoteNetConfigMap>>() {}.type

                val r = gson.fromJson<ViteNormalBaseResponse<RemoteNetConfigMap>>(
                    resp.body()?.string(),
                    type
                )

                r.data!!

            } catch (e: Exception) {
                kotlin.runCatching {
                    Thread.sleep(3 * 1000)
                }
                throw Exception("$url $e")
            }
        }.subscribeOn(Schedulers.newThread())
    }

    @SuppressLint("CheckResult")
    fun pullRemoteConfig() {
        if (ViteConfig.get().currentEnv != ENV_PRODUCT) {
            return
        }

        val bootDns1 = pull("https://config.vite.net/dns/hostips")
        val bootDns2 = pull("https://config.vite.net/dns/hostips")

        Observable.amb(listOf(bootDns1, bootDns2)).subscribe({
            val newConfig = NetConfig(
                remoteViteUrl = it.remoteViteUrl() ?: netConfig.remoteViteUrl,
                remoteViteWsUrl = it.remoteViteWsUrl() ?: netConfig.remoteViteWsUrl,
                remoteEthUrl = it.remoteEthUrl() ?: netConfig.remoteEthUrl,
                remoteEthTransactionsUrl = netConfig.remoteEthTransactionsUrl,
                discoverPageUrl = it.discoverPageUrl() ?: netConfig.discoverPageUrl,
                configBucketUrl = it.configBucketUrl() ?: netConfig.configBucketUrl,
                vitexWsUrl = it.vitexWsUrl() ?: netConfig.vitexWsUrl,
                remoteEtherscanPrefix = it.remoteEtherscanPrefix()
                    ?: netConfig.remoteEtherscanPrefix,
                viteGenesisUrlPrefix = netConfig.viteGenesisUrlPrefix,
                explorerUrlPrefix = it.explorerUrlPrefix() ?: netConfig.explorerUrlPrefix,
                vitexH5UrlPrefix = it.vitexH5UrlPrefix() ?: netConfig.vitexH5UrlPrefix,
                rewardQueryUrlPrefix = netConfig.rewardQueryUrlPrefix,
                growthUrlBase = it.growthUrlBase() ?: netConfig.growthUrlBase,
                vitexUrlBase = it.vitexUrl() ?: netConfig.vitexUrlBase,
                gwUrlBase = it.gwUrlBase() ?: netConfig.gwUrlBase,
                coinPurchaseUrlBase = it.coinPurchaseUrlBase() ?: netConfig.coinPurchaseUrlBase,
                rewardQueryUrl = netConfig.rewardQueryUrl,
                exchangeBannerUrl = it.walletConfigNew() ?: netConfig.exchangeBannerUrl,
                exchangeInviteUrl = netConfig.exchangeInviteUrl,
                pushUploadUrl = netConfig.pushUploadUrl,
                pushSubscribeUrl = netConfig.pushSubscribeUrl
            )

            val oldVitexWsUrl = netConfig.vitexWsUrl
            val oldRemoteViteWsUrl = netConfig.remoteViteWsUrl
            netConfig = newConfig
            tryUseCustomNode()
            tryUseCustomPow()

            if (newConfig.vitexWsUrl != oldVitexWsUrl) {
                Thread {
                    kotlin.runCatching {
                        ExchangeWsHolder.reconnect()
                    }
                }.start()
            }
            if (newConfig.remoteViteWsUrl != oldRemoteViteWsUrl) {
                //todo remoteViteWsUrl reconnect
            }
        }, {
        })
    }

    fun readCustomPoWNodes(): CustomPowSetting? {
        val customString = ViteConfig.get().kvstore.getString("custom_pow_url_setting", "")
        if (customString.isNullOrEmpty()) return null
        return kotlin.runCatching { gson.fromJson(customString, CustomPowSetting::class.java) }
            .getOrNull()
    }

    fun deletePowUrl(url: String) {
        val customNode = readCustomPoWNodes()?.let {
            if (url == it.selectPowNode) {
                netConfig = netConfig.copy(
                    customPowUrl = null
                )
                it.copy(
                    selectPowNode = null,
                    powNodes = it.powNodes.toMutableList().apply { remove(url) }
                )
            } else {
                it.copy(powNodes = it.powNodes.toMutableList().apply { remove(url) })
            }
        } ?: CustomNodeSetting()

        ViteConfig.get().kvstore.edit()
            .putString("custom_pow_url_setting", gson.toJson(customNode))
            .apply()
    }

    fun switchCustomPowNode(ip: String) {
        val customNode = readCustomPoWNodes() ?: return
        ViteConfig.get().kvstore.edit()
            .putString("custom_pow_url_setting", gson.toJson(customNode.copy(selectPowNode = ip)))
            .apply()
        tryUseCustomPow()
    }

    fun addCustomPowNode(url: String) {
        val customNode = readCustomPoWNodes()?.let {
            if (it.powNodes.indexOf(url) == -1) {
                it.copy(powNodes = it.powNodes.toMutableList().apply { add(url) })
            } else {
                it
            }
        } ?: CustomPowSetting(powNodes = listOf(url))
        ViteConfig.get().kvstore.edit().putString("custom_pow_url_setting", gson.toJson(customNode))
            .apply()
    }
}
