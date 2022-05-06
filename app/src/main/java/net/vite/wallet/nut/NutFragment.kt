package net.vite.wallet.nut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_nut.*
import net.vite.wallet.R
import net.vite.wallet.network.http.nut.NutConfig
import net.vite.wallet.utils.isChinese

class NutFragment : Fragment() {

    private var lastConfig: NutConfig? = null
    private val fragmentList = arrayListOf(R.layout.item_nut_title)
    private val nutAdapter = adapter()
    private val nutVm by activityViewModels<NutViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nut, container, false)
    }


    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            nutVm.pullConfig(requireActivity().isChinese())
        }
    }

    override fun onStart() {
        super.onStart()
        nutVm.pullConfig(requireActivity().isChinese())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nutVm.networkState.observe(viewLifecycleOwner, {})

        nutVm.nutConfigLd.observe(viewLifecycleOwner, Observer {
            if (it == lastConfig) {
                return@Observer
            }

            it.tags?.forEach { tag ->
                when (tag.tag) {
                    "game" -> fragmentList.add(R.layout.item_fragment_nut_game)
                    "activity" -> fragmentList.add(R.layout.item_fragment_nut_opreation)
                    "news" -> fragmentList.add(R.layout.item_fragment_nut_news)
                }
            }
            nutAdapter.notifyDataSetChanged()
        })

        nutMainList.adapter = nutAdapter
    }


    class vh(val view: View) : RecyclerView.ViewHolder(view)

    inner class adapter : RecyclerView.Adapter<vh>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): vh {
            return vh(
                when (viewType) {
                    R.layout.item_nut_title -> LayoutInflater.from(parent.context).inflate(
                        R.layout.item_nut_title,
                        parent,
                        false
                    )
                    R.layout.item_fragment_nut_opreation -> LayoutInflater.from(parent.context)
                        .inflate(
                            R.layout.item_fragment_nut_opreation,
                            parent,
                            false
                        )
                    R.layout.item_fragment_nut_game -> LayoutInflater.from(parent.context).inflate(
                        R.layout.item_fragment_nut_game,
                        parent,
                        false
                    )
                    R.layout.item_fragment_nut_news -> LayoutInflater.from(parent.context).inflate(
                        R.layout.item_fragment_nut_news,
                        parent,
                        false
                    )
                    else -> View(parent.context)
                }
            )
        }

        override fun getItemCount(): Int {
            return fragmentList.size
        }

        override fun getItemViewType(position: Int): Int {
            return fragmentList[position]
        }

        override fun onBindViewHolder(holder: vh, position: Int) {

        }

    }

}