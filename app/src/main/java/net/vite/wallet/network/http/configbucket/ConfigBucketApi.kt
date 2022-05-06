package net.vite.wallet.network.http.configbucket

import com.google.gson.JsonObject
import io.reactivex.Observable
import net.vite.wallet.network.applyIoScheduler
import net.vite.wallet.network.http.HttpClient
import retrofit2.http.GET

interface ConfigBucketApi {
    companion object {
        fun getApi() = HttpClient.bucketNetwork().create(ConfigBucketApi::class.java)

        fun checkVersion(): Observable<VersionUpdate> {
            return getApi().checkVersion().applyIoScheduler()
        }

        fun getIOSAnnouncement(): Observable<JsonObject> {
            return getApi().getIOSAnnouncement().applyIoScheduler()
        }

        fun getPow(): Observable<JsonObject> {
            return getApi().getPow()
        }
    }


    @GET("config/android/AndroidUpdate")
    fun checkVersion(): Observable<VersionUpdate>

    @GET("config/AppNotice.json")
    fun getIOSAnnouncement(): Observable<JsonObject>

    @GET("config/ConfigHash.json")
    fun getPow(): Observable<JsonObject>

}