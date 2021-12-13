package sixue.naivereader.provider

import android.content.Context
import android.util.Log
import org.jsoup.Jsoup
import sixue.naivereader.Utils
import sixue.naivereader.data.Book
import sixue.naivereader.data.BookKind
import sixue.naivereader.data.Chapter
import sixue.naivereader.data.Source
import sixue.naivereader.helper.OnlineHelper
import java.io.IOException
import java.net.URLEncoder
import java.util.*

class XbqgProvider : NetProvider() {
    override val providerId: String
        get() = "www.xbiquge.cc"
    override val providerName: String
        get() = "笔趣阁cc"

    override fun search(s: String, context: Context): List<Book> {
        val list: MutableList<Book> = ArrayList()
        try {
            val key = URLEncoder.encode(s, "GBK")
            val url = "https://www.xbiquge.so/modules/article/search.php?searchkey=$key"
            val response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute()
            if (url != response.url().toString()) {
                val doc = response.parse()
                var author = "*"
                for (span in doc.select("#info > p")) {
                    val t = span.text()
                    Log.i(javaClass.toString(), "span.text=$t")
                    val prefix = "作者："
                    if (t.startsWith(prefix)) {
                        author = t.substring(prefix.length)
                        break
                    }
                }
                val para = parseBookUrl(response.url().toString())
                val coverUrl = calcCoverUrl(para)
                val book = Book(
                    id = s,
                    title = s,
                    author = author,
                    kind = BookKind.Online,
                    localPath = "",
                )
                val source = Source(
                    id = providerId,
                    para = para,
                )
                book.sources.toMutableList().add(source)
                book.siteId = source.id
                book.sitePara = source.para
                val helper = book.buildHelper() as OnlineHelper
                helper.downloadCover(context, coverUrl)
                list.add(book)
            } else {
                val doc = response.parse()
                val elements = doc.body().select("table.grid")
                for (tr in Jsoup.parse(elements.toString()).select("tr")) {
                    val tds = tr.select("td.odd")
                    val a = tds.select("a")
                    if (tds.size == 0) {
                        continue
                    }
                    val para = parseBookUrl(a.attr("href").trim { it <= ' ' })
                    val coverUrl = calcCoverUrl(para)
                    val title = a.text()
                    val id = a.text()
                    val author = tds[1].text()
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
                    book.sources.toMutableList().add(source)
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

    override fun downloadContent(book: Book, bookSavePath: String): List<Chapter> {
        val para = book.sitePara!!
        val contentUrl = String.format("https://www.xbiquge.cc/book/%s/", para)
        val content: MutableList<Chapter> = ArrayList()
        try {
            val doc = Jsoup.connect(contentUrl).timeout(5000).get()
            val elements = doc.body().select("#list")
            for (ch in Jsoup.parse(elements.toString()).select("dd")) {
                val title = ch.select("a").text()
                val url = ch.select("a").attr("href").replace("/", "").trim { it <= ' ' }
                if (url.isEmpty()) {
                    continue
                }
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
            val text = doc.body().select("#content").html()
            val plainText = text.replace("<br>", "")
                    .replace("&nbsp;", " ")
            Utils.writeText(plainText, chapter.savePath)
        } catch (e: IOException) {
            Log.e(TAG, "downloadChapter ERROR: $chapterUrl")
        }
    }

    override fun getChapterUrl(book: Book, chapter: Chapter): String {
        val para = book.sitePara!!
        return String.format("https://www.xbiquge.cc/book/%s/%s", para, chapter.id)
    }

    private fun calcChapterSavePath(chapter: Chapter, bookSavePath: String): String {
        return bookSavePath + "/" + chapter.id.replace(".html", ".txt")
    }

    private fun calcCoverUrl(para: String): String {
        val prefix = if (para.length > 3) para.substring(0, para.length - 3) else "0"
        return String.format("https://www.xbiquge.cc/files/article/image/%s/%s/%ss.jpg", prefix, para, para)
    }

    private fun parseBookUrl(bookUrl: String): String {
        val bookUrl2 = if (bookUrl.endsWith("/")) bookUrl.substring(0, bookUrl.length - 1) else bookUrl
        val l = bookUrl2.lastIndexOf('/')
        val r = bookUrl2.length
        return if (l != -1 && l < r) {
            bookUrl2.substring(l + 1, r)
        } else {
            ""
        }
    }

    companion object {
        private val TAG = XbqgProvider::class.java.simpleName
    }
}