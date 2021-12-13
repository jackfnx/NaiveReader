package sixue.naivereader

import android.text.TextUtils
import android.util.Log
import sixue.naivereader.helper.PacketLoader.testServer
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 局域网扫描设备工具类
 * @author zihao
 * @since  2015年9月10日 下午3:36:40
 * @version v1.0
 */
class ScanDeviceTool : AutoCloseable {
    /** 线程池对象  */
    private var executor: ExecutorService? = null

    /**
     * 扫描局域网内ip，尝试找到服务器
     *
     * @return String
     */
    private fun tryToFind(): String? {
        val ips: MutableList<String> = ArrayList()
        executor = Executors.newCachedThreadPool()
        for (devAddress in hostIps) {
            // 获取本地ip前缀
            val locAddress = getLocAddress(devAddress)
            Log.d(TAG, "开始扫描设备，扫描范围为：$locAddress*")
            if (TextUtils.isEmpty(locAddress)) {
                Log.e(TAG, "扫描失败，请检查 WiFi 网络")
                continue
            }

            // 新建线程池
            for (i in 1..254) {
                val run = Runnable {
                    val currentIp = locAddress + i
                    // 如果与本机IP地址相同,跳过
                    if (devAddress == currentIp) return@Runnable
                    if (testServer(currentIp)) {
                        Log.d(TAG, "扫描成功，IP地址为：$currentIp")
                        ips.add(currentIp)
                    }
                }
                executor!!.execute(run)
            }
        }
        executor!!.shutdown()
        while (true) {
            try {
                // 找到了
                if (ips.size != 0) {
                    executor = null
                    return ips[0]
                }
                // 扫描结束
                if (executor!!.isTerminated) {
                    executor = null
                    return null
                }
            } catch (e: Exception) {
                // do nothing
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                // do nothing
            }
        }
    }

    /**
     * close
     */
    override fun close() {
        if (executor != null) {
            executor!!.shutdownNow()
        }
    }

    /**
     * 获取本机IP前缀
     *
     * @param devAddress 本机ip
     * @return String
     */
    private fun getLocAddress(devAddress: String): String? {
        return if (!TextUtils.isEmpty(devAddress)) {
            devAddress.substring(0, devAddress.lastIndexOf(".") + 1)
        } else null
    }

    companion object {
        private val TAG = ScanDeviceTool::class.java.simpleName

        /**
         * 扫描局域网内ip，找到对应服务器
         * @return ip
         */
        fun scan(): String? {
            ScanDeviceTool().use { scanDeviceTool -> return scanDeviceTool.tryToFind() }
        }// skip ipv6
        // 127.0.0.1
        /**
         * 获取ip地址
         *
         * @return List<String>
        </String> */
        private val hostIps: List<String>
            get() {
                val hostIps: MutableList<String> = ArrayList()
                try {
                    val nis: Enumeration<*> = NetworkInterface.getNetworkInterfaces()
                    var ia: InetAddress
                    while (nis.hasMoreElements()) {
                        val ni = nis.nextElement() as NetworkInterface
                        val ias = ni.inetAddresses
                        while (ias.hasMoreElements()) {
                            ia = ias.nextElement()
                            // skip ipv6
                            if (ia is Inet6Address) {
                                continue
                            }
                            // 127.0.0.1
                            if (ia.isLoopbackAddress) {
                                continue
                            }
                            val ip = ia.hostAddress
                            hostIps.add(ip)
                        }
                    }
                } catch (e: SocketException) {
                    Log.i(TAG, "SocketException")
                    e.printStackTrace()
                }
                return hostIps
            }
    }
}