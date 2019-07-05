package sixue.naivereader.provider;

import android.content.Context;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.Utils;
import sixue.naivereader.data.Book;
import sixue.naivereader.data.BookKind;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.data.Source;
import sixue.naivereader.helper.OnlineHelper;

public class PtwxProvider extends NetProvider {

    private static final String TAG = PtwxProvider.class.getSimpleName();

    @Override
    public String getProviderId() {
        return "www.piaotian.net";
    }

    @Override
    public String getProviderName() {
        return "飘天文学";
    }

    @Override
    public List<Book> search(String s, Context context) {
        List<Book> list = new ArrayList<>();
        try {
            String key = URLEncoder.encode(s, "GB2312");
            String url = "https://www.ptwxz.com/modules/article/search.php?searchkey=" + key;
            Connection.Response response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute();
            if (!url.equals(response.url().toString())) {
                Document doc = response.parse();

                Elements elements = doc.body().select("#content");

                Document subDoc = Jsoup.parse(elements.toString());
                String author = "*";
                for (Element td : subDoc.select("td")) {
                    String t = td.text();
                    Log.i(getClass().toString(), "td.text=" + t);

                    final String prefix = "作\u00a0\u00a0\u00a0 者：";
                    if (t.startsWith(prefix)) {
                        author = t.substring(prefix.length());
                        break;
                    }
                }

                String para = parseBookUrl(response.url().toString());
                String coverUrl = calcCoverUrl(para);

                Book book = new Book();
                book.setId(s);
                book.setTitle(s);
                book.setAuthor(author);
                book.setKind(BookKind.Online);

                Source source = new Source();
                source.setId(getProviderId());
                source.setPara(para);
                book.getSources().add(source);

                book.setSiteId(source.getId());
                book.setSitePara(source.getPara());

                OnlineHelper helper = (OnlineHelper) book.buildHelper();
                helper.downloadCover(context, coverUrl);

                list.add(book);
            } else {
                Document doc = response.parse();

                Elements elements = doc.body().select("table.grid");
                for (Element tr : Jsoup.parse(elements.toString()).select("tr")) {
                    Elements tds = tr.select("td.odd");
                    Elements a = tds.select("a");
                    if (tds.size() == 0) {
                        continue;
                    }

                    String para = parseBookUrl(a.attr("href").trim());
                    String coverUrl = calcCoverUrl(para);

                    Book book = new Book();

                    String title = a.text();
                    String id = a.text();
                    String author = tds.get(1).text();

                    book.setId(id);
                    book.setTitle(title);
                    book.setAuthor(author);
                    book.setKind(BookKind.Online);

                    Source source = new Source();
                    source.setId(getProviderId());
                    source.setPara(para);
                    book.getSources().add(source);

                    book.setSiteId(source.getId());
                    book.setSitePara(source.getPara());

                    OnlineHelper helper = (OnlineHelper) book.buildHelper();
                    helper.downloadCover(context, coverUrl);

                    list.add(book);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Chapter> downloadContent(Book book, String bookSavePath) {

        String contentUrl = calcBookUrl(book.getSitePara());
        List<Chapter> content = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(contentUrl).timeout(5000).get();
            Elements elements = doc.body().select(".centent");
            for (Element ch : Jsoup.parse(elements.toString()).select("li")) {
                String title = ch.select("a").text();
                String url = ch.select("a").attr("href").replace("/", "").trim();
                if (url.length() == 0 ||
                        url.toLowerCase().startsWith("javascript:") ||
                        url.toLowerCase().startsWith("window")) {
                    continue;
                }

                final Chapter chapter = new Chapter();
                chapter.setId(url);
                chapter.setTitle(title);
                String chapterSavePath = calcChapterSavePath(chapter, bookSavePath);
                chapter.setSavePath(chapterSavePath);

                content.add(chapter);
            }
        } catch (IOException | NullPointerException e) {
            Log.e(TAG, "downloadContent ERROR: " + contentUrl);
        }
        return content;
    }

    @Override
    public void downloadChapter(Book book, Chapter chapter) {

        String chapterUrl = getChapterUrl(book, chapter);
        try {
            Document doc = Jsoup.connect(chapterUrl).timeout(5000).get();
            String s = doc.body().toString()
                    .replace("<script language=\"javascript\">GetFont();</script>", "<div id=\"content\" class=\"fonts_mesne\">")
                    .replace("<!-- 翻页上AD开始 -->", "</div> <!-- 翻页上AD开始 -->");
            Element content = Jsoup.parse(s).select("#content").first();
            String text = content.html();
            String plainText = Utils.clearHtmlTag(text, new String[] {"h1", "table", "div"})
                    .replace("<br>", "")
                    .replace("&nbsp;", " ");
            Utils.writeText(plainText, chapter.getSavePath());
        } catch (IOException | NullPointerException e) {
            Log.e(TAG, "downloadChapter ERROR: " + chapterUrl);
        }
    }

    @Override
    public String getChapterUrl(Book book, Chapter chapter) {
        return calcBookUrl(book.getSitePara()) + "/" + chapter.getId();
    }

    private String calcChapterSavePath(Chapter chapter, String bookSavePath) {
        return bookSavePath + "/" + chapter.getId().replace(".html", ".txt");
    }

    private String calcPrefix(String para) {
        return para.length() > 3 ? para.substring(0, para.length() - 3) : "0";
    }

    private String calcBookUrl(String para) {
        String prefix = calcPrefix(para);
        return String.format("https://www.ptwxz.com/html/%s/%s", prefix, para);
    }

    private String calcCoverUrl(String para) {
        String prefix = calcPrefix(para);
        return String.format("https://www.ptwxz.com/files/article/image/%s/%s/%ss.jpg", prefix, para, para);
    }

    private String parseBookUrl(String bookUrl) {
        int l = bookUrl.lastIndexOf('/');
        int r = bookUrl.lastIndexOf(".html");
        if (l != -1 && r != -1 && l < r) {
            return bookUrl.substring(l + 1, r);
        } else {
            return "";
        }
    }
}
