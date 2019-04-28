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

import sixue.naivereader.SmartDownloader;
import sixue.naivereader.Utils;
import sixue.naivereader.data.Book;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.data.Source;

public class WlzwProvider extends NetProvider {
    @Override
    public String getProviderId() {
        return "wwww.50zw.la";
    }

    @Override
    public String getProviderName() {
        return "武林中文";
    }

    @Override
    public List<Book> search(String s, Context context) {
        List<Book> list = new ArrayList<>();
        try {
            String key = URLEncoder.encode(s, "GB2312");
            String url = "https://www.50zw.la/modules/article/search.php?searchkey=" + key;
            Connection.Response response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute();
            if (!url.equals(response.url().toString())) {
                Document doc = response.parse();

                Elements elements = doc.body().select(".book_info");
                Document subDoc = Jsoup.parse(elements.toString());

                String author = "*";
                for (Element span : subDoc.select("#info > .options > .item")) {
                    String t = span.text();
                    Log.i(getClass().toString(), "span.text=" + t);

                    final String prefix = "作者：";
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
    public List<Chapter> downloadContent(Book book, String bookSavePath) {

        List<Chapter> content = new ArrayList<>();
        try {
            String contentUrl = "https://www.50zw.la/book_" + book.getSitePara() + "/";
            Document doc = Jsoup.connect(contentUrl).timeout(5000).get();
            Elements elements = doc.body().select(".chapterlist");
            for (Element ch : Jsoup.parse(elements.toString()).select("li:not(.volume)")) {
                String title = ch.select("a").text();
                String url = ch.select("a").attr("href").replace("/", "").trim();
                if (url.length() == 0) {
                    continue;
                }

                final Chapter chapter = new Chapter();
                chapter.setId(url);
                chapter.setTitle(title);
                chapter.setPara("");
                String chapterSavePath = calcChapterSavePath(chapter, bookSavePath);
                chapter.setSavePath(chapterSavePath);

                content.add(chapter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    @Override
    public void downloadChapter(Book book, Chapter chapter) {

        try {
            Document doc = Jsoup.connect(getChapterUrl(book, chapter)).timeout(5000).get();
            String text = doc.body().select("#htmlContent").html().replace("<br>", "").replace("&nbsp;", " ");
            Utils.writeText(text, chapter.getSavePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getChapterUrl(Book book, Chapter chapter) {
        return "https://www.50zw.la/book_" + book.getSitePara() + "/" + chapter.getId();
    }

    private String calcChapterSavePath(Chapter chapter, String bookSavePath) {
        return bookSavePath + "/" + chapter.getId().replace(".html", ".txt");
    }

    private String calcCoverUrl(String para) {
        String prefix = para.length() > 3 ? para.substring(0, para.length() - 3) : "0";
        return String.format("https://www.50zw.la/files/article/image/%s/%s/%ss.jpg", prefix, para, para);
    }

    private String parseBookUrl(String bookUrl) {
        int l = bookUrl.lastIndexOf('_');
        int r = bookUrl.length();
        if (l != -1 && r != -1 && l < r) {
            return bookUrl.substring(l + 1, r - 1).replace("/", "");
        } else {
            return "";
        }
    }
}
