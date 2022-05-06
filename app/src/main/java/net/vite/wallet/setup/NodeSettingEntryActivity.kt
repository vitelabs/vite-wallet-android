package net.vite.wallet.setup

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_language_list.textViewtitle
import kotlinx.android.synthetic.main.activity_node_setting_entry.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.network.NetConfigHolder

class NodeSettingEntryActivity : UnchangableAccountAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_node_setting_entry)
        textViewtitle.setText(R.string.node_setting_entry_title)

        viteEntry.setOnClickListener {
            NodeSettingActivity.show(this, true)
        }

        ethEntry.setOnClickListener {
            NodeSettingActivity.show(this, false)
        }

        currentViteNodeIp.text = NetConfigHolder.netConfig.getViteNodeUrl()
        currentETHNodeIp.text = NetConfigHolder.netConfig.getEthNodeUrl()


    }

    override fun onStart() {
        super.onStart()
        currentViteNodeIp.text = NetConfigHolder.netConfig.getViteNodeUrl()
        currentETHNodeIp.text = NetConfigHolder.netConfig.getEthNodeUrl()
    }
}