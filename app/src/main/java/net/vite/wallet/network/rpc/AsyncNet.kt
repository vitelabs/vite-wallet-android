package net.vite.wallet.network.rpc

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import net.vite.wallet.account.EmptyAddressException
import net.vite.wallet.network.http.vitex.VIPStakeInfoList
import org.vitelabs.mobile.Mobile

const val DefaultGid = "00000000000000000001"

@Keep
data class RpcResponse<T>(val resp: T?, val throwable: Throwable? = null) {
    fun success() = throwable == null
}


object AsyncNet {

    private fun logd(msg: String) {
//        net.vite.wallet.logi(msg, "AsyncNet network")
    }

    private fun loge(throwable: Throwable) {
//        net.vite.wallet.loge(throwable, "AsyncNet network")
    }


    fun getBlocksByHash(
        addr: String?,
        hash: String,
        count: Int
    ): Observable<RpcResponse<List<AccountBlock>>> {
        return Observable.fromCallable {
            try {
                addr?.let {
                    val client = Mobile.dial(getRemoteViteUrl())
                    val blocks = client.getBlocksByHash(it, hash, count.toLong())
                    logd("getBlocksByHash$blocks")
                    RpcResponse(
                        Gson().fromJson(
                            blocks,
                            object : TypeToken<ArrayList<AccountBlock>>() {}.type
                        )
                            ?: emptyList<AccountBlock>()
                    )
                } ?: kotlin.run {
                    throw EmptyAddressException()
                }
            } catch (e: Exception) {
                loge(e)
                RpcResponse(emptyList<AccountBlock>(), e)
            }
        }
    }


    fun getBlocksByHashInToken(
        addr: String?,
        hash: String,
        tti: String,
        count: Int
    ): Observable<RpcResponse<List<AccountBlock>>> {
        return Observable.fromCallable {
            try {
                addr?.let {
                    val client = Mobile.dial(getRemoteViteUrl())
                    val blocks = client.getBlocksByHashInToken(it, hash, tti, count.toLong())
                    logi("getBlocksByHashInToken")
                    RpcResponse(
                        Gson().fromJson(
                            blocks,
                            object : TypeToken<ArrayList<AccountBlock>>() {}.type
                        )
                            ?: emptyList<AccountBlock>()
                    )
                } ?: kotlin.run {
                    throw EmptyAddressException()
                }
            } catch (e: Exception) {
                logw("getBlocksByHashInToken", e)
                RpcResponse(emptyList<AccountBlock>(), e)
            }
        }
    }


    fun getSnapshotBlockHeight(): Observable<RpcResponse<String>> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(getRemoteViteUrl())
                val height = client.snapshotChainHeight
//                logi("snapshotChainHeight")
                RpcResponse(height)
            } catch (e: Exception) {
                logw("snapshotChainHeight", e)
                RpcResponse("", e)
            }
        }
    }

    fun getPledgeList(
        addr: String?,
        index: Long,
        count: Long
    ): Observable<RpcResponse<PledgeInfoResp>> {
        return Observable.fromCallable {
            try {
                addr?.let {
                    val client = Mobile.dial(getRemoteViteUrl())
                    val json = client.getPledgeList(it, index, count)
                    logi("getPledgeList")
                    RpcResponse(Gson().fromJson(json, PledgeInfoResp::class.java))
                } ?: kotlin.run {
                    throw EmptyAddressException()
                }
            } catch (e: Exception) {
                logw("getPledgeList", e)
                RpcResponse(PledgeInfoResp(), e)
            }
        }
    }


    fun getOnroadInfoByAddress(addr: String?): Observable<RpcResponse<AccountBalanceInfo>> {
        return Observable.fromCallable {
            try {
                addr?.let {
                    val client = Mobile.dial(getRemoteViteUrl())
                    val balanceInfoStr = client.getOnroadInfoByAddress(it)
                    logd("getOnroadInfoByAddress$balanceInfoStr")
                    RpcResponse(
                        Gson().fromJson(balanceInfoStr, AccountBalanceInfo::class.java)
                            ?: AccountBalanceInfo()
                    )
                } ?: kotlin.run {
                    throw EmptyAddressException()
                }
            } catch (e: Exception) {
                loge(e)
                RpcResponse(AccountBalanceInfo(), EmptyAddressException())
            }

        }
    }


    fun getPledgeQuotaData(addr: String): Observable<RpcResponse<out String>> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(getRemoteViteUrl())
                val data = client.getPledgeData(addr)
                logd("getPledgeData$data")
                if (data.isNullOrBlank()) {
                    RpcResponse(null, Exception("pledge blank"))
                } else {
                    RpcResponse(data)
                }
            } catch (t: Throwable) {
                RpcResponse(null, t)
            }
        }
    }


    fun getCandidateList(gid: String = DefaultGid): Observable<RpcResponse<List<CandidateItem>>> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(getRemoteViteUrl())
                val jsonstr = client.getCandidateList(gid)
                logi("getCandidateList")
                RpcResponse(
                    resp = Gson().fromJson(
                        jsonstr,
                        object : TypeToken<ArrayList<CandidateItem>>() {}.type
                    )
                        ?: emptyList<CandidateItem>()
                )
            } catch (e: Exception) {
                logw("getCandidateList", e)
                RpcResponse(emptyList<CandidateItem>(), e)
            }
        }
    }

    fun getVoteInfo(addr: String?, gid: String = DefaultGid): Observable<RpcResponse<VoteInfo>> {
        return Observable.fromCallable {
            try {
                addr?.let {
                    val client = Mobile.dial(getRemoteViteUrl())
                    val jsonstr = client.getVoteInfo(gid, it)
                    logi("getVoteInfo")
                    RpcResponse(
                        resp = Gson().fromJson(jsonstr, VoteInfo::class.java)
                    )
                } ?: kotlin.run {
                    throw EmptyAddressException()
                }
            } catch (e: Exception) {
                logw("getVoteInfo", e)
                RpcResponse(VoteInfo(), e)
            }
        }
    }

    fun getTokenMintage(tokenId: String): Observable<RpcResponse<out ViteChainTokenInfo>> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(getRemoteViteUrl())
                val jsonstr = client.getTokenMintage(tokenId)
                val tokenInfo = Gson().fromJson(jsonstr, ViteChainTokenInfo::class.java)
                RpcResponse(resp = tokenInfo)
            } catch (e: Exception) {
                loge(e)
                RpcResponse(null, e)
            }
        }
    }

    fun getVoteData(name: String, gid: String = DefaultGid): Observable<RpcResponse<String>> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(getRemoteViteUrl())
                val jsonstr = client.getVoteData(gid, name)
                RpcResponse(resp = jsonstr)
            } catch (e: Exception) {
                loge(e)
                RpcResponse("", e)
            }
        }
    }

    fun getAccountFundInfo(
        addr: String,
        tti: String
    ): Observable<RpcResponse<out String>> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(getRemoteViteUrl())
                val blocks = client.getAccountFundInfo(addr, tti)
//            logi("getAccountFundInfo")
                RpcResponse(resp = blocks)
            } catch (e: Exception) {
//            logw("getAccountFundInfo", e) // just warnning {"code":-37013,"message":"fund user doesn't exist."}
                RpcResponse(null, e)
            }
        }
    }

    fun dexHasStackedForVIP(address: String): Observable<RpcResponse<String>> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(getRemoteViteUrl())
                val jsonstr = client.dexHasStackedForVIP(address)
                logi("dexHasStackedForVIP$jsonstr")
                RpcResponse(resp = jsonstr)
            } catch (e: Exception) {
                net.vite.wallet.loge(e)
                RpcResponse("", e)
            }
        }
    }

    fun dexGetPlaceOrderInfo(
        address: String,
        tradeTokenId: String,
        quoteTokenId: String,
        side: Boolean
    ): Observable<RpcResponse<String>> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(getRemoteViteUrl())
                val jsonstr = client.dexGetPlaceOrderInfo(address, tradeTokenId, quoteTokenId, side)
                logi("dexGetPlaceOrderInfo$jsonstr")
                RpcResponse(resp = jsonstr)
//            RpcResponse(Gson().fromJson(jsonstr, CalcQuotaRequiredResp::class.java))
            } catch (e: Exception) {
                net.vite.wallet.loge(e)
                RpcResponse("", e)
            }
        }
    }


    fun dexGetVIPStakeInfoList(
        address: String,
        pageIndex: Long,
        pageSize: Long
    ): Observable<RpcResponse<VIPStakeInfoList>> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(getRemoteViteUrl())
                val jsonstr = client.dexGetVIPStakeInfoList(address, pageIndex, pageSize)
                logi("dexGetVIPStakeInfoList$jsonstr")
                val resp = Gson().fromJson(jsonstr, VIPStakeInfoList::class.java)
                RpcResponse(resp = resp)
            } catch (e: Exception) {
                net.vite.wallet.loge(e)
                RpcResponse(null, e)
            }
        }
    }
}
