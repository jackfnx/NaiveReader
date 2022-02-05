package sixue.naivereader.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Gravity
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import sixue.naivereader.R
import sixue.naivereader.Utils
import sixue.naivereader.data.Book
import sixue.naivereader.data.Chapter
import java.io.IOException
import java.util.ArrayList

class ArchiveHelper(private val book: Book) : BookHelper {
    private lateinit var cover: Bitmap
    override fun reloadContent(context: Context): Boolean {
        if (book.chapterList.isEmpty()) {
            val bookSavePath = calcArchiveSavePath(context)
            val contentRelPath = "${book.siteId}/.CONTENT"
            val json = Utils.readTextFromZip(bookSavePath, contentRelPath) ?: return false
            return try {
                val mapper = jacksonObjectMapper()
                val listType = mapper.typeFactory.constructParametricType(
                    ArrayList::class.java,
                    Chapter::class.java
                )
                val list = mapper.readValue<List<Chapter>>(json, listType)
                book.chapterList = list
                list.isNotEmpty()
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        } else {
            return true
        }
    }

    private fun calcArchiveSavePath(context: Context): String {
        val saveRootPath = Utils.getSavePathRoot(context)
        return saveRootPath + "/archives/" + book.id + ".zip"
    }

    override fun downloadContent(context: Context) {
    }

    override fun loadCoverBitmap(context: Context): Bitmap {
        if (!this::cover.isInitialized) {
            val bookSavePath = calcArchiveSavePath(context)
            val coverBytes = Utils.readBytesFromZip(bookSavePath, book.coverSavePath)
            cover = if (coverBytes == null) {
                Utils.getAutoCover(context, book.title, book.author, 3)
            } else {
                BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
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
        val summary = context.getString(R.string.download)
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
        if (reloadContent(context)) {
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
        } else {
            return context.getString(R.string.read_progress_net_predownload)
        }
    }

    override fun readText(chapter: Chapter, context: Context): String {
        val bookSavePath = calcArchiveSavePath(context)
        return Utils.readTextFromZip(bookSavePath, chapter.savePath) ?: "Can't open file."
    }
}
