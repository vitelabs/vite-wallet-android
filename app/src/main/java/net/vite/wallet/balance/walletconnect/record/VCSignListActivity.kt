package net.vite.wallet.balance.walletconnect.record

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_vcsign_list.*
import net.vite.wallet.R
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.balance.walletconnect.session.VCFsmHolder
import net.vite.wallet.balance.walletconnect.session.VcRequestTask
import net.vite.wallet.dialog.TextTitleNotifyDialog
import java.util.concurrent.TimeUnit


class VCSignListActivity : BaseTxSendActivity() {

    companion object {
        fun show(context: Context) {
            context.startActivity(
                Intent(
                    context,
                    VCSignListActivity::class.java
                )
            )
        }
    }

    var disposable: Disposable? = null

    private val vcSignListAdapter = VCSignListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vcsign_list)
        signList.adapter = vcSignListAdapter
        wcKillSession.setOnClickListener {

            with(TextTitleNotifyDialog(this)) {
                setTitle(R.string.warm_notice)
                setMessage(R.string.wc_kill_confirm_msg)
                setBottom(R.string.confirm) { dialog ->

                    VCFsmHolder.close()
                    dialog.dismiss()
                    finish()
                }
                show()
            }
        }
    }


    override fun onStart() {
        super.onStart()
        disposable = Observable.fromCallable {
            val list = VCFsmHolder.signManager?.requestTaskArray ?: ArrayList()

            val invertOrderList =
                list.foldRight(ArrayList<VcRequestTask>()) { item: VcRequestTask, acc: ArrayList<VcRequestTask> ->
                    acc.add(item)
                    acc
                }
            invertOrderList
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .repeatWhen { it.delay(2, TimeUnit.SECONDS) }
            .subscribe {
                vcSignListAdapter.data = it
                vcSignListAdapter.notifyDataSetChanged()
            }
    }

    override fun onStop() {
        super.onStop()
        disposable?.dispose()
        disposable = null
    }


}
