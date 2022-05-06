package net.vite.wallet

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.vite.wallet.network.http.configbucket.ConfigBucketApi
import net.vite.wallet.utils.isChinese


class Announcement(
    val title: String,
    val message: String,
    val isCancel: Boolean
)

class GetAnnouncementVM : ViewModel() {
    val announcementLd = MutableLiveData<Announcement>()
    @SuppressLint("CheckResult")
    fun getAnnouncement() {
        ConfigBucketApi.getIOSAnnouncement()
            .subscribe({
                var title = ""
                var message = ""
                try {
                    if (ViteConfig.get().context.isChinese()) {
                        title = it.getAsJsonObject("title")
                            .getAsJsonObject("localized")
                            .get("zh-Hans").asString
                        message = it.getAsJsonObject("message")
                            .getAsJsonObject("localized")
                            .get("zh-Hans").asString
                    } else {
                        title = it.getAsJsonObject("title")
                            .get("base").asString
                        message = it.getAsJsonObject("message")
                            .get("base").asString
                    }
                } catch (e: Exception) {
                    announcementLd.postValue(Announcement(title, message, true))
                }
                if (message.isNotEmpty() && title.isNotEmpty()) {
                    announcementLd.postValue(Announcement(title, message, false))
                } else {
                    announcementLd.postValue(Announcement(title, message, true))
                }
            }, {
                it.printStackTrace()
                announcementLd.postValue(Announcement("", "", true))
            })
    }

}