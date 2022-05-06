package net.vite.wallet.setup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_language_list.*
import kotlinx.android.synthetic.main.activity_language_list.textViewtitle
import kotlinx.android.synthetic.main.activity_node_setting.*
import net.vite.wallet.R
import net.vite.wallet.account.UnchangableAccountAwareActivity
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.eth.EthOkHttpClient
import net.vite.wallet.utils.dp2px
import net.vite.wallet.utils.showToast
import org.vitelabs.mobile.Mobile
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class NodeSettingActivity : UnchangableAccountAwareActivity() {
    companion object {
        fun show(activity: Activity, isVite: Boolean) {
            activity.startActivity(Intent(activity, NodeSettingActivity::class.java).apply {
                putExtra("isVite", isVite)
            })
        }
    }

    private val isVite by lazy { intent.getBooleanExtra("isVite", false) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_node_setting)
        textViewtitle.text = getString(
            R.string.vite_node_setting, if (isVite) {
                "Vite"
            } else {
                "ETH"
            }
        )

        settingList.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL)
        )

        settingList.adapter = MyAdapter()

        saveBtn.setOnClickListener {
            showDialog()
        }
    }

    fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.add_node_title))
        val dialogLayout =
            LayoutInflater.from(this).inflate(R.layout.dialog_add_node, null)
        val input = dialogLayout.findViewById<EditText>(R.id.customIpText)
        builder.setView(dialogLayout)
        builder.setPositiveButton(getString(R.string.confirm)) { dialog, which ->
            if (isVite) {
                checkVite(input.editableText.toString())
            } else {
                checkEth(input.editableText.toString())
            }
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, which -> dialog.cancel() }
        builder.show()
    }

    @SuppressLint("CheckResult")
    fun checkVite(url: String) {
        cycleProgressBar.visibility = View.VISIBLE
        getSnapshotBlockHeight(url).applyIoScheduler().subscribe({
            cycleProgressBar.visibility = View.GONE
            if (it) {
                NetConfigHolder.addCustomViteNode(url)
                NetConfigHolder.switchViteNode(url)
                settingList.adapter?.notifyDataSetChanged()
                showToast(R.string.success)
            } else {
                showToast(R.string.invalid_node)
            }
        }, {
            showToast(R.string.invalid_node)
            cycleProgressBar.visibility = View.GONE
        })
    }

    @SuppressLint("CheckResult")
    fun checkEth(url: String) {
        cycleProgressBar.visibility = View.VISIBLE
        Observable.fromCallable {
            val number = kotlin.runCatching {
                Web3j.build(HttpService(url, EthOkHttpClient.create())).ethBlockNumber().send()
            }.getOrNull()
            number?.hasError() == false
        }.applyIoScheduler().subscribe({
            cycleProgressBar.visibility = View.GONE
            if (it) {
                NetConfigHolder.addCustomEthNode(url)
                NetConfigHolder.switchCustomEthNode(url)
                settingList.adapter?.notifyDataSetChanged()
                showToast(R.string.success)
            } else {
                showToast(R.string.invalid_node)
            }
        }, {
            showToast(R.string.invalid_node)
            cycleProgressBar.visibility = View.GONE
        })

    }


    fun getSnapshotBlockHeight(url: String): Observable<Boolean> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(url)
                val height = client.snapshotChainHeight
                print(height)
                true
            } catch (e: Exception) {
                false
            }
        }
    }


    private inner class MyVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.text)
        val checked = itemView.findViewById<View>(R.id.checked)

        fun bind(ip: String) {
            textView.text = ip
            itemView.setOnClickListener {
                if (isVite) {
                    NetConfigHolder.switchViteNode(ip)
                } else {
                    NetConfigHolder.switchCustomEthNode(ip)
                }
                settingList.adapter?.notifyDataSetChanged()
            }

            itemView.setOnLongClickListener {
                if (isVite) {
                    if (ip == NetConfigHolder.netConfig.remoteViteUrl) {
                        return@setOnLongClickListener true
                    }
                } else {
                    if (ip == NetConfigHolder.netConfig.remoteEthUrl) {
                        return@setOnLongClickListener true
                    }
                }

                PopupWindow(this@NodeSettingActivity).apply {
                    this.contentView = LayoutInflater.from(this@NodeSettingActivity)
                        .inflate(R.layout.delete_popup, null)
                    setBackgroundDrawable(null)
                    isOutsideTouchable = true
                    elevation = 8.0F
                    this.contentView.findViewById<View>(R.id.delete).setOnClickListener {
                        if (isVite) {
                            NetConfigHolder.deleteViteUrl(ip)
                        } else {
                            NetConfigHolder.deleteEthUrl(ip)
                        }
                        settingList.adapter?.notifyDataSetChanged()
                        dismiss()
                    }
                    showAsDropDown(itemView, itemView.width - 24.0F.dp2px().toInt(), (-itemView.height * 1.3).toInt())
                }


                true
            }
            if (ip == NetConfigHolder.netConfig.getEthNodeUrl() || ip == NetConfigHolder.netConfig.getViteNodeUrl()) {
                checked.visibility = View.VISIBLE
            } else {
                checked.visibility = View.GONE
            }
        }
    }


    private inner class MyAdapter : RecyclerView.Adapter<MyVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_node, parent, false)
            return MyVH(view)
        }

        override fun onBindViewHolder(holder: MyVH, position: Int) {
            holder.bind(getNodes()[position])
        }

        override fun getItemCount() = getNodes().size
    }

    fun getNodes(): List<String> {
        val customNodeSetting = NetConfigHolder.readCustomNodes()
        return if (isVite) {
            customNodeSetting?.viteNodes?.toMutableList()?.let {
                it.add(0, NetConfigHolder.netConfig.remoteViteUrl)
                it
            } ?: listOf(NetConfigHolder.netConfig.remoteViteUrl)
        } else {
            customNodeSetting?.ethNodes?.toMutableList()?.let {
                it.add(0, NetConfigHolder.netConfig.remoteEthUrl)
                it
            } ?: listOf(NetConfigHolder.netConfig.remoteEthUrl)
        }
    }


}