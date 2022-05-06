package net.vite.wallet.account.register

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_check_mnemonic.*
import net.vite.wallet.R
import net.vite.wallet.activities.BaseActivity
import net.vite.wallet.utils.dp2px
import java.util.*
import kotlin.collections.ArrayList

class CheckMnemonicActivity : BaseActivity() {

    companion object {
        fun show(activity: Activity, mnemonicList: ArrayList<String>, requestCode: Int) {
            activity.startActivityForResult(Intent(activity, CheckMnemonicActivity::class.java).apply {
                putStringArrayListExtra("mnemonicList", mnemonicList)
                putExtra("requestCode", requestCode)
            }, requestCode)
        }
    }

    private lateinit var mnemonicStrList: ArrayList<String>
    private var requestCode: Int = 0
    private lateinit var shuffledStrList: ArrayList<String>
    private val userInputStrList: ArrayList<String> = ArrayList()
    private lateinit var userInputAdapter: MnemonicAdapter
    private lateinit var shuffledAdapter: MnemonicAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_check_mnemonic)
        requestCode = intent.getIntExtra("requestCode", 0)
        userInputList.layoutManager = GridLayoutManager(this, 4)
        mnemonicList.layoutManager = GridLayoutManager(this, 4)
        mnemonicStrList = intent.getStringArrayListExtra("mnemonicList") ?: ArrayList()
        if (mnemonicStrList.size == 12) {
            recyclerContainer.layoutParams.height = 232f.dp2px().toInt()
        }
        shuffledStrList = ArrayList(mnemonicStrList)
        shuffledStrList.shuffle()

        userInputAdapter = MnemonicAdapter(userInputStrList) { adapter, list, position ->
            val removedList = ArrayList(adapter.list.subList(position, list.size))
            adapter.list.removeAll(removedList)
            adapter.notifyDataSetChanged()
            shuffledAdapter.list.addAll(removedList)
            shuffledAdapter.notifyDataSetChanged()
        }
        userInputList.adapter = userInputAdapter

        shuffledAdapter = MnemonicAdapter(shuffledStrList) { adapter, list, position ->
            userInputAdapter.list.add(list[position])
            userInputAdapter.notifyDataSetChanged()
            adapter.list.removeAt(position)
            adapter.notifyDataSetChanged()
        }
        mnemonicList.adapter = shuffledAdapter

        completeBtn.setOnClickListener {
            if (!Arrays.equals(userInputStrList.toArray(), mnemonicStrList.toArray())) {
                Toast.makeText(this, R.string.check_mnemonic_failed, Toast.LENGTH_SHORT).show()
            } else {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        backIcon.setOnClickListener {
            onBackPressed()
        }

    }

    override fun onBackPressed() {
        if (requestCode == RememberMnemonicActivity.MODE_FROM_NEW) {
            AlertDialog.Builder(this)
                .setMessage(R.string.back_regenerate_mnemonic_alert)
                .setPositiveButton(R.string.yes) { _, _ ->
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }.setNegativeButton(R.string.no) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        } else {
            super.onBackPressed()
        }
    }


}
