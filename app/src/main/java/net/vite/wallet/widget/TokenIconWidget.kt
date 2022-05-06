package net.vite.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.widget_token_icon.view.*
import net.vite.wallet.R
import net.vite.wallet.utils.dp2px

class TokenIconWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val view = LayoutInflater.from(context).inflate(R.layout.widget_token_icon, this)


    fun setup(iconUrl: String?, showGatewayFooter: Boolean = false) {
        iconUrl?.let {
            tokenIcon.visibility = View.VISIBLE
            val options = RequestOptions.circleCropTransform()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            Glide.with(this).load(it).apply(options).into(tokenIcon)
        } ?: kotlin.run {
            tokenIcon.visibility = View.GONE
        }
        if (showGatewayFooter) {
            setTokenFooter(R.mipmap.gateway_icon)
        } else {
            setTokenFooter(null)
        }
    }

//    fun setup(family: Int, iconUrl: String?, tokenCode: String?) {
//        if (family == TokenFamily.ETH) {
//            if (tokenCode != TokenInfoCenter.EthEthTokenCodes.tokenCode) {
//                setTokenFooter(R.mipmap.eth_token_footer)
//            } else {
//                tokenFooter.visibility = View.GONE
//            }
//            setOutlineImageResource(R.mipmap.circle_outline_eth)
//        } else if (family == TokenFamily.VITE) {
//            setOutlineImageResource(R.mipmap.circle_outline_vite)
//            if (tokenCode != TokenInfoCenter.ViteViteTokenCodes.tokenCode) {
//                setTokenFooter(R.mipmap.vite_token_footer)
//            } else {
//                tokenFooter.visibility = View.GONE
//            }
//        }
//        iconUrl?.let {
//            tokenIcon.visibility = View.VISIBLE
//            val options = RequestOptions.circleCropTransform()
//                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
//            Glide.with(this).load(it).apply(options).into(tokenIcon)
//        } ?: kotlin.run {
//            tokenIcon.visibility = View.GONE
//        }
//    }

    fun setOutlineImageResource(@DrawableRes resId: Int) {
        tokenOutline.setImageResource(resId)
    }

    fun setTokenFooterSize(diameterInDp: Float) {
        val l = tokenFooter.layoutParams
        l.height = diameterInDp.dp2px().toInt()
        l.width = diameterInDp.dp2px().toInt()
    }

    fun setTokenFooter(@DrawableRes resId: Int?) {
        resId?.let {
            tokenFooter.visibility = View.VISIBLE
            tokenFooter.setImageResource(it)
        } ?: kotlin.run {
            tokenFooter.visibility = View.GONE
        }
    }


}