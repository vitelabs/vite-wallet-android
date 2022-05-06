package net.vite.wallet.nut.game

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

import kotlinx.android.synthetic.main.fragment_nut_game.*
import net.vite.wallet.R
import net.vite.wallet.network.http.nut.NutItem
import net.vite.wallet.nut.HorizontalDivider
import net.vite.wallet.nut.NutViewModel
import net.vite.wallet.utils.WhiteHostManager
import net.vite.wallet.utils.dp2px
import net.vite.wallet.vitebridge.H5WebActivity
import java.util.*

class GameFragment : Fragment() {
    private lateinit var nutVm: NutViewModel
    private val adapter = gameAdapter()
    private val gameList = ArrayList<NutItem>()
    private val timer = Timer()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nut_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        nutVm = activity?.run {
            ViewModelProviders.of(this).get(NutViewModel::class.java)
        } ?: throw  Exception("invalid activity")

        nutVm.nutConfigLd.observe(this, Observer {
            it.tags?.find { it.tag == "game" }?.list?.run {
                val list = this.filter { it.isExpire == 0 }
                if (list.isNotEmpty()) {
                    gameList.clear()
                    gameList.addAll(list)
                    adapter.notifyDataSetChanged()
                }
            }
        })

        val manager = LinearLayoutManager(activity!!, LinearLayoutManager.HORIZONTAL, false)
        recyclerPages.layoutManager = manager
        recyclerPages.adapter = adapter
        recyclerPages.addItemDecoration(HorizontalDivider())
        Handler().post {
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(recyclerPages)
        }
    }

    private inner class vh(val view: View) : RecyclerView.ViewHolder(view) {
        val iconImage = view.findViewById<ImageView>(R.id.gameIcon)
        val gameTitle = view.findViewById<TextView>(R.id.gameTitle)
        val gameSynopsis = view.findViewById<TextView>(R.id.gameSynopsis)

        fun bind(position: Int) {

            if (position == 0) {
                (view.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                    24.0f.dp2px().toInt(), 0, 0, 0
                )
            } else if (position == gameList.size - 1) {
                (view.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 0, 24.0f.dp2px().toInt(), 0)
            }

            gameList[position].let { nutItem ->
                view.setOnClickListener {
                    nutItem.skipUrl?.let { skipUrl ->

                        if (WhiteHostManager.isWhiteHost(skipUrl)) {
                            H5WebActivity.show(activity!!, skipUrl)
                        } else {
                            AlertDialog.Builder(activity!!)
                                .setMessage(R.string.warning_of_jump_to_third)
                                .setPositiveButton(R.string.confirm) { _, _ ->
                                    H5WebActivity.show(activity!!, skipUrl)
                                }
                                .create().show()
                        }
                    }
                }
                Glide.with(activity!!).load(
                    nutItem.imgUrl
                ).into(iconImage)

                gameTitle.text = nutItem.title
                gameSynopsis.text = nutItem.desc

            }

        }

    }

    private inner class gameAdapter : RecyclerView.Adapter<vh>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): vh {
            return vh(LayoutInflater.from(parent.context).inflate(R.layout.item_nut_game, parent, false))
        }

        override fun getItemCount(): Int {
            return gameList.size
        }

        override fun onBindViewHolder(holder: vh, position: Int) {
            holder.bind(position)
        }

    }

}