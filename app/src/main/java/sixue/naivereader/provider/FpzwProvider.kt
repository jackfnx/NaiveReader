package sixue.naivereader.provider

import android.content.Context
import android.util.Log
import org.jsoup.Jsoup
import sixue.naivereader.Utils
import sixue.naivereader.data.Book
import sixue.naivereader.data.Chapter
import java.io.IOException
import java.util.*

class FpzwProvider : NetProvider() {

    override val providerId: String
        get() = "www.fpzw.com"

    override val providerName: String
        get() = "富品中文[已挂]"

    override fun search(s: String, context: Context): List<Book> {
        // 似乎over了，干
//        List<Book> list = new ArrayList<>();
//        try {
//            String key = URLEncoder.encode(s, "UTF-8");
//            String url = "https://m.fpzw.cc/case.php?m=search&key=" + key;
//            Connection.Response response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute();
//            if (!url.equals(response.url().toString())) {
//                Document doc = response.parse();
//
//                Elements elements = doc.select("#title > h2 > em > a");
//                String author = elements.first().text();
//
//                String para = parseBookUrl(response.url().toString());
//                String coverUrl = calcCoverUrl(para);
//
//                Book book = new Book();
//                book.setId(s);
//                book.setTitle(s);
//                book.setAuthor(author);
//                book.setKind(BookKind.Online);
//
//                Source source = new Source();
//                source.setId(getProviderId());
//                source.setPara(para);
//                book.getSources().add(source);
//
//                book.setSiteId(source.getId());
//                book.setSitePara(source.getPara());
//
//                OnlineHelper helper = (OnlineHelper) book.buildHelper();
//                helper.downloadCover(context, coverUrl);
//
//                list.add(book);
//            } else {
//                Document doc = response.parse();
//
//                Elements elements = doc.body().select("#main > #newscontent > .1 > ul > li");
//                for (Element li : elements) {
//                    Elements a = li.select(".s2 > a");
//                    Elements au = li.select(".s5");
//
//                    String para = parseBookUrl(a.attr("href").trim());
//                    String coverUrl = calcCoverUrl(para);
//
//                    Book book = new Book();
//                    String title = a.text();
//                    String id = a.text();
//                    String author = au.get(0).text();
//                    book.setId(id);
//                    book.setTitle(title);
//                    book.setAuthor(author);
//                    book.setKind(BookKind.Online);
//
//                    Source source = new Source();
//                    source.setId(getProviderId());
//                    source.setPara(para);
//                    book.getSources().add(source);
//
//                    book.setSiteId(source.getId());
//                    book.setSitePara(source.getPara());
//
//                    OnlineHelper helper = (OnlineHelper) book.buildHelper();
//                    helper.downloadCover(context, coverUrl);
//
//                    list.add(book);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return ArrayList()
    }

    override fun parseBookUrl(s: String, context: Context): String? {
        return null
    }

    override fun loadBookUrl(s: String, context: Context): Book? {
        return null
    }

    override fun downloadContent(book: Book, bookSavePath: String): List<Chapter> {
        val para = book.sitePara
        val prefix = if (para!!.length > 3) para.substring(0, para.length - 3) else "0"
        val contentUrl = String.format("https://www.2kxs.com/xiaoshuo/%s/%s/", prefix, para)
        var content: MutableList<Chapter> = ArrayList()
        try {
            val doc = Jsoup.connect(contentUrl).timeout(5000).get()
            val elements = doc.body().select(".book")
            for (ch in Jsoup.parse(elements.toString()).select("dd")) {
                val title = ch.select("a").text()
                val url = ch.select("a").attr("href").replace("/", "").trim { it <= ' ' }
                if (url.isEmpty()) {
                    continue
                }
                val chapter = Chapter(id=url, title=title)
                val chapterSavePath = calcChapterSavePath(chapter, bookSavePath)
                chapter.savePath = chapterSavePath
                content.add(chapter)
            }
        } catch (e: IOException) {
            Log.e(TAG, "downloadContent ERROR: $contentUrl")
        }
        content = if (content.size >= 4) content.subList(4, content.size) else content
        return content
    }

    override fun downloadChapter(book: Book, chapter: Chapter) {
        val chapterUrl = getChapterUrl(book, chapter)
        try {
            val doc = Jsoup.connect(chapterUrl).timeout(5000).get()
            val text = doc.body().select(".text").html()
            val plainText = Utils.clearHtmlTag(text, arrayOf("a", "script", "font", "strong"))
                    .replace("<br>", "")
                    .replace("&nbsp;", " ")
            Utils.writeText(plainText, chapter.savePath)
        } catch (e: IOException) {
            Log.e(TAG, "downloadChapter ERROR: $chapterUrl")
        }
    }

    override fun getChapterUrl(book: Book, chapter: Chapter): String {
        val para = book.sitePara
        val prefix = if (para!!.length > 3) para.substring(0, para.length - 3) else "0"
        return String.format("https://www.2kxs.com/xiaoshuo/%s/%s/%s", prefix, para, chapter.id)
    }

    private fun calcChapterSavePath(chapter: Chapter, bookSavePath: String): String {
        return bookSavePath + "/" + chapter.id.replace(".html", ".txt")
    }

    private fun calcCoverUrl(para: String): String {
        val prefix = if (para.length > 3) para.substring(0, para.length - 3) else "0"
        return String.format("https://www.2kxs.com/files/article/image/%s/%s/%ss.jpg", prefix, para, para)
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
        private val TAG = FpzwProvider::class.java.simpleName
    }
}