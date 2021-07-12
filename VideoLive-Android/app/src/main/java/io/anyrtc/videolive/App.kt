package io.anyrtc.videolive

import android.app.Application
import android.content.pm.ApplicationInfo
import com.hjq.permissions.XXPermissions
import com.kongzue.dialog.util.DialogSettings
import io.anyrtc.videolive.utils.Constans.HTTP_TOKEN
import io.anyrtc.videolive.utils.SpUtil
import okhttp3.OkHttpClient
import rxhttp.RxHttpPlugins
import rxhttp.wrapper.ssl.HttpsUtils
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import kotlin.properties.Delegates


class App : Application() {

    companion object{
        var app : App by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        SpUtil.init(this)
        XXPermissions.setScopedStorage(true)
        DialogSettings.style = DialogSettings.STYLE.STYLE_IOS
        RxHttpPlugins.init(getDefaultOkHttpClient()).setDebug(BuildConfig.DEBUG)
        .setOnParamAssembly {
            val token = SpUtil.get().getString(HTTP_TOKEN, "")
            if (!token.isNullOrEmpty()){
                it.addHeader("Authorization", "Bearer $token")
            }
            it
        }
    }

    private fun getDefaultOkHttpClient(): OkHttpClient? {
        val sslParams = HttpsUtils.getSslSocketFactory()
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager) //添加信任证书
            .hostnameVerifier(HostnameVerifier { hostname: String?, session: SSLSession? -> true }) //忽略host验证
            .build()
    }

}