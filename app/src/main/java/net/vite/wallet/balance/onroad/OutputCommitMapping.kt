package net.vite.wallet.balance.onroad

import androidx.annotation.Keep


/**
 * {
 * "commit": "087df32304c5d4ae8b2af0bc31e700019d722910ef87dd4eec3197b80b207e3045",
 * "output": {
 * "commit": "087df32304c5d4ae8b2af0bc31e700019d722910ef87dd4eec3197b80b207e3045",
 * "height": "2",
 * "is_coinbase": true,
 * "key_id": "0300000000000000000000000100000000",
 * "lock_height": "5",
 * "mmr_index": null,
 * "n_child": 1,
 * "root_key_id": "0200000000000000000000000000000000",
 * "status": "Unspent",
 * "tx_log_entry": 1,
 * "value": "60000000000"
 * }* 					}
 */
@Keep
data class OutputData(
    var commit: String? = null,
    var height: String? = null,
    var is_coinbase: Boolean? = null,
    var key_id: String? = null,
    var lock_height: String? = null,
    var mmr_index: String? = null,
    var n_child: Int? = null,
    var root_key_id: String? = null,
    var status: String? = null,
    var tx_log_entry: Int? = null,
    var value: String? = null
)


@Keep
data class OutputCommitMapping(
    var commit: String? = null,
    var output: OutputData? = null
)

