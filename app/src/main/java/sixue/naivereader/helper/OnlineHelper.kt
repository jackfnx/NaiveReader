package sixue.naivereader.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
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
            val mapper = ObjectMapper()
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
                val mapper = ObjectMapper()
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
            val coverSavePath = saveRootPath + "/" + book.coverSavePath
            val f = File(coverSavePath)
            cover = if (book.coverSavePath.isEmpty() || !f.exists()) {
                Utils.getAutoCover(context, book.title, book.author, 2)
            } else {
                BitmapFactory.decodeFile(coverSavePath)
            }
        }
        return cover
    }

    private fun calcBookSavePath(context: Context): String {
        return Utils.getSavePathRoot(context) + "/" + calcRelBookSavePath()
    }

    private fun calcRelBookSavePath(): String {
        return "books/" + book.id + "/" + book.siteId
    }

    fun downloadCover(context: Context, coverUrl: String) {
        val bookSavePath = calcBookSavePath(context)
        Utils.mkdir(bookSavePath)
        val bookRelSavePath = calcRelBookSavePath()
        val coverRelSavePath = "$bookRelSavePath/cover.jpg"
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