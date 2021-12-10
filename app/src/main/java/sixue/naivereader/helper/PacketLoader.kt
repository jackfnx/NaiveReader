package sixue.naivereader.helper

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import sixue.naivereader.data.Book
import sixue.naivereader.data.BookKind
import sixue.naivereader.data.Packet
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object PacketLoader {
    private val TAG = PacketLoader::class.java.simpleName
    private const val HTTP_PORT = 5000
    private const val INIT_URL = "/books"
    @JvmStatic
    fun testServer(ip: String): Boolean {
        try {
            val url = URL(String.format(Locale.PRC, "http://%s:%d%s",
                    ip, HTTP_PORT, INIT_URL))
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Connection", "close")
            conn.doInput = true
            conn.connect()
            val c = conn.responseCode
            if (c == HttpURLConnection.HTTP_OK) {
                return true
            }
        } catch (e: Exception) {
            // do nothing
        }
        return false
    }

    @JvmStatic
    fun loadPackets(ip: String): List<Packet> {
        return try {
            val url = URL(String.format(Locale.PRC, "http://%s:%d%s", ip, HTTP_PORT, INIT_URL))
            Log.i(TAG, "GET:$url")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Connection", "close")
            conn.doInput = true
            conn.connect()
            val c = conn.responseCode
            if (c != HttpURLConnection.HTTP_OK) {
                Log.i(TAG, "HTTP Error:$c")
                return ArrayList()
            }
            val `is` = conn.inputStream
            val isr = InputStreamReader(`is`)
            val br = BufferedReader(isr)
            var line: String?
            val buf = StringBuilder()
            while (br.readLine().also { line = it } != null) {
                buf.append(line)
            }
            val json = buf.toString()
            val mapper = jacksonObjectMapper()
            val listType = mapper.typeFactory.constructParametricType(ArrayList::class.java, Packet::class.java)
            val packets = mapper.readValue<List<Packet>>(json, listType)
            Log.i(TAG, "GET JSON: " + packets.size + " packets.")
            packets
        } catch (e: IOException) {
            e.printStackTrace()
            ArrayList()
        }
    }

    fun downloadPacket(ip: String, savePath: String, packetUrl: String?) {
        try {
            val url = URL(String.format(Locale.PRC, "http://%s:%d%s", ip, HTTP_PORT, packetUrl))
            val conn = url.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return
            }
            conn.inputStream.use { inputStream ->
                val buffer = ByteArray(1024)
                ByteArrayOutputStream().use { bos ->
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        bos.write(buffer, 0, len)
                    }
                    val getData = bos.toByteArray()
                    val file = File(savePath)
                    FileOutputStream(file).use { fos -> fos.write(getData) }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun createBook(packet: Packet): Book {
        return Book(
            id = packet.key,
            title = packet.title,
            author = packet.author,
            kind = BookKind.Packet,
        )
    }
}