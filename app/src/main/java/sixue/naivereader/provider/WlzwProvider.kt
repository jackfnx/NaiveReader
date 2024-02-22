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

class WlzwProvider : NetProvider() {
    override val providerId: String
        get() = "www.50zw.la"
    override val providerName: String
        get() = "武林中文[反爬]"

    override fun search(s: String, context: Context): List<Book> {
        // 日你妈的防盗链
        val list: MutableList<Book> = ArrayList()
        try {
            val key = URLEncoder.encode(s, "GBK")
            val url = "https://www.50zw.co/modules/article/search.php?searchkey=$key"
            val response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute()
            if (url != response.url().toString()) {
                val doc = response.parse()
                val elements = doc.body().select(".book_info")
                val subDoc = Jsoup.parse(elements.toString())
                var author = "*"
                for (span in subDoc.select("#info > .options > .item")) {
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

    override fun downloadContent(book: Book, bookSavePath: String): List<Chapter> {
        val content: MutableList<Chapter> = ArrayList()
        val queue: MutableList<String> = ArrayList()
        queue.add("https://www.50zw.co/book/" + book.sitePara!! + "/")
        try {
            while (queue.isNotEmpty()) {
                val contentUrl = queue[0]
                val doc = Jsoup.connect(contentUrl).timeout(5000).get()
                val elements = doc.body().select(".zjlist").first()
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
                if (contentUrl.endsWith("/")) {
                    val select = doc.body().select("select")[0]
                    for (op in Jsoup.parse(select.toString()).select("option")) {
                        val pageUrl = "https://www.50zw.co" + op.attr("value")
                        if (pageUrl != contentUrl) {
                            queue.add(pageUrl)
                        }
                    }
                }
                queue.removeAt(0)
            }
        } catch (e: IOException) {
            Log.e(TAG, "downloadContent ERROR: $queue[0]")
        }
        return content
    }

    override fun downloadChapter(book: Book, chapter: Chapter) {
        val chapterUrl = getChapterUrl(book, chapter)
        try {
            val doc = Jsoup.connect(chapterUrl).timeout(5000).get()
            val text = doc.body().select("#htmlContent").html().replace("<br>", "").replace("&nbsp;", " ")
            Utils.writeText(text, chapter.savePath)
        } catch (e: IOException) {
            Log.e(TAG, "downloadChapter ERROR: $chapterUrl")
        }
    }

    override fun getChapterUrl(book: Book, chapter: Chapter): String {
        return "https://www.50zw.co/book_" + book.sitePara + "/" + chapter.id
    }

    private fun calcChapterSavePath(chapter: Chapter, bookSavePath: String): String {
        return bookSavePath + "/" + chapter.id.replace(".html", ".txt")
    }

    private fun calcCoverUrl(para: String): String {
        val prefix = if (para.length > 3) para.substring(0, para.length - 3) else "0"
        return String.format("https://www.50zw.co/files/article/image/%s/%s/%ss.jpg", prefix, para, para)
    }

    private fun parseBookUrl(bookUrl: String): String {
        val l = bookUrl.lastIndexOf('_')
        val r = bookUrl.length
        return if (l != -1 && l < r) {
            bookUrl.substring(l + 1, r - 1).replace("/", "")
        } else {
            ""
        }
    }

    companion object {
        private val TAG = WlzwProvider::class.java.simpleName
    }
}