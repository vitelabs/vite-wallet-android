package net.vite.wallet.network.http

import android.util.Log
import com.google.gson.Gson
import net.vite.wallet.BuildConfig
import net.vite.wallet.ViteConfig
import net.vite.wallet.network.NetConfigHolder
import net.vite.wallet.utils.isChinese
import okhttp3.*
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object HttpClient {
    private val gsonConverterFactory = GsonConverterFactory.create()
    private val rxJava2CallAdapter = RxJava2CallAdapterFactory.create()
    private val airdropReqStringResGsonConverter = AirdropReqStringResGsonConverter()

    fun jsonRpc2Network() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.getViteNodeUrl()))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun bucketNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.configBucketUrl))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun ethTransactionNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.remoteEthTransactionsUrl))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun discoverNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.discoverPageUrl))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun normalGrwothNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.growthUrlBase))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()


    fun normalPushUploadNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.pushUploadUrl))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun normalPushSubscribeNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.pushSubscribeUrl))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun normalGwNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.gwUrlBase))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun coinPurchaseNetWork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.coinPurchaseUrlBase))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun airdropNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.growthUrlBase))
        .client(normalOkClient)
        .addConverterFactory(airdropReqStringResGsonConverter)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun exhangeBannerNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.exchangeBannerUrl))
        .client(normalOkClient)
        .addConverterFactory(airdropReqStringResGsonConverter)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()

    fun vitexNetwork() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.vitexUrlBase))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .addCallAdapterFactory(rxJava2CallAdapter)
        .build()


    fun vitexNetworkSuspendable() = Retrofit.Builder()
        .baseUrl(makeBaseUrlLegal(NetConfigHolder.netConfig.vitexUrlBase))
        .client(normalOkClient)
        .addConverterFactory(gsonConverterFactory)
        .build()


    val crosschainRetrofit = HashMap<String, Retrofit>()
    fun normalGwOverchainNetwork(crossChainUrl: String): Retrofit {
        val curl = makeBaseUrlLegal(crossChainUrl)
        return crosschainRetrofit[curl] ?: kotlin.run {
            Retrofit.Builder()
                .baseUrl(curl)
                .client(gwOverchainClient)
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(rxJava2CallAdapter)
                .build()
        }.also {
            crosschainRetrofit[curl] = it
        }
    }

    private fun makeBaseUrlLegal(rawUrl: String): String {
        return if (rawUrl.endsWith("/")) {
            rawUrl
        } else {
            "$rawUrl/"
        }
    }

    class AirdropReqStringResGsonConverter : Converter.Factory() {
        val gson = GsonConverterFactory.create()
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
            return gson.responseBodyConverter(type, annotations, retrofit)
        }

        override fun requestBodyConverter(
            type: Type,
            parameterAnnotations: Array<Annotation>,
            methodAnnotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<*, RequestBody>? {
            return AirdropReqConvert<Type>()
        }
    }

    class AirdropReqConvert<T> : Converter<T, RequestBody> {
        override fun convert(value: T): RequestBody? {
            val a = Gson().toJson(value)
            return RequestBody.create(
                MediaType.parse("text/plain"),
                ctrEncrypt(a)
            )
        }
    }

    private val gwOverchainClient = OkHttpClient()
        .newBuilder()
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                addInterceptor(ThreadInterceptor())
            }
            addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("version", "v1.0")
                        .addHeader(
                            "lang", if (ViteConfig.get().context.isChinese()) {
                                "zh-cn"
                            } else {
                                "en"
                            }
                        )
                        .build()
                )
            }
        }.build()

    val normalOkClient = OkHttpClient()
        .newBuilder()
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                addInterceptor(ThreadInterceptor())
            }
            connectTimeout(10, TimeUnit.SECONDS)
            writeTimeout(10, TimeUnit.SECONDS)
            readTimeout(10, TimeUnit.SECONDS)
            addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("channel", "android")
                        .addHeader("platform", "android")
                        .addHeader("version", ViteConfig.get().versionName)
                        .addHeader("versionCode", ViteConfig.get().versionCode.toString())
                        .addHeader(
                            "language", if (ViteConfig.get().context.isChinese()) {
                                "zh-Hans"
                            } else {
                                "en"
                            }
                        )
                        .build()
                )
            }
        }.build()

    class ThreadInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            Log.d("OkHttp", "OkHttp: ${request.url()} ,thread:${Thread.currentThread().id}")
            return chain.proceed(request)
        }
    }
}
