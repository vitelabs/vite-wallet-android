package net.vite.wallet.balance.uritx

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.uri_tx_layout.*
import net.vite.wallet.R
import net.vite.wallet.TxEndStatus
import net.vite.wallet.TxSendFailed
import net.vite.wallet.TxSendSuccess
import net.vite.wallet.activities.BaseTxSendActivity
import net.vite.wallet.balance.tokenselect.TokenSearchVM
import net.vite.wallet.balance.walletconnect.taskdetail.*
import net.vite.wallet.balance.walletconnect.taskdetail.buildin.Vote
import net.vite.wallet.constants.BlockDetailTypeVote
import net.vite.wallet.constants.BlockTypeToContactAddress
import net.vite.wallet.dialog.TextTitleNotifyDialog
import net.vite.wallet.network.rpc.BuildInContractEncoder
import net.vite.wallet.network.rpc.DefaultGid
import net.vite.wallet.network.rpc.NormalTxParams
import net.vite.wallet.network.rpc.ViteTokenInfo
import net.vite.wallet.utils.showToast
import net.vite.wallet.vep.decodeViteTransferUrl
import net.vite.wallet.viteappuri.ViteAppUri
import org.walletconnect.Session
import java.math.BigDecimal
import java.math.BigInteger

class UriTxActivity : BaseTxSendActivity() {
    companion object {
        fun show(context: Context, uri: Uri) {
            context.startActivity(Intent(context, UriTxActivity::class.java).apply {
                data = uri
            })
        }
    }

    val tokenSearchVM by lazy {
        ViewModelProviders.of(this)[TokenSearchVM::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uri_tx_layout)
        val uri = intent.data ?: kotlin.run {
            finish()
            return
        }
        cancelButton.setOnClickListener {
            finish()
        }

        if (!ViteAppUri.isSupportUri(uri)) {
            finish()
            return
        }

        if (uri.host == "vote") {
            vote(uri)
            return
        }


        val vepUriTransParam = uri.getQueryParameter("uri")?.let {
            kotlin.runCatching { decodeViteTransferUrl(it) }.getOrNull()
        } ?: kotlin.run {
            finish()
            return
        }
        tokenSearchVM.queryByTokenId(vepUriTransParam.tti)

        tokenSearchVM.tokenIdQueryLd.observe(this, Observer {
            if (it.isLoading()) {
                contentLoadingProgressBar.show()
                return@Observer
            }
            contentLoadingProgressBar.hide()

            val tokenInfo = it.resp ?: kotlin.run {
                with(TextTitleNotifyDialog(this@UriTxActivity)) {
                    setMessage(R.string.cannot_find_token)
                    setCancelable(false)
                    setBottom(R.string.confirm) { dialog ->
                        dialog.dismiss()
                        finish()
                    }
                    show()
                }
                return@Observer
            }

            val block = Session.MethodCall.ViteBlock(
                toAddress = vepUriTransParam.toAddr,
                tokenId = vepUriTransParam.tti,
                amount = tokenInfo.baseToSmallestUnit(
                    vepUriTransParam.amount?.toBigDecimal() ?: BigDecimal.ZERO
                ).toBigInteger().toString(),
                data = vepUriTransParam.data
            )

            val confirmInfo = if (vepUriTransParam.isFunctionCall()) {
                parseOtherContractToConfirmInfoOnlyUserViteBlock(tokenInfo, block)
            } else {
                normalTransferWcConfirmInfo(tokenInfo, block)
            }

            val title = getString(
                if (vepUriTransParam.isFunctionCall()) {
                    R.string.contract_address
                } else {
                    R.string.receive_address
                }
            )
            recyclerView.adapter = ConfirmInfoAdapter(confirmInfo)

            lastSendParams = block.toNormalTxParams(currentAccount.nowViteAddress()!!)

            confirmBtn.setOnClickListener {
                verifyIdentityDirectlyFinger(title,
                    { sendTx(lastSendParams!!) },
                    {})
            }
        })


    }

    override fun onTxEnd(status: TxEndStatus) {
        when (status) {
            is TxSendSuccess -> {
                status.powProfile?.let {
                    showPowProfileDialog(it) {
                        finish()
                    }
                } ?: kotlin.run {
                    baseTxSendFlow.showBaseTxSendSuccessDialog {
                        finish()
                    }
                }
            }
            is TxSendFailed -> status.throwable.showToast(this)
        }
    }


    private fun vote(uri: Uri) {
        val voteName = uri.getQueryParameter("name") ?: kotlin.run {
            finish()
            return
        }

        val gid = uri.getQueryParameter("gid") ?: kotlin.run {
            DefaultGid
        }

        val confirmInfo = WCConfirmInfo(
            Vote.desc.title(), listOf(
                WCConfirmItemInfo.create(
                    Vote.desc.inputs[0], voteName
                )
            )
        )
        recyclerView.adapter = ConfirmInfoAdapter(confirmInfo)
        lastSendParams = NormalTxParams(
            accountAddr = currentAccount.nowViteAddress()!!,
            toAddr = BlockTypeToContactAddress[BlockDetailTypeVote]!!,
            tokenId = ViteTokenInfo.tokenId!!,
            amountInSu = BigInteger.ZERO,
            data = BuildInContractEncoder.encodeVote(voteName, gid)
        )

        confirmBtn.setOnClickListener {
            verifyIdentityDirectlyFinger(getString(R.string.vote), {
                sendTx(lastSendParams!!)
            }, {})
        }


    }


}