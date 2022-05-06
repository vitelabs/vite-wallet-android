package net.vite.wallet.network.rpc

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.vite.wallet.account.EmptyAddressException
import net.vite.wallet.loge
import net.vite.wallet.logt
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.utils.ZeroHash
import net.vite.wallet.utils.hexToBytes
import net.vite.wallet.utils.toHex
import org.vitelabs.mobile.Address
import org.vitelabs.mobile.Mobile

fun getRemoteViteUrl(): String {
    return NetConfigHolder.netConfig.getViteNodeUrl()
}

fun logi(msg: String) {
    net.vite.wallet.logi(msg, "vitenetwork")
}

fun logw(msg: String, throwable: Throwable? = null) {
    net.vite.wallet.logw("$msg ${throwable?.message ?: "unknown error"}", "vitenetwork")
}

object SyncNet {

    fun getBlocksByAccAddr(addr: String?, index: Int, count: Int): RpcResponse<List<AccountBlock>> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val jsonstr = client.getBlocksByAccAddr(addr, index.toLong(), count.toLong())
            logi("getBlocksByAccAddr$jsonstr")
            RpcResponse(
                Gson().fromJson(
                    jsonstr,
                    object : TypeToken<ArrayList<AccountBlock>>() {}.type
                )
            )
        } catch (t: Throwable) {
            RpcResponse(null, t)
        }
    }

    fun getAccountFundInfo(
        addr: String,
        tti: String
    ): RpcResponse<Map<String, GetAccountFundInfoResp>> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val blocks = client.getAccountFundInfo(addr, tti)
            RpcResponse(
                Gson().fromJson(
                    blocks,
                    object : TypeToken<Map<String, GetAccountFundInfoResp>>() {}.type
                )
            )

        } catch (e: Exception) {
            RpcResponse(null, e)
        }
    }


    fun getPledgeList(addr: String, index: Long, count: Long): RpcResponse<PledgeInfoResp> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val json = client.getPledgeList(addr, index, count)
            RpcResponse(Gson().fromJson(json, PledgeInfoResp::class.java))
        } catch (e: Exception) {
            logw("getPledgeList", e)
            RpcResponse(null, e)
        }
    }


    fun getOnroadBlocksByAddress(
        addr: String,
        index: Int,
        count: Int
    ): RpcResponse<List<AccountBlock>> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val jsonstr = client.getOnroadBlocksByAddress(addr, index.toLong(), count.toLong())
//            logi("getOnroadBlocksByAddress")
            RpcResponse(
                Gson().fromJson(
                    jsonstr,
                    object : TypeToken<ArrayList<AccountBlock>>() {}.type
                )
            )
        } catch (e: Exception) {
            logw("getOnroadBlocksByAddress", e)
            RpcResponse(null, e)
        }
    }


    fun dexIsInviteCodeValid(code: String): RpcResponse<Boolean> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val boolStr = client.dexIsInviteCodeValid(code)
            logi("dexIsInviteCodeValid")
            RpcResponse(boolStr == "true")
        } catch (e: Exception) {
            logw("dexIsInviteCodeValid", e)
            RpcResponse(null, e)
        }
    }

    fun dexGetInviteCodeBinding(addr: String): RpcResponse<UInt> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val uint32Str = client.dexGetInviteCodeBinding(addr)
            logi("dexGetInviteCodeBinding")
            RpcResponse(uint32Str.toUInt())
        } catch (e: Exception) {
            logw("dexGetInviteCodeBinding", e)
            RpcResponse(null, e)
        }
    }

    fun getLatestBlock(addr: String): RpcResponse<AccountBlock> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val jsonstr = client.getLatestBlock(addr)
            logi("getLatestBlock")
            RpcResponse(Gson().fromJson(jsonstr, AccountBlock::class.java) ?: AccountBlock())
        } catch (e: Exception) {
            logw("getLatestBlock", e)
            RpcResponse(null, e)
        }
    }

    fun getPowNonce(difficulty: String, preHashHex: String?, addr: String): RpcResponse<String> {
        return try {
            val preBytes = Address(addr).bytes.toMutableList()
            preBytes.addAll((preHashHex ?: ZeroHash).hexToBytes().toList())

            val client = Mobile.dial(NetConfigHolder.netConfig.getPoWNodeUrl())
            val nonce =
                client.getPowNonce(difficulty, Mobile.hash256(preBytes.toByteArray()).toHex())

            logi("getPowNonce")
            if (nonce.isNullOrBlank()) {
                RpcResponse(null, Exception("nonce blank"))
            } else {
                RpcResponse(nonce)
            }
        } catch (e: Exception) {
            logw("getLatestBlock", e)
            RpcResponse(null, e)
        }
    }


    fun calcPoWDifficulty(calcPoWDifficultyParams: CalcPowDifficultyReq): RpcResponse<CalcPowDifficultyResp> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val difficultyStr = client.calcPoWDifficulty(Gson().toJson(calcPoWDifficultyParams))
            logi("getPowNonce")
            val c = Gson().fromJson<CalcPowDifficultyResp>(
                difficultyStr,
                CalcPowDifficultyResp::class.java
            )
            RpcResponse(c, null)
        } catch (e: Exception) {
            logw("getPowNonce", e)
            RpcResponse(null, e)
        }
    }


    fun getPledgeQuota(addr: String?): RpcResponse<GetPledgeQuotaResp> {
        return try {
            addr?.let {
                val client = Mobile.dial(getRemoteViteUrl())
                val jsonstr = client.getPledgeQuota(it)
//                logi("getPledgeQuota")
                RpcResponse(Gson().fromJson(jsonstr, GetPledgeQuotaResp::class.java))
            } ?: kotlin.run {
                throw EmptyAddressException()
            }
        } catch (e: Exception) {
            logw("getPledgeQuota", e)
            RpcResponse(null, e)
        }

    }

    fun getBlocksByHashInToken(
        addr: String,
        hash: String,
        tti: String,
        count: Int
    ): RpcResponse<List<AccountBlock>> {

        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val blocks = client.getBlocksByHashInToken(addr, hash, tti, count.toLong())
            logi("getBlocksByHashInToken")
            RpcResponse(
                Gson().fromJson(blocks, object : TypeToken<ArrayList<AccountBlock>>() {}.type)
                    ?: emptyList()
            )
        } catch (e: Exception) {
            logw("getBlocksByHashInToken", e)
            RpcResponse(null, e)
        }
    }

    fun getBlocksByHash(
        addr: String,
        hash: String,
        count: Int
    ): RpcResponse<List<AccountBlock>> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val blocks = client.getBlocksByHash(addr, hash, count.toLong())
            logt("getBlocksByHash$blocks")
            RpcResponse(
                Gson().fromJson(blocks, object : TypeToken<ArrayList<AccountBlock>>() {}.type)
                    ?: emptyList()
            )
        } catch (t: Throwable) {
            RpcResponse(null, t)
        }
    }

    fun getPledgeData(addr: String): RpcResponse<String> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val data = client.getPledgeData(addr)
            logi("getPledgeData$data")
            if (data.isNullOrBlank()) {
                RpcResponse(null, Exception("pledge blank"))
            } else {
                RpcResponse(data)
            }
        } catch (t: Throwable) {
            RpcResponse(null, t)
        }
    }

    fun sendRawTx(accBlockStr: String): RpcResponse<Unit> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            client.sendRawTx(accBlockStr)
            logi("sendRawTx success")
            RpcResponse(Unit)
        } catch (e: Exception) {
            logw("sendRawTx", e)
            RpcResponse(null, RpcException.tryMake(e))
        }
    }

    fun calcQuotaRequired(req: CalcQuotaRequiredReq): RpcResponse<CalcQuotaRequiredResp> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val quotaRequired = client.calcQuotaRequired(Gson().toJson(req))
            logi("calcQuotaRequired")
            RpcResponse(Gson().fromJson(quotaRequired, CalcQuotaRequiredResp::class.java))
        } catch (e: Exception) {
            logw("calcQuotaRequired", e)
            RpcResponse(null, e)
        }
    }


    fun getAccountByAccAddr(addr: String): RpcResponse<AccountBalanceInfo> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val balanceInfoStr = client.getAccountByAccAddr(addr)
//            logi("getAccountByAccAddr")
            RpcResponse(Gson().fromJson(balanceInfoStr, AccountBalanceInfo::class.java))
        } catch (e: Exception) {
            logw("getAccountByAccAddr", e)
            RpcResponse(null, e)
        }
    }


    fun getOnroadInfoByAddress(addr: String): RpcResponse<AccountBalanceInfo> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val balanceInfoStr = client.getOnroadInfoByAddress(addr)
//            logi("getOnroadInfoByAddress")
            RpcResponse(Gson().fromJson(balanceInfoStr, AccountBalanceInfo::class.java))
        } catch (e: Exception) {
            logw("getOnroadInfoByAddress", e)
            RpcResponse(null, e)
        }
    }

    fun getVoteData(name: String, gid: String = DefaultGid): RpcResponse<String> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val jsonstr = client.getVoteData(gid, name)
            logi("getVoteData$jsonstr")
            RpcResponse(resp = jsonstr)
        } catch (e: Exception) {
            loge(e)
            RpcResponse("", e)
        }
    }

    fun getCancelVoteData(gid: String = DefaultGid): RpcResponse<String> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val jsonstr = client.getCancelVoteData(gid)
            logi("getCancelVoteData$jsonstr")
            RpcResponse(resp = jsonstr)
        } catch (e: Exception) {
            loge(e)
            RpcResponse("", e)
        }
    }

    fun dexHasStackedForSVIP(address: String): RpcResponse<String> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val jsonstr = client.dexHasStackedForSVIP(address)
            logi("dexHasStackedForSVIP$jsonstr")
            RpcResponse(resp = jsonstr)
        } catch (e: Exception) {
            loge(e)
            RpcResponse("", e)
        }
    }

    fun dexHasStackedForVIP(address: String): RpcResponse<String> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val jsonstr = client.dexHasStackedForVIP(address)
            logi("dexHasStackedForVIP$jsonstr")
            RpcResponse(resp = jsonstr)
        } catch (e: Exception) {
            loge(e)
            RpcResponse("", e)
        }
    }

    fun dexGetVIPStakeInfoList(
        address: String,
        pageIndex: Long,
        pageSize: Long
    ): RpcResponse<String> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val jsonstr = client.dexGetVIPStakeInfoList(address, pageIndex, pageSize)
            logi("dexGetVIPStakeInfoList$jsonstr")
            RpcResponse(resp = jsonstr)
//            RpcResponse(Gson().fromJson(jsonstr, CalcQuotaRequiredResp::class.java))
        } catch (e: Exception) {
            loge(e)
            RpcResponse("", e)
        }
    }

    fun dexGetPlaceOrderInfo(
        address: String,
        tradeTokenId: String,
        quoteTokenId: String,
        side: Boolean
    ): RpcResponse<String> {
        return try {
            val client = Mobile.dial(getRemoteViteUrl())
            val jsonstr = client.dexGetPlaceOrderInfo(address, tradeTokenId, quoteTokenId, side)
            logi("dexGetPlaceOrderInfo$jsonstr")
            RpcResponse(resp = jsonstr)
//            RpcResponse(Gson().fromJson(jsonstr, CalcQuotaRequiredResp::class.java))
        } catch (e: Exception) {
            loge(e)
            RpcResponse("", e)
        }
    }

}