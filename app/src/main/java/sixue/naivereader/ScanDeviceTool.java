package sixue.naivereader;

import android.text.TextUtils;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 局域网扫描设备工具类
 * @author zihao
 * @since  2015年9月10日 下午3:36:40
 * @version v1.0
 */
public class ScanDeviceTool implements AutoCloseable {

    private static final String TAG = ScanDeviceTool.class.getSimpleName();

    /** 线程池对象 **/
    private ExecutorService executor;

    /**
     * 扫描局域网内ip，找到对应服务器
     * @return ip
     */
    static String scan() {
        try (ScanDeviceTool scanDeviceTool = new ScanDeviceTool()) {
            return scanDeviceTool.tryToFind();
        }
    }

    /**
     * 扫描局域网内ip，尝试找到服务器
     *
     * @return String
     */
    private String tryToFind() {
        final List<String> ips = new ArrayList<>();

        executor = Executors.newCachedThreadPool();

        for (final String devAddress : getHostIps()) {
            // 获取本地ip前缀
            final String locAddress = getLocAddress(devAddress);
            Log.d(TAG, "开始扫描设备，扫描范围为：" + locAddress + "*");

            if (TextUtils.isEmpty(locAddress)) {
                Log.e(TAG, "扫描失败，请检查 WiFi 网络");
                continue;
            }

            // 新建线程池
            for (int i = 1; i < 255; i++) {
                final int lastAddress = i;

                Runnable run = new Runnable() {

                    @Override
                    public void run() {
                        String currentIp = locAddress + lastAddress;
                        // 如果与本机IP地址相同,跳过
                        if (devAddress.equals(currentIp))
                            return;

                        try {
                            URL url = new URL(String.format(Locale.PRC, "http://%s:%d%s",
                                    currentIp, PacketLoader.HTTP_PORT, PacketLoader.INIT_URL));
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestProperty("Connection", "close");
                            conn.setDoInput(true);
                            conn.connect();
                            int c = conn.getResponseCode();
                            if (c == HttpURLConnection.HTTP_OK) {
                                Log.d(TAG, "扫描成功，IP地址为：" + currentIp);
                                ips.add(currentIp);
                            }
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                };

                executor.execute(run);
            }
        }

        executor.shutdown();

        while (true) {
            try {
                // 找到了
                if (ips.size() != 0) {
                    executor = null;
                    return ips.get(0);
                }
                // 扫描结束
                if (executor.isTerminated()) {
                    executor = null;
                    return null;
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO: handle exception
            }
        }
    }

    /**
     * close
     */
    @Override
    public void close() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 获取ip地址
     *
     * @return List<String>
     */
    private static List<String> getHostIps() {

        List<String> hostIps = new ArrayList<>();
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    // skip ipv6
                    if (ia instanceof Inet6Address) {
                        continue;
                    }
                    // 127.0.0.1
                    if (ia.isLoopbackAddress()) {
                        continue;
                    }
                    String ip = ia.getHostAddress();
                    hostIps.add(ip);
                }
            }
        } catch (SocketException e) {
            Log.i(TAG, "SocketException");
            e.printStackTrace();
        }
        return hostIps;
    }

    /**
     * 获取本机IP前缀
     *
     * @param devAddress 本机ip
     * @return String
     */
    private String getLocAddress(String devAddress) {
        if (!TextUtils.isEmpty(devAddress)) {
            return devAddress.substring(0, devAddress.lastIndexOf(".") + 1);
        }
        return null;
    }
}