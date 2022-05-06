package net.vite.wallet.balance.walletconnect.taskdetail

import net.vite.wallet.balance.walletconnect.taskdetail.buildin.*
import net.vite.wallet.constants.*
import net.vite.wallet.network.http.vitex.NormalTokenInfo
import org.walletconnect.Session

val BlockTypeToDecoder = mapOf(
    BlockDetailTypeRegister to SBPRegister(),
    BlockDetailTypeRegisterUpdate to SBPUpdate(),
    BlockDetailTypeCancelRegister to SBPDeregister(),
    BlockDetailTypeExtractReward to SBPExtractReward(),
//    BlockDetailTypeUpdateSBPRewardWithdrawAddress to UpdateSBPRewardWithdrawAddress(),

    BlockDetailTypeVote to Vote(),
    BlockDetailTypeCancelVote to VoteRevoke(),

    BlockDetailTypePledge to QuotaAcquire(),
    BlockDetailTypeCancelPledge to QuotaRegainStakefor(),
    BlockDetailTypeCancelPledgeNew to QuotaRegainStakeforNew(),
    BlockDetailTypeDexCancelStakeById to DexCancelStakeById(),

    BlockDetailTypeMintage to MintageTokenIssuance(),
//    BlockDetailTypeCancelMintage to ,
    BlockDetailTypeMintageReissue to MintageReIssue(),
//    BlockDetailTypeMintageBurn to ,
    BlockDetailTypeMintageTransferOwner to MintageTransferOwner(),
    BlockDetailTypeMintageChangeToNonIssuable to MintageChangeToNonIssuable(),


    BlockDetailTypeDexDeposit to DexDeposit(),
    BlockDetailTypeDexWithdraw to DexWithdraw(),
    BlockDetailTypeDexPost to DexPost(),
    BlockDetailTypeDexNewInviter to DexNewInviter(),
    BlockDetailTypeDexBindInviter to DexBindInviter(),
    BlockDetailTypeDexTransferTokenOwner to DexFundTransferTokenOwner(),
    BlockDetailNewMarket to DexNewMarket(),
    BlockDetailTypeMarketConfig to DexMarketConfig(),


    BlockDetailTypeDexCancel to DexCancel(),

    BlockDetailTypeStakeAsMine to DexStakingAsMining(),
    BlockDetailTypeBecomeVIP to DexBecomeVIP(),
    BlockDetailTypeDexLockVxForDividend to DexLockVxForDividend()

)


internal fun Session.MethodCall.SendTransaction.parseInnerContractToConfirmInfo(
    abiDataParseResult: List<DataDecodeResult>,
    normalTokenInfo: NormalTokenInfo,
    type: Int
): WCConfirmInfo {
    return BlockTypeToDecoder[type]!!.decode(this, abiDataParseResult, normalTokenInfo)
}