package sixue.naivereader.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import sixue.naivereader.Utils
import sixue.naivereader.data.Book
import sixue.naivereader.data.Chapter
import sixue.naivereader.data.Packet
import java.io.File
import java.io.IOException
import java.util.*

class PacketHelper(private val book: Book) : BookHelper {
    private lateinit var cover: Bitmap
    override fun reloadContent(context: Context): Boolean {
        val bookSavePath = calcPacketSavePath(context)
        val json = Utils.readTextFromZip(bookSavePath, ".CONTENT") ?: return false
        return try {
            val mapper = jacksonObjectMapper()
            val listType = mapper.typeFactory.constructParametricType(ArrayList::class.java, Chapter::class.java)
            val list = mapper.readValue<List<Chapter>>(json, listType)
            book.chapterList = list
            list.isNotEmpty()
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun calcPacketSavePath(context: Context): String {
        val saveRootPath = Utils.getSavePathRoot(context)
        return saveRootPath + "/packets/" + book.id + ".zip"
    }

    private fun ensurePacketSavePath(context: Context) {
        val saveRootPath = Utils.getSavePathRoot(context)
        val packetsRootPath = "$saveRootPath/packets"
        val dir = File(packetsRootPath)
        if (!dir.exists()) {
            val mk = dir.mkdirs()
            Log.i(TAG, "mkdir:$dir, $mk")
        }
    }

    override fun downloadContent(context: Context) {}
    fun downloadPacket(context: Context, ip: String, callback: Func<String>) {
        ensurePacketSavePath(context)
        Thread {
            val savePath = calcPacketSavePath(context)
            PacketLoader.downloadPacket(ip, savePath, "/book/" + book.id)
            reloadContent(context)
            book.localPath = savePath
            book.currentChapterIndex = book.chapterList.size - 1
            callback.exec(savePath)
        }.start()
    }

    override fun loadCoverBitmap(context: Context): Bitmap {
        if (!this::cover.isInitialized) {
            val bookSavePath = calcPacketSavePath(context)
            val coverBytes = Utils.readBytesFromZip(bookSavePath, "cover.jpg")
            cover = if (coverBytes == null) {
                Utils.getAutoCover(context, book.title, book.author, 3)
            } else {
                BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
            }
        }
        return cover
    }

    fun loadMetaData(context: Context): Packet? {
        val bookSavePath = calcPacketSavePath(context)
        val json = Utils.readTextFromZip(bookSavePath, ".META.json") ?: return null
        return try {
            val mapper = jacksonObjectMapper()
            mapper.readValue(json, Packet::class.java)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    interface Func<T> {
        fun exec(t: T)
    }

    companion object {
        private val TAG = PacketHelper::class.java.simpleName
    }
}