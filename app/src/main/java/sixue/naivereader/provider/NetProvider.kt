package sixue.naivereader.provider

import android.content.Context
import sixue.naivereader.data.Book
import sixue.naivereader.data.Chapter

abstract class NetProvider {
    var isActive = true
    abstract val providerId: String
    abstract val providerName: String
    abstract fun search(s: String, context: Context): List<Book>
    abstract fun downloadContent(book: Book, bookSavePath: String): List<Chapter>
    abstract fun downloadChapter(book: Book, chapter: Chapter)
    abstract fun getChapterUrl(book: Book, chapter: Chapter): String
}