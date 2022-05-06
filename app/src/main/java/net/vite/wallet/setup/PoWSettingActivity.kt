package net.vite.wallet.setup

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import net.vite.wallet.utils.dp2px
import net.vite.wallet.utils.showToast
import net.vite.wallet.utils.toHex
import org.vitelabs.mobile.Mobile

class PoWSettingActivity : UnchangableAccountAwareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_node_setting)
        textViewtitle.text = getString(R.string.pow_setting_entry_title)

        settingList.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL)
        )
        settingList.adapter = MyAdapter()
        saveBtn.setOnClickListener {
            showDialog()
        }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.add_a_valid_pow_url_title))
        val dialogLayout =
            LayoutInflater.from(this).inflate(R.layout.dialog_add_node, null)
        val input = dialogLayout.findViewById<EditText>(R.id.customIpText)
        builder.setView(dialogLayout)
        builder.setPositiveButton(getString(R.string.confirm)) { _, _ ->
            checkPoW(input.editableText.toString())
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    @SuppressLint("CheckResult")
    fun checkPoW(url: String) {
        cycleProgressBar.visibility = View.VISIBLE
        getPowNonce(url).applyIoScheduler().subscribe({
            cycleProgressBar.visibility = View.GONE
            if (it) {
                NetConfigHolder.addCustomPowNode(url)
                NetConfigHolder.switchCustomPowNode(url)
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


    fun getPowNonce(url: String): Observable<Boolean> {
        return Observable.fromCallable {
            try {
                val client = Mobile.dial(url)
                client.getPowNonce(
                    "1",
                    Mobile.hash256("1".toByteArray()).toHex()
                ).isNotEmpty()
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
                NetConfigHolder.switchCustomPowNode(ip)
                settingList.adapter?.notifyDataSetChanged()
            }

            itemView.setOnLongClickListener {
                if (ip == NetConfigHolder.netConfig.customPowUrl) {
                    return@setOnLongClickListener true
                }

                PopupWindow(this@PoWSettingActivity).apply {
                    this.contentView = LayoutInflater.from(this@PoWSettingActivity)
                        .inflate(R.layout.delete_popup, null)
                    setBackgroundDrawable(null)
                    isOutsideTouchable = true
                    elevation = 8.0F
                    this.contentView.findViewById<View>(R.id.delete).setOnClickListener {
                        NetConfigHolder.deletePowUrl(ip)
                        settingList.adapter?.notifyDataSetChanged()
                        dismiss()
                    }
                    showAsDropDown(
                        itemView,
                        itemView.width - 24.0F.dp2px().toInt(),
                        (-itemView.height * 1.3).toInt()
                    )
                }
                true
            }
            if (ip == NetConfigHolder.netConfig.getPoWNodeUrl()) {
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
        val customNodeSetting = NetConfigHolder.readCustomPoWNodes()
        return customNodeSetting?.powNodes?.toMutableList()?.let {
            it.add(0, NetConfigHolder.netConfig.remoteViteUrl)
            it
        } ?: listOf(NetConfigHolder.netConfig.remoteViteUrl)
    }


}