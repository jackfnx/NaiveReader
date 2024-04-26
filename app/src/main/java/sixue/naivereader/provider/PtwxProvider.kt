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

class PtwxProvider : NetProvider() {
    override val providerId: String
        get() = "www.piaotian.net"
    override val providerName: String
        get() = "飘天文学"

    override fun search(s: String, context: Context): List<Book> {
        val list: MutableList<Book> = ArrayList()
        try {
            val key = URLEncoder.encode(s, "GBK")
            val url = "https://www.piaotia.com/modules/article/search.php?searchkey=$key"
            val response = Jsoup.connect(url).followRedirects(true).timeout(15000).execute()
            if (url != response.url().toString()) {
                val doc = response.parse()
                val elements = doc.body().select("#content")
                val subDoc = Jsoup.parse(elements.toString())
                var author = "*"
                for (td in subDoc.select("td")) {
                    val t = td.text()
                    Log.i(javaClass.toString(), "td.text=$t")
                    val prefix = "作 者："
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
                book.sources += source
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

    override fun parseBookUrl(s: String, context: Context): String? {
        return null
    }

    override fun loadBookUrl(s: String, context: Context): Book? {
        return null
    }

    override fun downloadContent(book: Book, bookSavePath: String): List<Chapter> {
        val contentUrl = calcBookUrl(book.sitePara!!)
        val content: MutableList<Chapter> = ArrayList()
        try {
            val doc = Jsoup.connect(contentUrl).timeout(5000).get()
            val elements = doc.body().select(".centent")
            for (ch in Jsoup.parse(elements.toString()).select("li")) {
                val title = ch.select("a").text()
                val url = ch.select("a").attr("href").replace("/", "").trim { it <= ' ' }
                if (url.isEmpty() ||
                        url.lowercase(Locale.getDefault()).startsWith("javascript:") ||
                        url.lowercase(Locale.getDefault()).startsWith("window")) {
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
        } catch (e: NullPointerException) {
            Log.e(TAG, "downloadContent ERROR: $contentUrl")
        }
        return content
    }

    override fun downloadChapter(book: Book, chapter: Chapter) {
        val chapterUrl = getChapterUrl(book, chapter)
        try {
            val doc = Jsoup.connect(chapterUrl).timeout(5000).get()
            val s = doc.body().toString()
                    .replace("<script language=\"javascript\">GetFont();</script>", "<div id=\"content\" class=\"fonts_mesne\">")
                    .replace("<!-- 翻页上AD开始 -->", "</div> <!-- 翻页上AD开始 -->")
            val content = Jsoup.parse(s).select("#content").first()!!
            val text = content.html()
            val plainText = Utils.clearHtmlTag(text, arrayOf("h1", "table", "div"))
                    .replace("<br>", "")
                    .replace("&nbsp;", " ")
            Utils.writeText(plainText, chapter.savePath)
        } catch (e: IOException) {
            Log.e(TAG, "downloadChapter ERROR: $chapterUrl")
        } catch (e: NullPointerException) {
            Log.e(TAG, "downloadChapter ERROR: $chapterUrl")
        }
    }

    override fun getChapterUrl(book: Book, chapter: Chapter): String {
        return calcBookUrl(book.sitePara!!) + "/" + chapter.id
    }

    private fun calcChapterSavePath(chapter: Chapter, bookSavePath: String): String {
        return bookSavePath + "/" + chapter.id.replace(".html", ".txt")
    }

    private fun calcPrefix(para: String): String {
        return if (para.length > 3) para.substring(0, para.length - 3) else "0"
    }

    private fun calcBookUrl(para: String): String {
        val prefix = calcPrefix(para)
        return String.format("https://www.piaotia.com/html/%s/%s", prefix, para)
    }

    private fun calcCoverUrl(para: String): String {
        val prefix = calcPrefix(para)
        return String.format("https://www.piaotia.com/files/article/image/%s/%s/%ss.jpg", prefix, para, para)
    }

    private fun parseBookUrl(bookUrl: String): String {
        val l = bookUrl.lastIndexOf('/')
        val r = bookUrl.lastIndexOf(".html")
        return if (l != -1 && r != -1 && l < r) {
            bookUrl.substring(l + 1, r)
        } else {
            ""
        }
    }

    companion object {
        private val TAG = PtwxProvider::class.java.simpleName
    }
}