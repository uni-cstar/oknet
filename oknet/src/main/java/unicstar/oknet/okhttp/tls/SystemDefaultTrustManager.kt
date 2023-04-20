package unicstar.oknet.okhttp.tls

import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.*
import javax.net.ssl.*

object SystemDefaultTrustManager {

    @JvmStatic
    fun systemDefaultTrustManager(): X509TrustManager {
        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers

            if (trustManagers.size != 1 || (trustManagers[0] !is X509TrustManager)) {
                throw  IllegalStateException(
                    "Unexpected default trust managers:"
                            + Arrays.toString(trustManagers)
                )
            }
            return trustManagers[0] as X509TrustManager
        } catch (e: GeneralSecurityException) {
            throw AssertionError("No System TLS", e); // The system has no TLS. Just give up.
        }
    }

    @JvmStatic
    fun systemDefaultSslSocketFactory(trustManager: X509TrustManager): SSLSocketFactory {
        try {
            val sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
            return sslContext.socketFactory
        } catch (e: GeneralSecurityException) {
            throw AssertionError("No System TLS", e); // The system has no TLS. Just give up.
        }
    }
}