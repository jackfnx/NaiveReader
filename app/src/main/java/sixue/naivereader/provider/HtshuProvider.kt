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
import java.net.URLEncoder

class HtshuProvider: NetProvider() {
    override val providerId: String
        get() = "www.hetushu.com"
    override val providerName: String
        get() = "和图书"

    override fun search(s: String, context: Context): List<Book> {
        val list: MutableList<Book> = ArrayList()
        try {
            val key = URLEncoder.encode(s, "UTF-8")
            val url = "https://www.hetushu.com/search/?keyword=$key"
            val response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute()
            if (url != response.url().toString()) {
                Log.d(TAG, response.url().toString())
                // cant reach.
            } else {
                val doc = response.parse()
                for (item in doc.body().select("dl.list dd")) {
                    val titleEle = item.select("h4 a").first()!!
                    val authorEle = item.select("h4 span").first()!!
                    val title = titleEle.text()
                    val author = authorEle.text()
                    val para = titleEle.attr("href")
                    val id = parseBookUrl(para)
                    val coverUrl = item.select("a img").first()!!.attr("src")

                    val book = Book(
                        id = id,
                        title = title,
                        author = author,
                        kind = BookKind.Online,
                        localPath = "",
                    )
                    val source = Source(
                        id = providerId,
                        para = para
                    )
                    book.sources += source
                    book.siteId = source.id
                    book.sitePara = source.para
                    val helper = book.buildHelper() as OnlineHelper
                    helper.downloadCover(context, coverUrl)
                    list.add(book)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return list
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

    private fun parseBookUrl(bookUrl: String): String {
        val fs = bookUrl.split("/")
        return if (fs.size >=3 && fs[3] == "index.html") {
            fs[2]
        } else {
            ""
        }
    }

    companion object {
        private val TAG = HtshuProvider::class.java.simpleName
    }
}