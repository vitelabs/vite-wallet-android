package net.vite.wallet.exchange

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.network.http.operation.ExchangeBannerImageResp
import net.vite.wallet.network.http.operation.ExchangeBannerResp
import net.vite.wallet.utils.isChinese
import net.vite.wallet.vitebridge.H5WebActivity

class ExchangeBannerAdapter(val glide: RequestManager) : RecyclerView.Adapter<ExchangeVH>() {


    val list = if (ViteConfig.get().context.isChinese()) {
        mutableListOf(
            ExchangeBannerResp(
                link = "https://app.vite.net/webview/vitex_invite_inner/index.html",
                image = ExchangeBannerImageResp(url = "/uploads/3397c325ad5c4243ade8f82f4e061b40.jpg")
            ),
            ExchangeBannerResp(
                link = "https://forum.vite.net/topic/2655/vitex-%E4%BA%A4%E6%98%93%E6%89%80%E7%A7%BB%E5%8A%A8%E7%AB%AF%E6%93%8D%E4%BD%9C%E6%8C%87%E5%8D%97",
                image = ExchangeBannerImageResp(url = "/uploads/02258b6328ea40c394fd69de7c20cd7a.jpg")
            )
        )
    } else {
        mutableListOf(
            ExchangeBannerResp(
                link = "https://forum.vite.net/topic/2654/vitex-mobile-terminal-operation-guide",
                image = ExchangeBannerImageResp(url = "/uploads/479afd767ae54d3cb30c81666540aeb5.jpg")
            ),
            ExchangeBannerResp(
                link = "https://app.vite.net/webview/vitex_invite_inner/index.html",
                image = ExchangeBannerImageResp(url = "/uploads/c64ee76e76ed42a8a898f3a5982fcfc1.jpg")
            )
        )
    }


    fun setList(list: List<ExchangeBannerResp>) {
        this.list.clear()
        this.list.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeVH {
        return ExchangeVH.create(parent, glide)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ExchangeVH, position: Int) {
        holder.bind(list[position])
    }

}

class ExchangeVH(val rootView: View, val glide: RequestManager) :
    RecyclerView.ViewHolder(rootView) {
    val guideBgImg = rootView.findViewById<ImageView>(R.id.guideBgImg)

    companion object {
        fun create(parent: ViewGroup, glide: RequestManager): ExchangeVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exchange_banner, parent, false)
            return ExchangeVH(view, glide)
        }
    }

    fun bind(res: ExchangeBannerResp) {
        val guideImgUrl = NetConfigHolder.netConfig.exchangeBannerUrl + res.image.url
        glide.load(guideImgUrl).into(guideBgImg)
        rootView.setOnClickListener { H5WebActivity.show(rootView.context, res.link) }
    }
}