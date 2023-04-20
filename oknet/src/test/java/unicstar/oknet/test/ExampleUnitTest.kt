package unicstar.oknet.test

import org.junit.Test
import unicstar.oknet.PingNetworkUtils
import unicstar.oknet.isNetworkAvailable
import unicstar.oknet.pingByCmd
import unicstar.oknet.pingByInetAddress
import java.util.concurrent.CountDownLatch
import kotlin.system.measureTimeMillis

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        var isNetEnable2 = false
        val time2 = measureTimeMillis {
            isNetEnable2 = isNetworkAvailable()
        }
        println("isNetworkAvailable take ${time2}ms result=${isNetEnable2}")
    }

    /**
     * 该方法在单元测试中会超时，在真机中可用
     */
    @Test
    fun testPingByInetAddress(){
        var isNetEnable = false
        val time = measureTimeMillis {
            isNetEnable = pingByInetAddress()
        }
        println("pingByInetAddress take ${time}ms result=${isNetEnable}")
    }

    @Test
    fun testPingByCmd(){
        var isNetEnable = false
        val time = measureTimeMillis {
            isNetEnable = pingByCmd()
        }
        println("pingByCmd take ${time}ms result=${isNetEnable}")
    }

    @Test
    fun dd(){
        val time = System.currentTimeMillis()
        val lock = CountDownLatch(1)
        PingNetworkUtils.setPingListener {
            println("PingNetworkUtils take ${System.currentTimeMillis() - time}ms result=${it}")
            lock.countDown()
        }
        PingNetworkUtils.startPing()
        lock.await()
        println("")
    }
}