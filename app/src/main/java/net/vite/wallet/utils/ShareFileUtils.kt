package net.vite.wallet.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.text.TextUtils
import net.vite.wallet.R
import java.io.File


class ShareFileUtils {

    companion object {
        fun shareFile(context: Context, path: String) {
            if (TextUtils.isEmpty(path)) {
                return
            }

            checkFileUriExposure()

            val intent = Intent()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val uri = Uri.fromFile(File(path))
            intent.putExtra(Intent.EXTRA_STREAM, uri)  //传输图片或者文件 采用流的方式
            intent.type = "*/*"   //分享文件
            intent.action = Intent.ACTION_SEND
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getString(R.string.send_file)
                )
            )
        }

        private fun checkFileUriExposure() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val builder = StrictMode.VmPolicy.Builder()
                StrictMode.setVmPolicy(builder.build())
                builder.detectFileUriExposure()
            }
        }
    }


}