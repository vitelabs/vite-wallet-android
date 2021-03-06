package net.vite.wallet.constants

import android.graphics.Color
import androidx.annotation.Keep
import com.google.gson.Gson


const val BlockDetailTypeReceive = 998
const val BlockDetailTypeSend = 999

const val BlockDetailTypeRegister = 1000
const val BlockDetailTypeRegisterUpdate = 1001
const val BlockDetailTypeCancelRegister = 1002
const val BlockDetailTypeExtractReward = 1003
const val BlockDetailTypeUpdateSBPRewardWithdrawAddress = 1009
const val BlockDetailTypeDexCancelStakeById = 1010

const val BlockDetailTypeVote = 1004
const val BlockDetailTypeCancelVote = 1005

const val BlockDetailTypePledge = 1006
const val BlockDetailTypeCancelPledge = 1007
const val BlockDetailTypeCancelPledgeNew = 1008

const val BlockDetailTypeMintage = 1300
const val BlockDetailTypeCancelMintage = 1301
const val BlockDetailTypeMintageReissue = 1302
const val BlockDetailTypeMintageBurn = 1303
const val BlockDetailTypeMintageTransferOwner = 1304
const val BlockDetailTypeMintageChangeToNonIssuable = 1305

const val BlockDetailTypeDexDeposit = 1100
const val BlockDetailTypeDexWithdraw = 1101
const val BlockDetailTypeDexPost = 1102
const val BlockDetailTypeDexCancel = 1103
const val BlockDetailTypeDexNewInviter = 1104
const val BlockDetailTypeDexBindInviter = 1105
const val BlockDetailTypeDexTransferTokenOwner = 1106
const val BlockDetailNewMarket = 1107
const val BlockDetailTypeMarketConfig = 1108
const val BlockDetailTypeStakeAsMine = 1109
const val BlockDetailTypeBecomeVIP = 1110
const val BlockDetailTypeDexLockVxForDividend = 1111

const val BlockDetailTypeContractCall = 1999


const val DexContractAddress = "vite_0000000000000000000000000000000000000006e82b8ba657"
const val DexCancelContractAddress = "vite_00000000000000000000000000000000000000079710f19dc7"

//hard fork function
val DataPrefixToBlockType = mapOf(
    "4b8d9eae" to BlockDetailTypeDexCancelStakeById,
    "43075fbf" to BlockDetailTypeRegister,
    "1448e38e" to BlockDetailTypeRegisterUpdate,
    "ae0167df" to BlockDetailTypeCancelRegister,
    "dfe845eb" to BlockDetailTypeExtractReward,
//    "863f8813" to BlockDetailTypeUpdateSBPRewardWithdrawAddress, //not use now

    "42f2eee4" to BlockDetailTypeVote,
    "30b3f2ad" to BlockDetailTypeCancelVote,

    "d2fd352d" to BlockDetailTypePledge,
    "f19e3c6b" to BlockDetailTypeCancelPledge,
    "b7fb0919" to BlockDetailTypeCancelPledgeNew,

    "5bf06ed0" to BlockDetailTypeMintage,
    "7d925ef1" to BlockDetailTypeCancelMintage,
    "7dab20ec" to BlockDetailTypeMintageReissue,
    "dd28f156" to BlockDetailTypeMintageTransferOwner,
    "bfe44e34" to BlockDetailTypeMintageChangeToNonIssuable,

    "319e46dd" to BlockDetailTypeDexDeposit,
    "00365d82" to BlockDetailTypeDexWithdraw,
    "0454f5ef" to BlockDetailTypeDexPost,
    "2205eb17" to BlockDetailTypeDexCancel,
    "d8088efc" to BlockDetailTypeDexNewInviter,
    "f19ffbcb" to BlockDetailTypeDexBindInviter,
    "67e2522f" to BlockDetailTypeDexTransferTokenOwner,
    "fe5b5288" to BlockDetailNewMarket,
    "ad503cfa" to BlockDetailTypeMarketConfig,
    "76db6cdd" to BlockDetailTypeStakeAsMine,
    "21d811bc" to BlockDetailTypeBecomeVIP,
    "e7f03bc7" to BlockDetailTypeDexLockVxForDividend
)

val BlockTypeToContactAddress = mapOf(

    BlockDetailTypeRegister to "vite_0000000000000000000000000000000000000004d28108e76b",
    BlockDetailTypeRegisterUpdate to "vite_0000000000000000000000000000000000000004d28108e76b",
    BlockDetailTypeCancelRegister to "vite_0000000000000000000000000000000000000004d28108e76b",
    BlockDetailTypeExtractReward to "vite_0000000000000000000000000000000000000004d28108e76b",
    BlockDetailTypeUpdateSBPRewardWithdrawAddress to "vite_0000000000000000000000000000000000000004d28108e76b",

    BlockDetailTypeVote to "vite_0000000000000000000000000000000000000004d28108e76b",
    BlockDetailTypeCancelVote to "vite_0000000000000000000000000000000000000004d28108e76b",

    BlockDetailTypePledge to "vite_0000000000000000000000000000000000000003f6af7459b9",
    BlockDetailTypeCancelPledge to "vite_0000000000000000000000000000000000000003f6af7459b9",
    BlockDetailTypeCancelPledgeNew to "vite_0000000000000000000000000000000000000003f6af7459b9",

    BlockDetailTypeMintage to "vite_000000000000000000000000000000000000000595292d996d",
    BlockDetailTypeCancelMintage to "vite_000000000000000000000000000000000000000595292d996d",
    BlockDetailTypeMintageReissue to "vite_000000000000000000000000000000000000000595292d996d",
    BlockDetailTypeMintageBurn to "vite_000000000000000000000000000000000000000595292d996d",
    BlockDetailTypeMintageTransferOwner to "vite_000000000000000000000000000000000000000595292d996d",
    BlockDetailTypeMintageChangeToNonIssuable to "vite_000000000000000000000000000000000000000595292d996d",


    BlockDetailTypeDexDeposit to DexContractAddress,
    BlockDetailTypeDexWithdraw to DexContractAddress,
    BlockDetailTypeDexPost to DexContractAddress,
    BlockDetailTypeDexNewInviter to DexContractAddress,
    BlockDetailTypeDexBindInviter to DexContractAddress,
    BlockDetailTypeDexTransferTokenOwner to DexContractAddress,
    BlockDetailNewMarket to DexContractAddress,
    BlockDetailTypeMarketConfig to DexContractAddress,
    BlockDetailTypeStakeAsMine to DexContractAddress,
    BlockDetailTypeBecomeVIP to DexContractAddress,
    BlockDetailTypeDexLockVxForDividend to DexContractAddress,
    BlockDetailTypeDexCancelStakeById to DexContractAddress,


    BlockDetailTypeDexCancel to DexCancelContractAddress
)


val BuildInContactAbiMap = mapOf(
    BlockDetailTypeRegister to "  {\"type\":\"function\",\"name\":\"RegisterSBP\", \"inputs\":[{\"name\":\"sbpName\",\"type\":\"string\"},{\"name\":\"blockProducingAddress\",\"type\":\"address\"},{\"name\":\"rewardWithdrawAddress\",\"type\":\"address\"}]}",
    BlockDetailTypeRegisterUpdate to "  {\"type\":\"function\",\"name\":\"UpdateSBPBlockProducingAddress\", \"inputs\":[{\"name\":\"sbpName\",\"type\":\"string\"},{\"name\":\"blockProducingAddress\",\"type\":\"address\"}]}",
    BlockDetailTypeCancelRegister to "{\"type\":\"function\",\"name\":\"RevokeSBP\",\"inputs\":[{\"name\":\"sbpName\",\"type\":\"string\"}]}",
    BlockDetailTypeExtractReward to "{\"type\":\"function\",\"name\":\"WithdrawSBPReward\",\"inputs\":[{\"name\":\"sbpName\",\"type\":\"string\"},{\"name\":\"receiveAddress\",\"type\":\"address\"}]}",
    BlockDetailTypeUpdateSBPRewardWithdrawAddress to "{\"type\":\"function\",\"name\":\"UpdateSBPRewardWithdrawAddress\", \"inputs\":[{\"name\":\"sbpName\",\"type\":\"string\"},{\"name\":\"rewardWithdrawAddress\",\"type\":\"address\"}]}",
    BlockDetailTypeDexCancelStakeById to "{\"type\":\"function\",\"name\":\"CancelStakeById\", \"inputs\":[{\"name\":\"id\",\"type\":\"bytes32\"}]}",

    BlockDetailTypeVote to "{\"type\":\"function\",\"name\":\"VoteForSBP\", \"inputs\":[{\"name\":\"sbpName\",\"type\":\"string\"}]}",
    BlockDetailTypeCancelVote to "{\"type\":\"function\",\"name\":\"CancelSBPVoting\",\"inputs\":[]}",

    BlockDetailTypePledge to "{\"type\":\"function\",\"name\":\"StakeForQuota\", \"inputs\":[{\"name\":\"beneficiary\",\"type\":\"address\"}]}",
    BlockDetailTypeCancelPledge to "{\"type\":\"function\",\"name\":\"CancelStake\",\"inputs\":[{\"name\":\"beneficiary\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"}]}",
    BlockDetailTypeCancelPledgeNew to "{\"type\":\"function\",\"name\":\"CancelQuotaStaking\",\"inputs\":[{\"name\":\"id\",\"type\":\"bytes32\"}]}",

    BlockDetailTypeMintage to "{\"type\":\"function\",\"name\":\"IssueToken\",\"inputs\":[{\"name\":\"isReIssuable\",\"type\":\"bool\"},{\"name\":\"tokenName\",\"type\":\"string\"},{\"name\":\"tokenSymbol\",\"type\":\"string\"},{\"name\":\"totalSupply\",\"type\":\"uint256\"},{\"name\":\"decimals\",\"type\":\"uint8\"},{\"name\":\"maxSupply\",\"type\":\"uint256\"},{\"name\":\"isOwnerBurnOnly\",\"type\":\"bool\"}]}",
    BlockDetailTypeMintageReissue to "{\"type\":\"function\",\"name\":\"ReIssue\",\"inputs\":[{\"name\":\"tokenId\",\"type\":\"tokenId\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"receiveAddress\",\"type\":\"address\"}]}",
    BlockDetailTypeMintageBurn to "{\"type\":\"function\",\"name\":\"Burn\",\"inputs\":[]}",
    BlockDetailTypeMintageTransferOwner to "{\"type\":\"function\",\"name\":\"TransferOwnership\",\"inputs\":[{\"name\":\"tokenId\",\"type\":\"tokenId\"},{\"name\":\"newOwner\",\"type\":\"address\"}]}",
    BlockDetailTypeMintageChangeToNonIssuable to "{\"type\":\"function\",\"name\":\"DisableReIssue\",\"inputs\":[{\"name\":\"tokenId\",\"type\":\"tokenId\"}]}",


    BlockDetailTypeDexDeposit to "{\"type\":\"function\",\"name\":\"Deposit\", \"inputs\":[]}",
    BlockDetailTypeDexWithdraw to "{\"type\":\"function\",\"name\":\"Withdraw\", \"inputs\":[{\"name\":\"token\",\"type\":\"tokenId\"},{\"name\":\"amount\",\"type\":\"uint256\"}]}",
    BlockDetailTypeDexPost to "{\"type\":\"function\",\"name\":\"PlaceOrder\", \"inputs\":[{\"name\":\"tradeToken\",\"type\":\"tokenId\"}, {\"name\":\"quoteToken\",\"type\":\"tokenId\"}, {\"name\":\"side\", \"type\":\"bool\"}, {\"name\":\"orderType\", \"type\":\"uint8\"}, {\"name\":\"price\", \"type\":\"string\"}, {\"name\":\"quantity\", \"type\":\"uint256\"}]}",
    BlockDetailTypeDexCancel to "{\"type\":\"function\",\"name\":\"CancelOrder\", \"inputs\":[{\"name\":\"orderId\",\"type\":\"bytes\"}]}",
    BlockDetailTypeDexNewInviter to "{\"type\":\"function\",\"name\":\"CreateNewInviter\", \"inputs\":[]}",
    BlockDetailTypeDexBindInviter to "{\"type\":\"function\",\"name\":\"BindInviteCode\", \"inputs\":[{\"name\":\"code\",\"type\":\"uint32\"}]}",
    BlockDetailTypeDexTransferTokenOwner to "{\"type\":\"function\",\"name\":\"TransferTokenOwnership\", \"inputs\":[{\"name\":\"token\",\"type\":\"tokenId\"}, {\"name\":\"newOwner\",\"type\":\"address\"}]}",
    BlockDetailNewMarket to "{\"type\":\"function\",\"name\":\"OpenNewMarket\", \"inputs\":[{\"name\":\"tradeToken\",\"type\":\"tokenId\"}, {\"name\":\"quoteToken\",\"type\":\"tokenId\"}]}",
    BlockDetailTypeMarketConfig to "{\"type\":\"function\",\"name\":\"MarketAdminConfig\", \"inputs\":[{\"name\":\"operationCode\",\"type\":\"uint8\"},{\"name\":\"tradeToken\",\"type\":\"tokenId\"},{\"name\":\"quoteToken\",\"type\":\"tokenId\"},{\"name\":\"marketOwner\",\"type\":\"address\"},{\"name\":\"takerFeeRate\",\"type\":\"int32\"},{\"name\":\"makerFeeRate\",\"type\":\"int32\"},{\"name\":\"stopMarket\",\"type\":\"bool\"}]}",

    BlockDetailTypeStakeAsMine to "{\"type\":\"function\",\"name\":\"StakeForMining\", \"inputs\":[{\"name\":\"actionType\",\"type\":\"uint8\"}, {\"name\":\"amount\",\"type\":\"uint256\"}]}",
    BlockDetailTypeBecomeVIP to "{\"type\":\"function\",\"name\":\"StakeForVIP\", \"inputs\":[{\"name\":\"actionType\",\"type\":\"uint8\"}]}",

    BlockDetailTypeDexLockVxForDividend to "{\"type\":\"function\",\"name\":\"LockVxForDividend\", \"inputs\":[{\"name\":\"actionType\",\"type\":\"uint8\"},{\"name\":\"amount\",\"type\":\"uint256\"}]}"
)


val BuildInTransferDesc = mapOf(
    "Transfer" to "{\"function\":{\"name\":{\"base\":\"Transfer\",\"zh\":\"??????\"}},\"inputs\":[{\"name\":{\"base\":\"Transaction Address\",\"zh\":\"????????????\"}},{\"name\":{\"base\":\"Amount\",\"zh\":\"????????????\"},\"style\":{\"textColor\":\"007AFF\",\"backgroundColor\":\"007AFF0F\"}},{\"name\":{\"base\":\"Comment\",\"zh\":\"????????????\"}}]}",
    "CrossChainTransfer" to "{\"function\":{\"name\":{\"base\":\"Cross-Chain Transfer\",\"zh\":\"????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Amount\",\"zh\":\"????????????\"},\"style\":{\"textColor\":\"007AFF\",\"backgroundColor\":\"007AFF0F\"}},{\"name\":{\"base\":\"Fee\",\"zh\":\"?????????\"}},{\"name\":{\"base\":\"Receive Address\",\"zh\":\"????????????\"}}]}"
)


@Keep
data class WcDesc(
    val function: WcNameItem,
    val inputs: List<WcNameItem>
) {
    companion object {
        fun create(json: String): WcDesc {
            return Gson().fromJson<WcDesc>(json, WcDesc::class.java)
        }
    }

    fun title() = function.name?.getTitle() ?: ""
}

@Keep
data class WcNameItem(
    val name: WcLangItem? = null,
    val style: WcStyle? = null
)


@Keep
data class WcLangItem(
    val base: String? = null,
    val zh: String? = null
) {
    fun getTitle() = base ?: ""
}

@Keep
data class WcStyle(
    val textColor: String? = null, // RGBA
    val backgroundColor: String? = null // RGBA
) {
    private fun convertRGBAtoARGB(rgba: String?): Int? {
        if (rgba == null) {
            return null
        }

        if (rgba.length == 6) {
            return Color.parseColor("#$rgba")
        }
        if (rgba.length == 8) {
            return Color.parseColor("#${rgba.substring(6, 8)}${rgba.substring(0, 6)}")
        }
        return null
    }

    fun getTextColor() = convertRGBAtoARGB(textColor)
    fun getBackgroundColor() = convertRGBAtoARGB(backgroundColor)
}


//
//val BuildInContactDesc = mapOf(
//    BlockDetailTypeRegister to "{\"function\":{\"name\":{\"base\":\"Register SBP\",\"zh\":\"?????? SBP\"}},\"inputs\":[{\"name\":{\"base\":\"SBP Name\",\"zh\":\"SBP ??????\"}},{\"name\":{\"base\":\"Amount\",\"zh\":\"????????????\"}}]}",
//    BlockDetailTypeRegisterUpdate to "{\"function\":{\"name\":{\"base\":\"Update SBP\",\"zh\":\"?????? SBP\"}},\"inputs\":[{\"name\":{\"base\":\"Updated Address\",\"zh\":\"????????????\"}}]}",
//    BlockDetailTypeCancelRegister to "{\"function\":{\"name\":{\"base\":\"Deregister SBP\",\"zh\":\"?????? SBP ??????\"}},\"inputs\":[{\"name\":{\"base\":\"SBP Name\",\"zh\":\"SBP??????\"}}]}",
//    BlockDetailTypeExtractReward to "{\"function\":{\"name\":{\"base\":\"Extract Rewards\",\"zh\":\"????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Receive Address\",\"zh\":\"????????????\"}}]}",
//
//    BlockDetailTypeVote to "{\"function\":{\"name\":{\"base\":\"Vote\",\"zh\":\"??????\"}},\"inputs\":[{\"name\":{\"base\":\"SBP Candidate\",\"zh\":\"??????????????????\"}},{\"name\":{\"base\":\"Votes\",\"zh\":\"?????????\"}}]}",
//    BlockDetailTypeCancelVote to "{\"function\":{\"name\":{\"base\":\"Revoke Vote\",\"zh\":\"????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Votes Revoked\",\"zh\":\"???????????????\"}}]}",
//
//    BlockDetailTypePledge to "{\"function\":{\"name\":{\"base\":\"Acquire Quota\",\"zh\":\"????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Amount\",\"zh\":\"????????????\"}},{\"name\":{\"base\":\"Beneficiary Address\",\"zh\":\"??????????????????\"}}]}",
//    BlockDetailTypeCancelPledge to "{\"function\":{\"name\":{\"base\":\"Regain Stake for Quota\",\"zh\":\"??????????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Amount\",\"zh\":\"??????????????????\"}}]}",
//
//    BlockDetailTypeMintage to "{\"function\":{\"name\":{\"base\":\"Token Issuance\",\"zh\":\"??????\"}},\"inputs\":[{\"name\":{\"base\":\"Token Name\",\"zh\":\"????????????\"}},{\"name\":{\"base\":\"Token Symbol\",\"zh\":\"????????????\"}},{\"name\":{\"base\":\"Total Supply\",\"zh\":\"????????????\"}},{\"name\":{\"base\":\"Decimals\",\"zh\":\"????????????\"}},{\"name\":{\"base\":\"Issuance Fee\",\"zh\":\"?????????\"}}]}",
//    BlockDetailTypeMintageReissue to "{\"type\":\"function\",\"name\":\"Issue\",\"inputs\":[{\"name\":\"tokenId\",\"type\":\"tokenId\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"beneficial\",\"type\":\"address\"}]}",
//    BlockDetailTypeMintageBurn to "{\"type\":\"function\",\"name\":\"Burn\",\"inputs\":[]}",
//    BlockDetailTypeMintageTransferOwner to "{\"type\":\"function\",\"name\":\"TransferOwner\",\"inputs\":[{\"name\":\"tokenId\",\"type\":\"tokenId\"},{\"name\":\"newOwner\",\"type\":\"address\"}]}",
//    BlockDetailTypeMintageChangeToNonIssuable to "{\"type\":\"function\",\"name\":\"ChangeTokenType\",\"inputs\":[{\"name\":\"tokenId\",\"type\":\"tokenId\"}]}",
//
//    BlockDetailTypeDexDeposit to "{\"function\":{\"name\":{\"base\":\"Dex Deposit\",\"zh\":\"???????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Amount\",\"zh\":\"????????????\"},\"style\":{\"textColor\":\"007AFF\",\"backgroundColor\":\"007AFF0F\"}}]}",
//    BlockDetailTypeDexWithdraw to "{\"function\":{\"name\":{\"base\":\"Dex Withdraw\",\"zh\":\"???????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Amount\",\"zh\":\"????????????\"},\"style\":{\"textColor\":\"007AFF\",\"backgroundColor\":\"007AFF0F\"}}]}",
//    BlockDetailTypeDexPost to "{\"function\":{\"name\":{\"base\":\"Dex Post\",\"zh\":\"???????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Order Type\",\"zh\":\"????????????\"},\"style\":{\"textColor\":\"5BC500\",\"backgroundColor\":\"007AFF0F\"}},{\"name\":{\"base\":\"Market\",\"zh\":\"??????\"}},{\"name\":{\"base\":\"Price\",\"zh\":\"??????\"}},{\"name\":{\"base\":\"Amount\",\"zh\":\"??????\"}}]}",
//    BlockDetailTypeDexNewInviter to "{\"function\":{\"name\":{\"base\":\"Create Referral Code\",\"zh\":\"???????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Current Address\",\"zh\":\"????????????\"}},{\"name\":{\"base\":\"Cost\",\"zh\":\"????????????\"}}]}",
//    BlockDetailTypeDexBindInviter to "{\"function\":{\"name\":{\"base\":\"Use Referral Code\",\"zh\":\"???????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Beneficiary Address\",\"zh\":\"??????????????????\"}},{\"name\":{\"base\":\"Referral Code\",\"zh\":\"?????????\"}}]}",
//
//    BlockDetailTypeDexCancel to "{\"function\":{\"name\":{\"base\":\"Dex Cancel\",\"zh\":\"???????????????\"}},\"inputs\":[{\"name\":{\"base\":\"Order ID\",\"zh\":\"?????? ID\"}},{\"name\":{\"base\":\"Order Type\",\"zh\":\"????????????\"},\"style\":{\"textColor\":\"5BC500\",\"backgroundColor\":\"007AFF0F\"}},{\"name\":{\"base\":\"Market\",\"zh\":\"??????\"}},{\"name\":{\"base\":\"Price\",\"zh\":\"??????\"}}]}"
//
//
//)
//
//
//fun getBuildInContractDesc(type: Int): WcDesc? {
//    return Gson().fromJson<WcDesc>(BuildInContactDesc[type], WcDesc::class.java)
//}