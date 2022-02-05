package sixue.naivereader

import android.content.Context
import android.content.Intent
import sixue.naivereader.data.Book
import sixue.naivereader.data.Chapter
import sixue.naivereader.provider.NetProviderCollections.findProviders
import java.io.File

class SmartDownloader(private val context: Context, private val book: Book) {
    fun isDownloaded(chapter: Chapter): Boolean {
        val file = File(chapter.savePath)
        return file.exists()
    }

    private fun downloadChapter(chapter: Chapter) {
        val provider = findProviders(book.siteId!!)
        provider?.let {
            it.downloadChapter(book, chapter)
            val intent = Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH)
            intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
            intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.id)
            intent.putExtra(Utils.INTENT_PARA_PATH, chapter.savePath)
            context.sendBroadcast(intent)
        }
    }

    fun startDownloadContent() {
        Thread { book.buildHelper().downloadContent(context) }.start()
    }

    fun startDownloadChapter(chapter: Chapter) {
        Thread { downloadChapter(chapter) }.start()
    }

    fun getChapterUrl(chapter: Chapter): String {
        val provider = findProviders(book.siteId!!)
        return provider?.getChapterUrl(book, chapter) ?: ""
    }

    fun startDownloadAllChapter() {
        Thread { downloadAllChapter() }.start()
    }

    private fun downloadAllChapter() {
        for (chapter in book.chapterList) {
            if (!isDownloaded(chapter)) {
                downloadChapter(chapter)
            }
        }
        val intent = Intent(Utils.ACTION_DOWNLOAD_ALL_CHAPTER_FINISH)
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
        context.sendBroadcast(intent)
    }
}