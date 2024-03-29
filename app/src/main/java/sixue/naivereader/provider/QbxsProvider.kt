package sixue.naivereader.provider

import android.content.Context
import android.util.Log
import org.jsoup.Jsoup
import sixue.naivereader.Utils
import sixue.naivereader.data.Book
import sixue.naivereader.data.BookKind
import sixue.naivereader.data.Chapter
import sixue.naivereader.data.Source
import java.io.IOException
import java.net.URLEncoder

class QbxsProvider : NetProvider() {
    override val providerId: String
        get() = "www.quanben.io"
    override val providerName: String
        get() = "全本小说"

    override fun search(s: String, context: Context): List<Book> {
        val list: MutableList<Book> = ArrayList()
        try {
            val key = URLEncoder.encode(s, "UTF-8")
            val url = "https://www.quanben-xiaoshuo.com/?c=book&a=search&keyword=$key"
            val response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute()
            if (url != response.url().toString()) {
                Log.d(TAG, response.url().toString())
                // cant reach.
            } else {
                val doc = response.parse()
                for (item in doc.body().select(".book")) {
                    val titleEle = item.select("h1[itemprop=name] a").first()!!
                    val authorEle = item.select("p span[itemprop=author]").first()!!
                    val title = titleEle.text()
                    val id = titleEle.text()
                    val author = authorEle.text()
                    val para = titleEle.attr("href")
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
                    list.add(book)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return list
    }

    override fun parseBookUrl(s: String, context: Context): String? {
        return null
    }

    override fun loadBookUrl(s: String, context: Context): Book? {
        return null
    }

    override fun downloadContent(book: Book, bookSavePath: String): List<Chapter> {
        val para = book.sitePara!!
        val contentUrl = String.format("https://www.quanben-xiaoshuo.com%sxiaoshuo.html", para)
        val content: MutableList<Chapter> = ArrayList()
        try {
            val doc = Jsoup.connect(contentUrl).timeout(5000).get()
            val elements = doc.body().select(".list")
            for (ch in Jsoup.parse(elements.toString()).select("li[itemprop=itemListElement]")) {
                val title = ch.select("a").text()
                val url = ch.select("a").attr("href").trim { it <= ' ' }
                if (url.isEmpty()) {
                    continue
                }
//                val id = url.substring(url.lastIndexOf("/"))
                val chapter = Chapter(
                    id = url,
                    title = title,
                )
                val chapterSavePath = calcChapterSavePath(chapter, bookSavePath)
                chapter.savePath = chapterSavePath
                content.add(chapter)
            }
        } catch (e: IOException) {
            Log.e(TAG, "downloadContent ERROR: $contentUrl")
        }
        return content
    }

    override fun downloadChapter(book: Book, chapter: Chapter) {
        val chapterUrl = getChapterUrl(book, chapter)
        try {
            val doc = Jsoup.connect(chapterUrl).timeout(5000).get()
            val articleBody = doc.body().select("#articlebody").first()!!
            val text = articleBody.html()
            val plainText = Utils.clearHtmlTag(text, arrayOf())
                    .replace("<p>", "").replace("</p>", "\n")
            Utils.writeText(plainText, chapter.savePath)
        } catch (e: IOException) {
            Log.e(TAG, "downloadChapter ERROR: $chapterUrl")
        }
    }

    override fun getChapterUrl(book: Book, chapter: Chapter): String {
        return "https://www.quanben-xiaoshuo.com" + chapter.id
    }

    private fun calcChapterSavePath(chapter: Chapter, bookSavePath: String): String {
        return bookSavePath + "/" + chapter.id.replace("/", "_").replace(".html", ".txt")
    }

    companion object {
        private val TAG = QbxsProvider::class.java.simpleName
    }
}