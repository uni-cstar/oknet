package unicstar.oknet

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.LoggingEventListener
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import unicstar.oknet.okhttp.OkDomain
import unicstar.oknet.okhttp.OnConflictStrategy
import java.util.concurrent.TimeUnit

/**
 * 简易的网络api请求客户端；也可以单独使用[unicstar.oknet.okhttp.OkDomain]的功能
 * */
object OkNetClient {

    /**
     * 延迟初始化
     */
    interface LazyInitializer {

        val baseUrl: String

        /**
         * 配置：用于配置用户的自定义配置
         * 注意：不用再调用[unicstar.oknet.okhttp.addOkDomain]放，内部已经调用了这个方法
         */
        fun onSetup(oBuilder: OkHttpClient.Builder, rBuilder: Retrofit.Builder)

        /**
         * 配置完成
         */
        fun onConfigured() {}

    }

    private lateinit var mOkhttpClient: OkHttpClient
    private lateinit var mRetrofit: Retrofit
    private val mApiServiceCaches = mutableMapOf<Class<*>, Any>()

    //是否已经初始化
    private var hasInit: Boolean = false
    private var mLazyInitializer: LazyInitializer? = null

    var debuggable: Boolean
        get() = OkDomain.debuggable
        set(value) {
            OkDomain.debuggable = value
        }

    @JvmStatic
    val okHttpClient: OkHttpClient
        get() {
            tryInit()
            return mOkhttpClient
        }

    @PublishedApi
    internal fun tryInit() {
        if (hasInit)
            return
        val lazyInitializer = mLazyInitializer ?: throw RuntimeException("请先调用setup方法进行初始化")
        setup(lazyInitializer.baseUrl, lazyInitializer::onSetup)
        lazyInitializer.onConfigured()
        mLazyInitializer = null
    }

    private fun quicklyPreferredConverterFactory(): Converter.Factory? {
        return if (isDependOn("retrofit2.converter.moshi.MoshiConverterFactory")) {
            MoshiConverterFactory.create()
        } else if (isDependOn("retrofit2.converter.gson.GsonConverterFactory")) {
            GsonConverterFactory.create()
        } else {
//            throw RuntimeException("can not found moshi and gson converter factory,please specified converter directly.")
            logD("OkNetClient") {
                "can not found moshi and gson converter factory,please specified converter directly."
            }
            null

        }
    }

    /**
     * @param connectTimeoutMsec 链接超时，默认30000
     * @param readTimeoutMsec 读取超时，默认30000
     * @param retryOnConnectionFailure  连接失败是否重连，默认true
     */
    private fun quicklyLazyInitializer(
        baseUrl: String,
        debug: Boolean = false,
        connectTimeoutMsec: Long = 30 * 1000,
        readTimeoutMsec: Long = 30 * 1000,
        retryOnConnectionFailure: Boolean = true,
        converterFactory: Converter.Factory? = quicklyPreferredConverterFactory(),
        vararg mainHeaders: Triple<String, String, OnConflictStrategy>
    ): LazyInitializer {

        return object : LazyInitializer {

            override val baseUrl: String = baseUrl

            override fun onSetup(oBuilder: OkHttpClient.Builder, rBuilder: Retrofit.Builder) {
                oBuilder.connectTimeout(connectTimeoutMsec, TimeUnit.MILLISECONDS)
                    .readTimeout(readTimeoutMsec, TimeUnit.MILLISECONDS)
                    .retryOnConnectionFailure(retryOnConnectionFailure) //
                ////指定TLS
                //trustSpecificCertificate(App.instance,"ucuxin7434801.pem",builder)
                if (debug) {
                    if (isDependOn("okhttp3.logging.HttpLoggingInterceptor")) {
                        //logging 拦截器，okhttp.logging提供，主要是用于输出网络请求和结果的Log
                        val httpLoggingInterceptor = HttpLoggingInterceptor()
                        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY//配置输出级别
                        oBuilder.addInterceptor(httpLoggingInterceptor)//配置日志拦截器
                        oBuilder.eventListenerFactory(LoggingEventListener.Factory())//配置完整logging事件
                    }
                }

                rBuilder.baseUrl(baseUrl)   //配置服务器路径
                if (converterFactory != null) //配置converter
                    rBuilder.addConverterFactory(converterFactory)
            }

            override fun onConfigured() {
                super.onConfigured()
                mainHeaders.forEach { addMainHeader(it.first, it.second, it.third) }
            }
        }
    }

    /**
     * 快速配置：用于通常没有自定义配置，开箱即用
     * @param baseUrl 基地址
     * @param debug 是否开启调试日志
     * @param connectTimeoutMsec 链接超时，默认30000
     * @param readTimeoutMsec 读取超时，默认30000
     * @param retryOnConnectionFailure  连接失败是否重连，默认true
     * @param converterFactory 用于retrofit配置的json工厂
     * @param mainHeaders 主域名配置的全局header，后续可以修改；[Triple.first]-对应header的key，[Triple.second]-对应header的value，[Triple.third]-对应header冲突处理策略
     */
    @JvmStatic
    fun setupQuickly(
        baseUrl: String,
        debug: Boolean = false,
        connectTimeoutMsec: Long = 30 * 1000,
        readTimeoutMsec: Long = 30 * 1000,
        retryOnConnectionFailure: Boolean = true,
        converterFactory: Converter.Factory? = quicklyPreferredConverterFactory(),
        vararg mainHeaders: Triple<String, String, OnConflictStrategy>
    ) {
        debuggable = debug
        setup(
            quicklyLazyInitializer(
                baseUrl,
                debug,
                connectTimeoutMsec,
                readTimeoutMsec,
                retryOnConnectionFailure,
                converterFactory,
                *mainHeaders
            )
        )
    }

    @JvmStatic
    fun setup(lazyInitializer: LazyInitializer) {
        mLazyInitializer = lazyInitializer
    }

    @JvmStatic
    fun setup(baseUrl: String, initializer: (OkHttpClient.Builder, Retrofit.Builder) -> Unit) {
        require(!hasInit) {
            "OkNetClient has already been configured and cannot be configured repeatedly"
        }
        hasInit = true
        val oBuilder = OkHttpClient.Builder()
            .addOkDomain(baseUrl)
        val rBuilder = Retrofit.Builder()
        initializer.invoke(oBuilder, rBuilder)
        mOkhttpClient = oBuilder.build()
        mRetrofit = rBuilder.client(mOkhttpClient).build()
    }

    /**
     * 修改主域名
     */
    @JvmStatic
    inline fun setMainDomain(url: String) {
        tryInit()
        OkDomain.setMainDomain(url)
    }

    /**
     * set the domain of the specified name.
     * 设置[name]表示的域名为[url],通常是配置其他域名
     * @param name 域名的key，标识符，比如使用腾讯的域名，那么自定义一个标识符区别该域名 ,比如使用tencent
     */
    @JvmStatic
    inline fun setDomain(name: String, url: String) {
        tryInit()
        OkDomain.setDomain(name, url)
    }

    /**
     * set the global headers of the main domain
     * @param key the key of main domain header.
     * @param value the value of main domain header
     */
    @JvmStatic
    @JvmOverloads
    inline fun addMainHeader(
        key: String,
        value: String,
        conflictStrategy: OnConflictStrategy = OnConflictStrategy.IGNORE
    ) {
        tryInit()
        OkDomain.addMainHeader(key, value, conflictStrategy)
    }

    @JvmStatic
    inline fun removeMainHeader(key: String): Pair<String, OnConflictStrategy>? {
        tryInit()
        return OkDomain.removeMainHeader(key)
    }

    @JvmStatic
    @JvmOverloads
    inline fun addHeader(
        domainName: String,
        key: String,
        value: String,
        conflictStrategy: OnConflictStrategy = OnConflictStrategy.IGNORE
    ) {
        tryInit()
        OkDomain.addHeader(domainName, key, value, conflictStrategy)
    }

    @JvmStatic
    inline fun removeHeader(domainName: String, key: String): Pair<String, OnConflictStrategy>? {
        tryInit()
        return OkDomain.removeHeader(domainName, key)
    }

    /**
     * 创建ApiService
     * @param cacheable 是否使用缓存：建议反复、长期使用的ApiService可以全局保存
     */
    fun <T : Any> createApiService(clz: Class<T>, cacheable: Boolean): T {
        tryInit()
        return if (cacheable) {
            mApiServiceCaches.getOrPut(clz) {
                mRetrofit.create(clz)
            } as T
        } else {
            mRetrofit.create(clz)
        }
    }

    /**
     * 移除指定的ApiService
     */
    fun removeApiService(clz: Class<*>): Any? {
        return mApiServiceCaches.remove(clz)
    }

    /**
     * 清空所有的ApiService缓存
     */
    fun clearApiService() {
        mApiServiceCaches.clear()
    }

}