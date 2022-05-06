package net.vite.wallet.nut.operation

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

import kotlinx.android.synthetic.main.fragment_nut_opreation.*
import net.vite.wallet.R
import net.vite.wallet.network.http.nut.NutItem
import net.vite.wallet.nut.HorizontalDivider
import net.vite.wallet.nut.NutViewModel
import net.vite.wallet.utils.dp2px
import net.vite.wallet.vitebridge.H5WebActivity
import java.util.*


class OperationFragment : Fragment() {
    private lateinit var nutVm: NutViewModel
    private val operationList = ArrayList<NutItem>()
    private val adapter = OperationRecyclerAdapter()
    private val timer = Timer()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nut_opreation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nutVm = activity?.run {
            ViewModelProviders.of(this).get(NutViewModel::class.java)
        } ?: throw  Exception("invalid activity")

        nutVm.nutConfigLd.observe(this, Observer {
            it.tags?.find { it.tag == "activity" }?.list?.run {
                val validList = this.filter { nutItem ->
                    nutItem.isExpire == 0
                }
                if (validList.isNotEmpty()) {
                    operationList.clear()
                    operationList.addAll(validList)
                    adapter.notifyDataSetChanged()
                }
            }
        })

        val manager = CenterZoomLayoutManager(context!!)
        recyclerPages.layoutManager = manager
        recyclerPages.adapter = adapter
        recyclerPages.addItemDecoration(HorizontalDivider())
        Handler().postDelayed({
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(recyclerPages)
        }, 100)

        timer.schedule(object : TimerTask() {
            override fun run() {
                try {
                    if (operationList.size > 1) {
                        recyclerPages.smoothScrollToPosition(
                            (manager.findFirstCompletelyVisibleItemPosition() + 1).rem(
                                operationList.size
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 2 * 1000, 5 * 1000)
    }

    override fun onDestroyView() {
        timer.cancel()
        super.onDestroyView()
    }

    private inner class OperationRecyclerAdapter : RecyclerView.Adapter<OperationVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperationVH {
            return OperationVH(LayoutInflater.from(parent.context).inflate(R.layout.item_nut_opreation, parent, false))
        }

        override fun getItemCount(): Int {
            return operationList.size
        }

        override fun onBindViewHolder(holder: OperationVH, position: Int) {
            if (position == 0) {
                (holder.view.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                    24.0f.dp2px().toInt(), 0, 0, 0
                )
            } else if (position == itemCount - 1) {
                (holder.view.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 0, 24.0f.dp2px().toInt(), 0)
            } else {
                (holder.view.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 0, 0, 0)
            }

            holder.bind(position)
        }

    }

    private inner class OperationVH(val view: View) : RecyclerView.ViewHolder(view) {

        private val itemTitle = view.findViewById<TextView>(R.id.itemTitle)
        private val itemBg = view.findViewById<ImageView>(R.id.itemBg)

        fun bind(position: Int) {
            val nutItem = operationList[position]
            Glide.with(activity!!).load(
                nutItem.imgUrl
            ).into(itemBg)
            itemTitle.text = nutItem.title
            view.setOnClickListener {
                nutItem.skipUrl?.let { skipUrl ->

                    H5WebActivity.show(activity!!, skipUrl)
                }
            }

        }
    }

}


class CenterZoomLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {

    private val mShrinkAmount = 0.15f
    private val mShrinkDistance = 0.75f

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val orientation = orientation
        if (orientation == HORIZONTAL) {
            val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
            val midpoint = width / 2f
            val d0 = 0f
            val d1 = mShrinkDistance * midpoint
            val s0 = 1f
            val s1 = 1f - mShrinkAmount
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                val childMidpoint = (getDecoratedRight(child!!) + getDecoratedLeft(child)) / 2f
                val d = Math.min(d1, Math.abs(midpoint - childMidpoint))
                val scale = s0 + (s1 - s0) * (d - d0) / (d1 - d0)
                val needHeightDec = child.height * (1.0F - scale)
                val topAdd = needHeightDec / 2
                val bottomDec = needHeightDec / 2
                child.top = topAdd.toInt()
                child.bottom = (440 - bottomDec).toInt()
            }
            return scrolled
        } else {
            return 0
        }
    }
}