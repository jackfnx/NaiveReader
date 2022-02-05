package sixue.naivereader.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.Gravity
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import sixue.naivereader.R
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

    override fun calcCurrentPosition(seemingIndex: Int): BookHelper.CurrentPosition {
        return BookHelper.CurrentPosition(seemingIndex, 0)
    }

    override fun getCurrentSeemingIndex(): Int {
        return book.currentChapterIndex
    }

    override fun getChapterSize(): Int {
        return book.chapterList.size
    }

    override fun getChapterDescription(
        seemingIndex: Int,
        context: Context
    ): BookHelper.ChapterDescription {

        val index = seemingIndex
        val chapter = book.chapterList[index]
        var title = chapter.title
        if (index == book.currentChapterIndex) {
            title += "*"
        }
        val summary = context.getString(R.string.download)
        return BookHelper.ChapterDescription(title, summary, Gravity.END)
    }

    override fun updateChapterTitleOnPageChange(): BookHelper.UpdateChapterTitleEvent {
        return BookHelper.UpdateChapterTitleEvent(false, "")
    }

    override fun calcTurnPageNewIndex(step: Int): BookHelper.TurnPageNewIndex {
        val i = book.currentChapterIndex + if (step > 0) -1 else 1
        val j = if (step > 0) 0 else Int.MAX_VALUE
        val notOver = (i >= 0 && i < book.chapterList.size)
        return BookHelper.TurnPageNewIndex(notOver, i, j)
    }

    override fun progressText(context: Context): String {
        val cp = book.currentChapterIndex
        return context.getString(R.string.read_progress_net, cp)
    }

    override fun readText(chapter: Chapter, context: Context): String {
        return Utils.readTextFromZip(book.localPath!!, chapter.savePath) ?: "Can't open file."
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

    fun freezeRead(): Int {
        return book.chapterList.size - 1 - book.currentChapterIndex
    }

    fun unfreezeRead(read: Int): Int {
        var idx = book.chapterList.size - 1 - read
        if (idx < 0) {
            idx = 0
        }
        if (idx >= book.chapterList.size) {
            idx = book.chapterList.size - 1
        }
        return idx
    }

    interface Func<T> {
        fun exec(t: T)
    }

    companion object {
        private val TAG = PacketHelper::class.java.simpleName
    }
}