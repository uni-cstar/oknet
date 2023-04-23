package unicstar.oknet

import android.os.Build
import android.util.Log
import okhttp3.OkHttpClient
import unicstar.oknet.okhttp.OkDomain

fun OkHttpClient.Builder.addOkDomain(baseUrl: String): OkHttpClient.Builder {
    OkDomain.useOkDomain(this, baseUrl)
    return this
}

var isAndroidPlatform: Boolean = run {
    try {
        Class.forName("android.os.Build")
        Build.VERSION.SDK_INT != 0
    } catch (ignored: ClassNotFoundException) {
        false
    }
}

internal fun isDependOn(className: String): Boolean {
    return try {
        Class.forName(className)
        true
    } catch (e: Throwable) {
        e.printStackTrace()
        false
    }
}

/**
 * 采用这种方法（内联），在使用的时候，可以避免字符串的创建
 */
internal inline fun logD(tag: String, msgFactory: () -> String) {
    if (!OkNetClient.debuggable)
        return
    if (isAndroidPlatform) {
        Log.d(tag, msgFactory.invoke())
    } else {
        println("${tag}:${msgFactory.invoke()}")
    }
}
