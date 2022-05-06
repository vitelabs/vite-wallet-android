package net.vite.wallet

import android.util.Log
import org.apache.log4j.Logger
import java.io.ByteArrayOutputStream
import java.io.PrintStream

fun logt(msg: String) {
    runCatching {
        if (BuildConfig.DEBUG) {
            Log.d("test", msg)
        }
//        Logger.getRootLogger().debug(msg) // Test log don't write to files
    }
}

fun logi(msg: String, tag: String = "") {
    runCatching {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg)
        }
        Logger.getRootLogger().info("$tag $msg")
    }
}

fun logw(msg: String, tag: String = "", throwable: Throwable? = null) {
    runCatching {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg, throwable)
        }
        Logger.getRootLogger().warn("$tag $msg", throwable)
    }
}

fun loge(throwable: Throwable, tag: String = "") {
    runCatching {
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace()
        }
        Logger.getRootLogger().error(tag, throwable)
    }
}

fun Throwable.toStackTraceString(): String {
    val bo = ByteArrayOutputStream()
    printStackTrace(PrintStream(bo))
    val stackString = bo.toString()
    bo.close()
    return stackString
}