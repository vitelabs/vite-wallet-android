package net.vite.wallet.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_token_info.view.*
import net.vite.wallet.R
import net.vite.wallet.network.http.vitex.Tradepair
import net.vite.wallet.utils.isChinese
import net.vite.wallet.vitebridge.H5WebActivity

private const val ARG_PARAM1 = "param1"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [TokenInfoFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [TokenInfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TokenInfoFragment : Fragment() {
    private var sellListener: View.OnClickListener? = null
    private var buyListener: View.OnClickListener? = null
    private var rootView: View? = null
    private var cachedTradepair: Tradepair? = null
    private var pairSymbol: String? = null
    private var hasCreatedView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pairSymbol = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        hasCreatedView = true
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_token_info, container, false)
//        rootView?.findViewById<View>(R.id.klineBuy)?.setOnClickListener(buyListener)
//        rootView?.findViewById<View>(R.id.klineSell)?.setOnClickListener(sellListener)
        return rootView
    }

    fun updatePairSymbol(pairSymbol: String) {
        this.pairSymbol = pairSymbol
    }

    fun updateUI() {
        if (rootView == null)
            return
        cachedTradepair?.tradeTokenDetail?.let {
            Glide.with(this).load(it.urlIcon).into(rootView!!.tokenIcoin)
            rootView!!.tokenName.text = it.originalSymbol ?: "--"
            rootView!!.tokenBio.text = it.symbol ?: "--"
            rootView!!.tokenIdValue.text = it.tokenId ?: "--"
            if (it.totalSupply == null)
                rootView!!.totalSupplyValue.text = "--"
            else {
                val decimal = it.tokenDecimals ?: 0
                rootView!!.totalSupplyValue.text =
                    it.totalSupply?.substring(0, it.totalSupply!!.length - decimal)
            }

            rootView!!.tokenTypeValue.text =
                if (it.gateway == null) rootView!!.tokenTypeValue.context.getString(R.string.raw_token) else rootView!!.tokenTypeValue.context.getString(
                    R.string.crosschain_token
                )
            rootView!!.crossChainGatewayValue.text = it.gateway?.name ?: "--"
            rootView!!.overviewValue.text = if (rootView!!.overviewValue.context.isChinese()) {
                it.overview?.zh
            } else {
                it.overview?.en
            }
        }

        cachedTradepair?.tradeTokenDetail?.let {
            rootView!!.officialSiteValue.text =
                it.links?.get(LINKS_CONSTANT.WEBSITE.key()).toString().replace("[", "")
                    .replace("]", "")
            rootView!!.officialSiteValue.text =
                if (rootView!!.officialSiteValue.text.toString() == "null") "--" else rootView!!.officialSiteValue.text.toString()
            rootView!!.officialSiteValue.setOnClickListener {
                H5WebActivity.show(
                    rootView!!.discordImg.context,
                    rootView!!.officialSiteValue.text.toString()
                )
            }

            rootView!!.whitePaperValue.text =
                it.links?.get(LINKS_CONSTANT.WHITEPAPER.key()).toString().replace("[", "")
                    .replace("]", "")
            rootView!!.whitePaperValue.text =
                if (rootView!!.whitePaperValue.text.toString() == "null") "--" else rootView!!.whitePaperValue.text.toString()
            rootView!!.whitePaperValue.setOnClickListener {
                H5WebActivity.show(
                    rootView!!.discordImg.context,
                    rootView!!.whitePaperValue.text.toString()
                )
            }

            rootView!!.browserValue.text =
                it.links?.get(LINKS_CONSTANT.EXPLORER.key()).toString().replace("[", "")
                    .replace("]", "")
            rootView!!.browserValue.text =
                if (rootView!!.browserValue.text.toString() == "null") "--" else rootView!!.browserValue.text.toString()
            rootView!!.browserValue.setOnClickListener {
                H5WebActivity.show(
                    rootView!!.discordImg.context,
                    rootView!!.browserValue.text.toString()
                )
            }

            rootView!!.githubValue.text =
                it.links?.get(LINKS_CONSTANT.GITHUB.key()).toString().replace("[", "")
                    .replace("]", "")
            rootView!!.githubValue.text =
                if (rootView!!.githubValue.text.toString() == "null") "--" else rootView!!.githubValue.text.toString()
            rootView!!.githubValue.setOnClickListener {
                H5WebActivity.show(
                    rootView!!.discordImg.context,
                    rootView!!.githubValue.text.toString()
                )
            }

            it.links?.get(LINKS_CONSTANT.FACEBOOK.key())?.get(0)?.let { url ->
                rootView!!.facebookImg.visibility = View.VISIBLE
                rootView!!.facebookImg.setOnClickListener {
                    H5WebActivity.show(rootView!!.facebookImg.context, url)
                }
            }

            it.links?.get(LINKS_CONSTANT.TWITTER.key())?.get(0)?.let { url ->
                rootView!!.twitterImg.visibility = View.VISIBLE
                rootView!!.twitterImg.setOnClickListener {
                    H5WebActivity.show(rootView!!.twitterImg.context, url)
                }
            }

            it.links?.get(LINKS_CONSTANT.DISCORD.key())?.get(0)?.let { url ->
                rootView!!.discordImg.visibility = View.VISIBLE
                rootView!!.discordImg.setOnClickListener {
                    H5WebActivity.show(rootView!!.discordImg.context, url)
                }
            }

            it.links?.get(LINKS_CONSTANT.TELEGRAM.key())?.get(0)?.let { url ->
                rootView!!.telegramImg.visibility = View.VISIBLE
                rootView!!.telegramImg.setOnClickListener {
                    H5WebActivity.show(rootView!!.telegramImg.context, url)
                }
            }

            it.links?.get(LINKS_CONSTANT.REDDIT.key())?.get(0)?.let { url ->
                rootView!!.redditImg.visibility = View.VISIBLE
                rootView!!.redditImg.setOnClickListener {
                    H5WebActivity.show(rootView!!.redditImg.context, url)
                }
            }
        }
    }

    fun updateData(tradePair: Tradepair) {
        cachedTradepair = tradePair
        updateUI()
    }

    fun setGlobalListeners(buyListener: View.OnClickListener, sellListener: View.OnClickListener) {
        this.buyListener = buyListener
        this.sellListener = sellListener
    }

    enum class LINKS_CONSTANT {

        WHITEPAPER, GITHUB, WEBSITE, EXPLORER, EMAIL, OVERVIEW, TWITTER, FACEBOOK, REDDIT, INSTAGRAM,
        MEDIUM, STEEMIT, YOUTUBE, TELEGRAM, LINKEDIN, DISCORD;

        fun key(): String {
            return this.name.toLowerCase()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TokenInfoFragment.
         */
        @JvmStatic
        fun newInstance(param1: String) =
            TokenInfoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}
