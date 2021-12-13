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

class QbxsProvider : NetProvider() {
    override val providerId: String
        get() = "www.quanben.io"
    override val providerName: String
        get() = "全本小说"

    override fun search(s: String, context: Context): List<Book> {
        val list: MutableList<Book> = ArrayList()
        try {
            val key = URLEncoder.encode(s, "UTF-8")
            val url = "http://www.quanben.io/index.php?c=book&a=search&keywords=$key"
            val response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute()
            if (url != response.url().toString()) {
                Log.d(TAG, response.url().toString())
                // cant reach.
            } else {
                val doc = response.parse()
                for (item in doc.body().select(".list2")) {
                    val titleEle = item.select("h3 a[itemprop=url]").first()
                    val authorEle = item.select("p span[itemprop=author]").first()
                    val coverEle = item.select("img").first()
                    val title = titleEle.text()
                    val id = titleEle.text()
                    val author = authorEle.text()
                    val para = titleEle.attr("href")
                    val coverUrl = coverEle.attr("src")
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
        val contentUrl = String.format("http://www.quanben.io%slist.html", para)
        val content: MutableList<Chapter> = ArrayList()
        try {
            val doc = Jsoup.connect(contentUrl).timeout(5000).get()
            val elements = doc.body().select(".list3")
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
            var myJs: String? = null
            val doc = Jsoup.connect(chapterUrl).timeout(5000).get()
            for (jsEle in doc.body().select("script[type=text/javascript]")) {
                val js = jsEle.html()
                if (js.startsWith("setTimeout(\"ajax_post(")) {
                    myJs = js
                    break
                }
            }
            if (myJs == null) {
                return
            }
            myJs = myJs.substring("setTimeout(\"ajax_post(".length)
            myJs = myJs.substring(0, myJs.indexOf("\""))
            val paras = myJs.replace("'", "").split(",".toRegex()).toTypedArray()
            if (paras.size < 10) {
                return
            }
            val ajaxUrl = String.format("http://www.quanben.io/index.php?c=%s&a=%s", paras[0], paras[1])
            val tm = "" + Date().time
            val doc2 = Jsoup.connect(ajaxUrl)
                    .referrer(chapterUrl)
                    .data(paras[2], paras[3]) // pinyin, duorushenyuan
                    .data(paras[4], paras[5]) // id, 1
                    .data(paras[6], paras[7]) // "sky", "bce7cae095b38a070b33746694953743"
                    .data(paras[8], paras[9]) // "t", "1562330872"
                    .data("_type", "ajax")
                    .data("rndval", tm)
                    .timeout(5000).post()
            val text = doc2.html()
            val plainText = Utils.clearHtmlTag(text, arrayOf())
                    .replace("<p>", "").replace("</p>", "\n")
            Utils.writeText(plainText, chapter.savePath)
        } catch (e: IOException) {
            Log.e(TAG, "downloadChapter ERROR: $chapterUrl")
        }
    }

    override fun getChapterUrl(book: Book, chapter: Chapter): String {
        return "http://www.quanben.io" + chapter.id
    }

    private fun calcChapterSavePath(chapter: Chapter, bookSavePath: String): String {
        return bookSavePath + "/" + chapter.id.replace("/", "_").replace(".html", ".txt")
    }

    companion object {
        private val TAG = QbxsProvider::class.java.simpleName
    }
}