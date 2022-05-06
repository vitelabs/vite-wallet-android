package net.vite.wallet.nut.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

import kotlinx.android.synthetic.main.fragment_nut_news.*
import net.vite.wallet.R
import net.vite.wallet.network.http.nut.NutItem
import net.vite.wallet.nut.NutViewModel
import net.vite.wallet.vitebridge.H5WebActivity
import java.text.SimpleDateFormat
import java.util.*

class NewsFragment : Fragment() {
    private lateinit var nutVm: NutViewModel
    private val adapter = NewsAdapter()
    private val newsList = ArrayList<NutItem>()
    private val dataFormat = SimpleDateFormat("MM.dd HH:mm")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nut_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nutVm = activity?.run {
            ViewModelProviders.of(this).get(NutViewModel::class.java)
        } ?: throw  Exception("invalid activity")

        nutVm.nutConfigLd.observe(this, Observer {
            it.tags?.find { it.tag == "news" }?.list?.run {
                val list = this.filter { it.isExpire == 0 }
                if (list.isNotEmpty()) {
                    newsList.clear()
                    newsList.addAll(list)
                    adapter.notifyDataSetChanged()
                }
            }
        })

        recyclerPages.layoutManager = LinearLayoutManager(activity!!)
        recyclerPages.adapter = NewsAdapter()
    }

    private inner class NewsVH(val view: View) : RecyclerView.ViewHolder(view) {

        val newsIcon = view.findViewById<ImageView>(R.id.newsIcon)
        val newTitle = view.findViewById<TextView>(R.id.newTitle)
        val newsTimeStamp = view.findViewById<TextView>(R.id.newsTimeStamp)
        val isOfficialTitle = view.findViewById<TextView>(R.id.isOfficialTitle)
        fun bind(position: Int) {
            newsList[position].let { nutItem ->
                view.setOnClickListener {
                    nutItem.skipUrl?.let { skipUrl ->

                        H5WebActivity.show(activity!!, skipUrl)
                    }
                }

                isOfficialTitle.visibility = if (1 == nutItem.source) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                nutItem.imgUrl?.let {
                    Glide.with(activity!!).load(
                        it
                    ).into(newsIcon)
                }

                newTitle.text = nutItem.title ?: ""
                nutItem.createTime?.let {
                    newsTimeStamp.text = dataFormat.format(it * 1000)
                }
            }
        }
    }

    private inner class NewsAdapter : RecyclerView.Adapter<NewsVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsVH {
            return NewsVH(LayoutInflater.from(parent.context).inflate(R.layout.item_nut_news, parent, false))
        }

        override fun getItemCount(): Int {
            return newsList.size
        }

        override fun onBindViewHolder(holder: NewsVH, position: Int) {
            holder.bind(position)
        }

    }
}