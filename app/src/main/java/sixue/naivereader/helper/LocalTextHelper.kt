package sixue.naivereader.helper

import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import sixue.naivereader.R
import sixue.naivereader.Utils
import sixue.naivereader.data.Book
import sixue.naivereader.data.Chapter

class LocalTextHelper(private val book: Book) : BookHelper {
    private lateinit var cover: Bitmap
    override fun reloadContent(context: Context): Boolean {
        val text = Utils.readExternalText(context, book.localPath!!) ?: "Can't open file."
        book.localChapterNodes = LocalTextLoader.calcChapterNodes(text)
        book.wordCount = text.length
        val titles = ArrayList<String>()
        val summaries = ArrayList<String>()
        for (i in book.localChapterNodes.indices) {
            val node = book.localChapterNodes[i]
            var end = text.indexOf('\n', node)
            end = if (end < 0) book.wordCount else end
            val s = text.substring(node, end)
            titles.add(s)
            val sumStart = node + s.length
            val sumEnd = if (sumStart + MAX_SUMMARY_LENGTH > book.wordCount) book.wordCount else sumStart + MAX_SUMMARY_LENGTH
            val sum = text.substring(sumStart, sumEnd).trim { it <= ' ' }.replace('\n', ' ')
            summaries.add(sum)
        }
        book.localChapterTitles = titles
        book.localChapterSummaries = summaries
        return true
    }

    override fun downloadContent(context: Context) {}
    override fun loadCoverBitmap(context: Context): Bitmap {
        if (!this::cover.isInitialized) {
            cover = Utils.getAutoCover(context, book.title, book.author, 1)
        }
        return cover
    }

    override fun calcCurrentPosition(seemingIndex: Int): BookHelper.CurrentPosition {
        val currPos = book.localChapterNodes[seemingIndex]
        return BookHelper.CurrentPosition(0, currPos)
    }

    override fun getCurrentSeemingIndex(): Int {
        for (i in book.localChapterNodes.indices) {
            val node = book.localChapterNodes[i]
            val next =
                if (i + 1 < book.localChapterNodes.size) book.localChapterNodes[i + 1] else Int.MAX_VALUE
            if (book.currentPosition in node until next) {
                return i
            }
        }
        return 0
    }

    override fun getChapterSize(): Int {
        return book.localChapterNodes.size
    }

    override fun getChapterDescription(
        seemingIndex: Int,
        context: Context
    ): BookHelper.ChapterDescription {

        val s = if (seemingIndex == getCurrentSeemingIndex()) {
            book.localChapterTitles[seemingIndex] + "*"
        } else {
            book.localChapterTitles[seemingIndex]
        }
        val sum = book.localChapterSummaries[seemingIndex]
        return BookHelper.ChapterDescription(s, sum, Gravity.START)
    }

    override fun updateChapterTitleOnPageChange(): BookHelper.UpdateChapterTitleEvent {
        val seemingIndex = book.buildHelper().getCurrentSeemingIndex()
        val title = if (0 <= seemingIndex && seemingIndex < book.localChapterTitles.size)
            book.localChapterTitles[getCurrentSeemingIndex()]
        else
            book.title
        return BookHelper.UpdateChapterTitleEvent(true, title)
    }

    override fun calcTurnPageNewIndex(step: Int): BookHelper.TurnPageNewIndex {
        return BookHelper.TurnPageNewIndex(false, 0, 0)
    }

    override fun progressText(context: Context): String {
        val cp = book.currentPosition
        val wc = book.wordCount
        return if (wc <= 0) {
            context.getString(R.string.read_progress_local_unread)
        } else {
            context.getString(R.string.read_progress_local, cp.toFloat() / wc.toFloat() * 100f)
        }
    }

    override fun readText(chapter: Chapter, context: Context): String {
        return Utils.readExternalText(context, book.localPath!!) ?: "Can't open file."
    }

    companion object {
        private const val MAX_SUMMARY_LENGTH = 40
    }
}