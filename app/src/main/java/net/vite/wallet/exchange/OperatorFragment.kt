package net.vite.wallet.exchange

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_operator.*
import net.vite.wallet.R
import net.vite.wallet.ViteConfig
import net.vite.wallet.kline.market.activity.KLineActivity
import net.vite.wallet.network.http.vitex.Tradepair
import net.vite.wallet.utils.isChinese

private const val ARG_PARAM1 = "param1"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OperatorFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [OperatorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OperatorFragment : Fragment() {
    private var sellListener: View.OnClickListener? = null
    private var buyListener: View.OnClickListener? = null
    private var rootView: View? = null
    private var pairSymbol: String? = null
    private var cachedTradepair: Tradepair? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pairSymbol = it.getString(ARG_PARAM1)
        }
    }

    fun updatePairSymbol(pairSymbol: String) {
        this.pairSymbol = pairSymbol
    }

    fun updateUI() {
        if (cachedTradepair?.operatorInfo == null) {
            operatorName.text = getString(R.string.anonymous_operator)
            operatorAddressTxt.text = "--"
            operatorIntoTxt.text = "--"
            operatorIcon.setImageResource(R.mipmap.anonymous_operator)
            tradePair.text = "--"
        } else {
            cachedTradepair?.let {
                operatorName.text = cachedTradepair?.operatorInfo?.name
                operatorAddressTxt.text = cachedTradepair?.operatorInfo?.address
                operatorIntoTxt.text =
                    if (ViteConfig.get().context.isChinese()) cachedTradepair?.operatorInfo?.overview?.zh else cachedTradepair?.operatorInfo?.overview?.en
                Glide.with(this).load(cachedTradepair?.operatorInfo?.icon).into(operatorIcon)

                var sb = StringBuilder()
                val pairsList = mutableListOf<String>()
                cachedTradepair?.operatorInfo?.tradePairs?.let { map ->
                    map.forEach {
                        it.value.forEach {
                            sb.append(it).append("   ")
                            pairsList.add(it)
                        }
                    }
                }

                val spannableString = SpannableString(sb.toString())//遍历交易对，每个单独设置clickableSpan
                pairsList.forEach {
                    val clickableSpan = MyClickableSpan(activity!!, it)
                    val start = spannableString.indexOf(it)
                    spannableString.setSpan(
                        clickableSpan,
                        start,
                        start + it.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                tradePair.text = spannableString
                tradePair.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    fun updateData(tradePair: Tradepair) {
        cachedTradepair = tradePair
        rootView?.let {
            updateUI()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_operator, container, false)

//        rootView?.findViewById<View>(R.id.klineBuy)?.setOnClickListener(buyListener)
//        rootView?.findViewById<View>(R.id.klineSell)?.setOnClickListener(sellListener)
        return rootView
    }

    fun setGlobalListeners(buyListener: View.OnClickListener, sellListener: View.OnClickListener) {
        this.buyListener = buyListener
        this.sellListener = sellListener
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment OperatorFragment.
         */
        @JvmStatic
        fun newInstance(param1: String) =
            OperatorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }


    class MyClickableSpan(val context: FragmentActivity, val tag: String) : ClickableSpan() {

        override fun onClick(view: View) {
//            Toast.makeText(context, tag, Toast.LENGTH_SHORT).show()
            (context as KLineActivity).toTradePage(tag)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }

    }
}
