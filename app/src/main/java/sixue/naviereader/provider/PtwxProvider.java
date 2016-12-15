package sixue.naviereader.provider;

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

import sixue.naviereader.SmartDownloader;
import sixue.naviereader.Utils;
import sixue.naviereader.data.Book;
import sixue.naviereader.data.Chapter;
import sixue.naviereader.data.Source;

public class PtwxProvider extends NetProvider {

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
            String url = "http://www.piaotian.net/modules/article/search.php?searchkey=" + key;
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
                book.setLocal(false);

                Source source = new Source();
                source.setId(getProviderId());
                source.setPara(para);
                book.getSources().add(source);

                book.setSiteId(source.getId());
                book.setSitePara(source.getPara());

                SmartDownloader downloader = new SmartDownloader(context, book);
                downloader.startDownloadCover(coverUrl);

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
                    book.setLocal(false);

                    Source source = new Source();
                    source.setId(getProviderId());
                    source.setPara(para);
                    book.getSources().add(source);

                    book.setSiteId(source.getId());
                    book.setSitePara(source.getPara());

                    SmartDownloader downloader = new SmartDownloader(context, book);
                    downloader.startDownloadCover(coverUrl);

                    list.add(book);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void downloadContent(Book book, String bookSavePath) {
        try {
            Document doc = Jsoup.connect(calcBookUrl(book.getSitePara())).timeout(5000).get();
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
                chapter.setPara("");
                String chapterSavePath = calcChapterSavePath(chapter, bookSavePath);
                chapter.setSavePath(chapterSavePath);

                book.getChapterList().add(chapter);
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void downloadChapter(Book book, Chapter chapter) {

        try {
            Document doc = Jsoup.connect(calcBookUrl(book.getSitePara()) + "/" + chapter.getId()).timeout(5000).get();
            String s = doc.body().toString()
                    .replace("<script language=\"javascript\">GetFont();</script>", "<div id=\"content\" class=\"fonts_mesne\">")
                    .replace("<!-- 翻页上AD开始 -->", "</div> <!-- 翻页上AD开始 -->");
            Element content = Jsoup.parse(s).select("#content").first();
            content.select("h1").remove();
            content.select("table").remove();
            String text = content.outerHtml().replace("<br>", "").replace("&nbsp;", " ");
            Utils.writeText(text, chapter.getSavePath());
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private String calcChapterSavePath(Chapter chapter, String bookSavePath) {
        return bookSavePath + "/" + chapter.getId().replace(".html", ".txt");
    }

    private String calcPrefix(String para) {
        return para.length() > 3 ? para.substring(0, para.length() - 3) : "0";
    }

    private String calcBookUrl(String para) {
        String prefix = calcPrefix(para);
        return String.format("http://www.piaotian.net/html/%s/%s", prefix, para);
    }

    private String calcCoverUrl(String para) {
        String prefix = calcPrefix(para);
        return String.format("http://www.piaotian.net/files/article/image/%s/%s/%ss.jpg", prefix, para, para);
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
