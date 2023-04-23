@file:JvmName("NetworkKt")

package unicstar.oknet.utils

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * 关于网络是否连通，参考地址：https://blog.csdn.net/guorun18/article/details/62216278
 */


/**
 * 功能：检测当前URL是否可连接或是否有效
 * 该方法所需时间更长（通常需要两三百毫秒），但检测结果更可靠
 * 描述：最多连接网络[retryCount]次, 如果[retryCount]次都不成功，视为该地址不可用
 * @return true:[preferredUrl]可以访问，并且也说明网络是可用的，false则相反
 */
fun isNetworkAvailable(
    preferredUrl: String = "https://www.baidu.com/",
    retryCount: Int = 2
): Boolean {
    var counts = 0
    while (counts < retryCount) {
        try {
            val url = URL(preferredUrl)
            val con = url.openConnection() as HttpURLConnection
            if (con.responseCode == 200) {
                return true
            }
            break
        } catch (ex: Exception) {
            counts++
            continue
        }
    }
    return false
}

/**
 * 判断主机地址是否可以访问
 * Jdk1.5的InetAddresss,代码简单，速度更快，缺点就是需要稳定的ip地址做测试
 * 备注：有个奇怪的问题，用单元测试的方法测试该方法会超时并返回false，而在app中调用该方法会很快返回true。
 * @param host 主机IP地址，比如"14.215.177.38"(当时获取的百度ip地址)
 * @param timeout 超时时间，默认5s，至少应该在3s以上
 */
fun pingByInetAddress(host: String = "14.215.177.38", timeout: Int = 5000): Boolean {
    return InetAddress.getByName(host).isReachable(timeout)
}

/**
 * 通过ping方式测试网络连通性
 * 该方法是通用方法，优点是可以查看ping的中间过程（但本方法没有用到中间ping的内容），并且只需要传入站点名字即可，
 * 缺点是该方法耗时较长,并且ping的命令不同系统有不同差别（比如Windows 支持-w，mac os不支持）。
 *
 * 注意： ping网址是最最常用的一种判断网络的方式，也是通用方法。这种方法最大的劣势是，耗时很长，
 * 我曾计算过，ping一次的时间大约是10s，两次是11s，每增加一次就多耗时1s。
 * 虽然这种方法耗时较长，但这是目前最常用的方法，而且没有更好的方法取代。
 * @param address ping地址
 * 注意：
 * 在mac设备上，使用terminal执行ping https://www.baidu.com/返回"ping: cannot resolve https://www.baidu.com/: Unknown host"，并且单元测试和用真机运行都返回false。
 * 改为使用"www.baidu.com"后可以ping，在（Mac os）单元测试和真机中都返回true
 *
 * @param pingCount ping的次数
 * @see [Process.waitFor] 返回0，当前网络可用;返回1，需要网页认证的wifi;返回2，当前网络不可用
 * 关于需要网页认证的wifi：警惕360wifi！！！
 * 360wifi本身其实是不需要认证的，连接上网络之后就可以直接使用。但是用方法进行判断时，process.waitFor()返回的却是1。
 * 有一些手机系统（比如魅族），在监测到当前wifi需要网页认证时，会自动打开浏览器。这本身是一个很人性化的设计，
 * 然而会被某些企业利用，在连接上wifi之后，自动跳转到广告页面。
 * 所以大家如果用360wifi进行验证，自求多福，那毕竟是360啊。
 * @return true-当前网络可用
 */
fun pingByCmd(address: String = "www.baidu.com", pingCount: Int = 3): Boolean {
    //ping命令的后面增加 -w 参数后会无法执行， -c -w 是windows的ping 命令格式
    /*
    -c 发送ICMP包的个数
    -i 每次发送数据包中间的间隔时间，单位秒
    -l <前置载入> 设置在送出要求信息之前，先行发出的数据包
    -s 设置数据包的大小
    -t 设置TTL（存活数值）的大小 / TTL : Time to Live该字段指定IP包被路由器丢弃之前允许通过的最大网段数量
    -w deadline 数据包存活最大时间
    -W timeout等待每个响应的最长时间，单位为秒*/
    val p: Process = Runtime.getRuntime().exec("ping -c $pingCount -W 1 $address")//ping网址3次
    val status = p.waitFor()
//        process.waitFor() 返回0，当前网络可用
//        process.waitFor() 返回1，需要网页认证的wifi
//        process.waitFor() 返回2，当前网络不可用
    println("ping by runtime: waitFor = $status")
    return status == 0
}
