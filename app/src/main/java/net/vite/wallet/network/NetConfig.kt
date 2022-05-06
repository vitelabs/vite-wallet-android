package net.vite.wallet.network

import net.vite.wallet.ENV_PRODUCT
import net.vite.wallet.ENV_TEST
import net.vite.wallet.ViteConfig

data class NetConfig(
    val remoteViteUrl: String,
    val remoteViteWsUrl: String,
    val remoteEthUrl: String,
    val remoteEthTransactionsUrl: String,
    val discoverPageUrl: String,
    val configBucketUrl: String,
    val vitexWsUrl: String,

    val remoteEtherscanPrefix: String,
    val viteGenesisUrlPrefix: String,
    val explorerUrlPrefix: String,
    val vitexH5UrlPrefix: String,
    val rewardQueryUrlPrefix: String,
    val rewardQueryUrl: String,

    val growthUrlBase: String,
    val vitexUrlBase: String,
    val gwUrlBase: String,
    val coinPurchaseUrlBase: String,
    val exchangeBannerUrl: String,
    val exchangeInviteUrl: String,
    val pushUploadUrl: String,
    val pushSubscribeUrl: String,
    val customViteUrl: String? = null,
    val customEthUrl: String? = null,
    val customPowUrl: String? = null
) {

    fun toReadableString() = "remoteViteUrl:$remoteViteUrl\n" +
            "remoteViteWsUrl:$remoteViteWsUrl\n" +
            "vitexWsUrl:$vitexWsUrl\n" +
            "remoteViteWsUrl:$remoteViteWsUrl\n" +
            "customViteUrl:$customViteUrl\n" +
            "customEthUrl:$customEthUrl\n"

    fun getViteNodeUrl() = customViteUrl?.takeIf { it.isNotEmpty() } ?: remoteViteUrl
    fun getEthNodeUrl() = customEthUrl?.takeIf { it.isNotEmpty() } ?: remoteEthUrl
    fun getPoWNodeUrl() = customPowUrl?.takeIf { it.isNotEmpty() } ?: remoteViteUrl

    companion object {
        fun getDefault(): NetConfig {
            val currentEnv = ViteConfig.get().currentEnv
            return NetConfig(
                remoteViteUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://node.vite.net/gvite"
                    ENV_TEST -> "http://148.70.30.139:48132"
                    else -> {
                        "https://node.vite.net/gvite"
                    }
                },
                remoteViteWsUrl = when (currentEnv) {
                    ENV_PRODUCT -> "wss://node.vite.net/gvite/ws"
                    ENV_TEST -> "wss://api.vitewallet.com/ws"
                    else -> {
                        "wss://node.vite.net/gvite/ws"
                    }
                },

                remoteEthUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://node.vite.net/eth/v3/caae2231051e46a1941f422df1fbcc94"
                    ENV_TEST -> "https://ropsten.infura.io/v3/44210a42716641f6a7c729313322929e"
                    else -> {
                        "https://node.vite.net/eth/v3/caae2231051e46a1941f422df1fbcc94"
                    }
                },
                remoteEthTransactionsUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://node.vite.net/etherscan"
                    ENV_TEST -> "https://node.vite.net/beta/etherscan"
                    else -> {
                        "https://node.vite.net/etherscan"
                    }
                },
                discoverPageUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://static.vite.net/testnet-vite-1257137467"
                    ENV_TEST -> "https://static.vite.net/testnet-vite-test-1257137467"
                    else -> {
                        "https://static.vite.net/testnet-vite-test-1257137467"
                    }
                },
                configBucketUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://static.vite.net/testnet-vite-test-1257137467"
                    ENV_TEST -> "https://static.vite.net/testnet-vite-test-1257137467"
                    else -> {
                        "https://static.vite.net/testnet-vite-test-1257137467"
                    }
                },
                vitexWsUrl = when (currentEnv) {
                    ENV_PRODUCT -> "wss://vitex.vite.net/websocket"
                    ENV_TEST -> "wss://vitex.vite.net/test/websocket"
                    else -> {
                        "wss://vitex.vite.net/websocket"
                    }
                },

                explorerUrlPrefix = "https://vitescan.io",

                viteGenesisUrlPrefix = when (currentEnv) {
                    ENV_PRODUCT -> "https://x.vite.net/balance?address="
                    ENV_TEST -> "https://x.vite.net/balance?address="
                    else -> {
                        "https://x.vite.net/balance?address="
                    }
                },

                rewardQueryUrlPrefix = when (currentEnv) {
                    ENV_PRODUCT -> "https://reward.vite.net"
                    ENV_TEST -> "https://test-reward.netlify.com"
                    else -> {
                        "https://reward.vite.net"
                    }
                },

                vitexH5UrlPrefix = when (currentEnv) {
                    ENV_PRODUCT -> "https://x.vite.net/mobiledex"
                    ENV_TEST -> "https://vite-wallet-test2.netlify.com/mobiledex"
                    else -> {
                        "https://x.vite.net/mobiledex"
                    }
                },

                remoteEtherscanPrefix = when (currentEnv) {
                    ENV_PRODUCT -> "https://etherscan.io"
                    ENV_TEST -> "https://ropsten.etherscan.io"
                    else -> {
                        "https://etherscan.io"
                    }
                },

                gwUrlBase = when (currentEnv) {
                    ENV_PRODUCT -> "https://gateway.vite.net"
                    ENV_TEST -> "http://132.232.60.116:8001"
                    else -> {
                        "https://gateway.vite.net"
                    }
                },
                coinPurchaseUrlBase = when (currentEnv) {
                    ENV_PRODUCT -> "https://api.vite.net/x/sale"
                    ENV_TEST -> "http://150.109.40.169:7070/test"
                    else -> {
                        "https://api.vite.net/x/sale"
                    }
                },
                vitexUrlBase = when (currentEnv) {
                    ENV_PRODUCT -> "https://api.vitex.net"
                    ENV_TEST -> "https://api.vitex.net/test"
                    else -> {
                        "https://api.vitex.net"
                    }
                },
                growthUrlBase = when (currentEnv) {
                    ENV_PRODUCT -> "https://growth.vite.net"
                    ENV_TEST -> "https://growth.vite.net/test"
                    else -> {
                        "https://growth.vite.net"
                    }
                },
                rewardQueryUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://reward.vite.net"
                    ENV_TEST -> "https://test-reward.netlify.com"
                    else -> {
                        "https://reward.vite.net"
                    }
                },
                exchangeBannerUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://config.vite.net"
                    ENV_TEST -> "http://129.226.74.210:1337"
                    else -> {
                        "https://config.vite.net"
                    }
                },
                exchangeInviteUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://app.vite.net/webview/vitex_invite_inner/index.html"
                    ENV_TEST -> "https://vite-wallet-test2.netlify.com/webview/vitex_invite_inner/index.html"
                    else -> {
                        "https://app.vite.net/webview/vitex_invite_inner/index.html"
                    }
                },

                pushUploadUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://api.vite.net/"
                    ENV_TEST -> "http://150.109.40.169:8086/test"
                    else -> "https://api.vite.net/"
                },

                pushSubscribeUrl = when (currentEnv) {
                    ENV_PRODUCT -> "https://api.vite.net/"
                    ENV_TEST -> "http://150.109.40.169:8079/"
                    else -> "https://api.vite.net/"
                }
            )
        }
    }
}
