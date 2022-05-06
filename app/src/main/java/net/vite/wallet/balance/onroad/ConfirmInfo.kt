package net.vite.wallet.balance.onroad

import androidx.annotation.Keep

@Keep
data class ConfirmInfo(
    var outputCommitMappings: List<OutputCommitMapping>? = null,
    var isConfirm: Boolean? = null,
    var txId: Int? = null,
    var confirmType: ConfirmType? = null,
    var curHeight: Long? = null,
    var beginHeight: Long? = null,
    var message: String? = null
)

