package sixue.naivereader.provider

import android.content.Context
import android.util.Log
import org.jsoup.Jsoup
import sixue.naivereader.data.Book
import sixue.naivereader.data.BookKind
import sixue.naivereader.data.Chapter
import sixue.naivereader.data.Source
import sixue.naivereader.helper.OnlineHelper
import java.io.IOException

class HtshuProvider: NetProvider() {
    override val providerId: String
        get() = "www.hetushu.com"
    override val providerName: String
        get() = "和图书"

    override fun search(s: String, context: Context): List<Book> {
        return ArrayList()
    }

    override fun parseBookUrl(s: String, context: Context): String? {
        val reg = Regex("https://www.hetushu.com/book/(\\d+)/index.html")
        return if (reg.matches(s)) {
            s
        } else {
            null
        }
    }

    override fun loadBookUrl(s: String, context: Context): Book? {
        val reg = Regex("https://www.hetushu.com/book/(\\d+)/index.html")
        val mr = reg.find(s) ?: return null
        val (id,) = mr.destructured

        try {
            val response = Jsoup.connect(s).followRedirects(true).timeout(5000).execute()
            if (s != response.url().toString()) {
                Log.d(TAG, response.url().toString())
                // cant reach.
                return null
            } else {
                val doc = response.parse()
                val item = doc.body().select(".book_info").first()!!
                val titleEle = item.select("h2").first()!!
                val authorEle = item.select("div a").first()!!
                val title = titleEle.text()
                val author = authorEle.text()

                val coverUrl = "https://www.hetushu.com" +
                        item.select("img").first()!!.attr("src")

                val book = Book(
                    id = id,
                    title = title,
                    author = author,
                    kind = BookKind.Online,
                    localPath = "",
                )
                val source = Source(
                    id = providerId,
                    para = id
                )
                book.sources += source
                book.siteId = source.id
                book.sitePara = source.para

                val helper = book.buildHelper() as OnlineHelper
                helper.downloadCover(context, coverUrl)
                return book
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    override fun downloadChapter(book: Book, chapter: Chapter) {
        TODO("Not yet implemented")
    }

    override fun downloadContent(book: Book, bookSavePath: String): List<Chapter> {
        TODO("Not yet implemented")
    }

    override fun getChapterUrl(book: Book, chapter: Chapter): String {
        TODO("Not yet implemented")
    }

    companion object {
        private val TAG = HtshuProvider::class.java.simpleName
    }
}