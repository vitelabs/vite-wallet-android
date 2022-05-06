package net.vite.wallet.balance.walletconnect.session.fsm

import android.os.CountDownTimer

class MyCountDownTimer(millis: Long, val finish: () -> Unit) : CountDownTimer(millis, millis) {

    override fun onFinish() {
        this.finish.invoke()
    }

    override fun onTick(millisUntilFinished: Long) {
    }
}