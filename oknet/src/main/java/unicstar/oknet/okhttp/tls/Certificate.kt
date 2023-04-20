package unicstar.oknet.okhttp.tls

import android.content.Context
import android.security.KeyChain
import okhttp3.OkHttpClient
import java.io.InputStream
import java.math.BigInteger
import java.security.KeyStore
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * 信任任何证书校验：即不校验任何证书
 * 不推荐使用该方式，这样子就失去https的意义了，上架谷歌市场也不允许这么处理
 */
fun trustAllCertificate(builder: OkHttpClient.Builder) {
    val trustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }
    }
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
    val sslSocketFactory = sslContext.socketFactory
    builder.sslSocketFactory(sslSocketFactory, trustManager)
}

/**
 * 指定本地使用的服务器证书来认证
 * @param assetsFileName 服务器证书在assets中的文件名  .cer 格式或者 .pem格式
 * @see okhttp3.internal.tls.OkHostnameVerifier 域名校验，不用设置，可以查看okhttp的实现方式作为学习.
 */
fun trustSpecificCertificate(
    context: Context,
    assetsFileName: String,
    builder: OkHttpClient.Builder
): OkHttpClient.Builder {
    val (factory, trustManager) = createSSLSocketFactoryAndX509TrustManager(
        context,
        assetsFileName
    )
    builder.sslSocketFactory(factory, trustManager)
    return builder
}

/**
 * 指定本地的服务器证书来认证 服务器的证书（服务器证书）
 *
 * @param assetsFileName 证书在assets中的文件名  .cer 格式或者 .pem格式
 */
fun createSSLSocketFactoryAndX509TrustManager(
    context: Context,
    assetsFileName: String
): Pair<SSLSocketFactory, X509TrustManager> {
    val trustManager = object : X509TrustManager {

        override fun checkClientTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?
        ) {
        }

        override fun checkServerTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?
        ) {
            if (chain.isNullOrEmpty()) {
                throw CertificateException("checkServerTrusted: X509Certificate array is null")
            }

            if (authType.isNullOrEmpty() || authType != "ECDHE_RSA") {
                throw CertificateException("checkServerTrusted: AuthType is not ECDHE_RSA")
            }

            //判断证书是否是本地信任列表里颁发的证书(系统默认的验证)
            try {
                val factory = TrustManagerFactory.getInstance("X509")
                factory.init(null as KeyStore?)
                //用系统的证书验证服务器证书,验证通过就不需要继续验证证书信息；也可以注释掉，继续走自己的服务器证书逻辑
                factory.trustManagers.forEach {
                    (it as X509TrustManager).checkServerTrusted(chain, authType)
                }
                return
            } catch (e: Throwable) {
                e.printStackTrace()
                //注意这个地方不能抛异常,用系统的证书验证服务器证书，没通过就用自己的验证规则
//                        throw new CertificateException(e);
            }

            //获取本地证书中的信息
            var clientEncoded: String//公钥
            var clientSubject: String//颁发给
            var clientIssUser: String//颁发机构

            context.assets.open(assetsFileName).use { inputStream ->
                val certificateFactory = CertificateFactory.getInstance("X.509")
                val clientCertificate =
                    certificateFactory.generateCertificate(inputStream) as X509Certificate
                clientEncoded =
                    BigInteger(1, clientCertificate.publicKey.encoded).toString(16)
                clientSubject = clientCertificate.subjectDN.name
                clientIssUser = clientCertificate.issuerDN.name
            }

            //获取网络中的证书信息
            val certificate: X509Certificate = chain[0]
            val publicKey: PublicKey = certificate.publicKey
            val serverEncoded: String = BigInteger(1, publicKey.encoded).toString(16)

            if (clientEncoded != serverEncoded) {
                throw  CertificateException("server's PublicKey is not equals to client's PublicKey");
            }
            val subject = certificate.subjectDN.name
            if (clientSubject != subject) {
                throw  CertificateException("server's SubjectDN is not equals to client's SubjectDN");
            }
            val issuser = certificate.issuerDN.name
            if (clientIssUser != issuser) {
                throw  CertificateException("server's IssuerDN is not equals to client's IssuerDN");
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }
    };
    val sslContext: SSLContext = SSLContext.getInstance("TLS");
    sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
    val sslSocketFactory = sslContext.socketFactory
    return Pair(sslSocketFactory, trustManager)
}

/**
 * 安装证书
 * todo 未测试过
 */
fun installSpecificCertificate(context: Context, assetsFileName: String) {
    val intent = KeyChain.createInstallIntent()
    context.assets.open(assetsFileName).use { inputStream: InputStream ->
        val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
        val clientCertificate =
            certificateFactory.generateCertificate(inputStream) as X509Certificate
        //将证书传给系统
        intent.putExtra(KeyChain.EXTRA_CERTIFICATE, clientCertificate.encoded)
        //此处为给证书设置默认别名，第二个参数可自定义，设置后无需用户输入
        intent.putExtra("name", "别名（可设置默认）");
        context.startActivity(intent);
    }
}