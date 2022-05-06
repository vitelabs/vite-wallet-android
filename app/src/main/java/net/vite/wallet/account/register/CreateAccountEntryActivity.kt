package net.vite.wallet.account.register

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import kotlinx.android.synthetic.main.activity_entry_create_account.*
import net.vite.wallet.*
import net.vite.wallet.account.LoginPrepareBaseActivity


class CreateAccountEntryActivity : LoginPrepareBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_create_account)
        showAnimation()

        qrCodeScan.setOnClickListener {
            scanQrcode()
        }

        createAccBtn.setOnClickListener {
            startActivityForResult(
                Intent(
                    this@CreateAccountEntryActivity,
                    CreateAccountActivity::class.java
                ), 100
            )
        }
        recoverAccBtn.setOnClickListener {
            startActivityForResult(
                Intent(
                    this@CreateAccountEntryActivity,
                    RecoverMnemonicActivity::class.java
                ), 101
            )
        }
    }

    private fun showAnimation() {
        val animationSet = AnimationSet(true)

        val scaleAnimation = ScaleAnimation(0F, 1F, 0F, 1F)
        val alphaAnimation = AlphaAnimation(0F, 1F)

        animationSet.addAnimation(scaleAnimation)
        animationSet.duration = 1500
        animationSet.addAnimation(alphaAnimation)
        animationSet.interpolator = AccelerateDecelerateInterpolator()
        slogenImg.animation = animationSet
        animationSet.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 101 || requestCode == 100) {
            if (resultCode == Activity.RESULT_OK) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
