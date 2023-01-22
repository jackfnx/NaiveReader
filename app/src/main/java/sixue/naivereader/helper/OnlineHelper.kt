package sixue.naivereader.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.Gravity
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import sixue.naivereader.R
import sixue.naivereader.SmartDownloader
import sixue.naivereader.Utils
import sixue.naivereader.data.Book
import sixue.naivereader.data.Chapter
import sixue.naivereader.provider.NetProviderCollections
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class OnlineHelper(private val book: Book) : BookHelper {
    private lateinit var cover: Bitmap
    override fun reloadContent(context: Context): Boolean {
        val bookSavePath = calcBookSavePath(context)
        val json = Utils.readText("$bookSavePath/.CONTENT") ?: return false
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

    override fun downloadContent(context: Context) {
        try {
            val bookSavePath = calcBookSavePath(context)
            val provider = book.siteId?.let { NetProviderCollections.findProviders(it) }
            val content = provider!!.downloadContent(book, bookSavePath)
            if (content.isNotEmpty()) {
                book.chapterList = content
                val mapper = jacksonObjectMapper()
                val json = mapper.writeValueAsString(content)
                Utils.writeText(json, "$bookSavePath/.CONTENT")
            }
            val intent = Intent(Utils.ACTION_DOWNLOAD_CONTENT_FINISH)
            intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
            context.sendBroadcast(intent)
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
    }

    override fun loadCoverBitmap(context: Context): Bitmap {
        if (!this::cover.isInitialized) {
            val saveRootPath = Utils.getSavePathRoot(context)
            val coverSavePath = "${saveRootPath}/books/${book.id}/${book.coverSavePath}"
            val f = File(coverSavePath)
            cover = if (book.coverSavePath.isEmpty() || !f.exists()) {
                Utils.getAutoCover(context, book.title, book.author, 2)
            } else {
                BitmapFactory.decodeFile(coverSavePath)
            }
        }
        return cover
    }

    override fun calcCurrentPosition(seemingIndex: Int): BookHelper.CurrentPosition {
        val idx = reverseIndex(seemingIndex)
        return BookHelper.CurrentPosition(idx, 0)
    }

    override fun getCurrentSeemingIndex(): Int {
        return reverseIndex(book.currentChapterIndex)
    }

    override fun getChapterSize(): Int {
        return book.chapterList.size
    }

    override fun getChapterDescription(
        seemingIndex: Int,
        context: Context
    ): BookHelper.ChapterDescription {
        val index = reverseIndex(seemingIndex)
        val chapter = book.chapterList[index]
        var title = chapter.title
        if (index == book.currentChapterIndex) {
            title += "*"
        }
        val downloader = SmartDownloader(context, book)
        val summary = if (downloader.isDownloaded(chapter)) {
            context.getString(R.string.download)
        } else {
            ""
        }
        return BookHelper.ChapterDescription(title, summary, Gravity.END)
    }

    override fun updateChapterTitleOnPageChange(): BookHelper.UpdateChapterTitleEvent {
        return BookHelper.UpdateChapterTitleEvent(false, "")
    }

    override fun calcTurnPageNewIndex(step: Int): BookHelper.TurnPageNewIndex {
        val i = book.currentChapterIndex + if (step > 0) 1 else -1
        val j = if (step > 0) 0 else Int.MAX_VALUE
        val notOver = (i >= 0 && i < book.chapterList.size)
        return BookHelper.TurnPageNewIndex(notOver, i, j)
    }

     private fun reverseIndex(n: Int): Int {
        var idx = book.chapterList.size - 1 - n
        if (idx < 0) {
            idx = 0
        }
        if (idx >= book.chapterList.size) {
            idx = book.chapterList.size - 1
        }
        return idx
    }

    override fun progressText(context: Context): String {
        val size = book.chapterList.size
        val cp = book.currentChapterIndex
        return if (size <= 0) {
            context.getString(R.string.read_progress_net_predownload)
        } else if (cp + 1 == size) {
            if (book.end) {
                context.getString(R.string.read_progress_net_end)
            } else {
                context.getString(R.string.read_progress_net_allread)
            }
        } else {
            context.getString(R.string.read_progress_net, size - cp - 1)
        }
    }

    override fun readText(chapter: Chapter, context: Context): String {
//        val saveRootPath = Utils.getSavePathRoot(context)
//        val chapterSavePath = "$saveRootPath/books/${book.id}/${chapter.savePath}"
        val chapterSavePath = chapter.savePath
        return Utils.readText(chapterSavePath) ?: "Can't open file."
    }

    private fun calcBookSavePath(context: Context): String {
        val saveRootPath = Utils.getSavePathRoot(context)
        return "$saveRootPath/books/${book.id}/${book.siteId}"
    }

    fun downloadCover(context: Context, coverUrl: String) {
        val bookSavePath = calcBookSavePath(context)
        Utils.mkdir(bookSavePath)
        val coverRelSavePath = "${book.siteId}/cover.jpg"
        val coverSavePath = "$bookSavePath/cover.jpg"
        try {
            if (File(coverSavePath).exists()) {
                book.coverSavePath = coverRelSavePath
                return
            }
            Log.i(javaClass.toString(), "cover:[$coverUrl]=>[$coverRelSavePath] startDownload.")
            val url = URL(coverUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return
            }
            val `is` = conn.inputStream
            val original = BitmapFactory.decodeStream(`is`)
            val scaledBitmap = Utils.createCropBitmap(original, 160, 200)
            val os: OutputStream = FileOutputStream(coverSavePath)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, os)
            os.close()
            `is`.close()
            Log.i(javaClass.toString(), "cover:[$coverUrl]=>[$coverRelSavePath] download finished.")
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }
        book.coverSavePath = coverRelSavePath
        val intent = Intent(Utils.ACTION_DOWNLOAD_COVER_FINISH)
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
        context.sendBroadcast(intent)
    }
}