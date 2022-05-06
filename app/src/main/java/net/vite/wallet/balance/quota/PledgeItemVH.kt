package net.vite.wallet.balance.quota

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.R
import net.vite.wallet.abi.datatypes.generated.Bytes32
import net.vite.wallet.account.AccountCenter
import net.vite.wallet.network.rpc.BuildInContractEncoder
import net.vite.wallet.network.rpc.PledgeInfo
import net.vite.wallet.utils.hexToBytes
import java.text.SimpleDateFormat

typealias onPledgeInfoRetrieve = (pledgeInfo: PledgeInfo) -> Unit

class PledgeItemVH(val view: View, val onPledgeInfoRetrieve: onPledgeInfoRetrieve) :
    RecyclerView.ViewHolder(view) {


    companion object {
        val dataFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        fun create(parent: ViewGroup, onPledgeInfoRetrieve: onPledgeInfoRetrieve): PledgeItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pledge_list, parent, false)
            return PledgeItemVH(view, onPledgeInfoRetrieve)
        }
    }

    private val benefitAddr = view.findViewById<TextView>(R.id.benefitAddr)
    private val pledgeAmount = view.findViewById<TextView>(R.id.pledgeAmount)
    private val pledgeTime = view.findViewById<TextView>(R.id.pledgeTime)
    private val pledgeTimeSuffix = view.findViewById<TextView>(R.id.pledgeTimeSuffix)
    private val retrieveQuota = view.findViewById<TextView>(R.id.retrieveQuota)

    fun bind(pledgeInfo: PledgeInfo?, listOfCancelPledging: ArrayList<String>) {
        pledgeInfo?.let { info ->
            benefitAddr.text =
                AccountCenter.getCurrentAccount()?.getAddressName(info.addr ?: "")?.let {
                    it + ": " + info.addr
                } ?: info.addr

            pledgeAmount.text = info.getAmountReadableText(1)
            pledgeTime.text = dataFormat.format(info.getMsWithdrawTime())
            pledgeTimeSuffix.setText(R.string.withdrawTime_suffix)

            if (listOfCancelPledging.find {
                    if (info.addr == null || info.amount == null) {
                        false
                    } else {
                        if (info.id == null) {
                            it == BuildInContractEncoder.encodeCancelPledge(info.addr, info.amount)
                        } else {
                            it == BuildInContractEncoder.encodeCancelPledgeNew(Bytes32(info.id.hexToBytes()))
                        }
                    }
                } != null) {
                retrieveQuota.setBackgroundResource(R.drawable.retrieve_quota_disable)
                retrieveQuota.setText(R.string.retrieve_pledge_viteing)
            } else if (System.currentTimeMillis() > info.getMsWithdrawTime()) {
                retrieveQuota.setBackgroundResource(R.drawable.retrieve_quota)
                retrieveQuota.setText(R.string.retrieve_quota)
                retrieveQuota.setOnClickListener {
                    onPledgeInfoRetrieve(pledgeInfo)
                }
            } else {
                retrieveQuota.setText(R.string.retrieve_quota)
                retrieveQuota.setBackgroundResource(R.drawable.retrieve_quota_disable)
                retrieveQuota.setOnClickListener {
                }
            }
        }

    }

}