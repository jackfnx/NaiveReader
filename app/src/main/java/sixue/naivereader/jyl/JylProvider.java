package sixue.naivereader.jyl;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sixue.naivereader.Utils;

public class JylProvider {

    private static List<JylAuthor> authors = new ArrayList<>();
    private static Map<JylAuthor, List<JylBook>> books = new HashMap<>();
    private Context context;

    public JylProvider(Context context) {
        this.context = context;
    }

    public static List<JylAuthor> getAuthors() {
        return authors;
    }

    public static List<JylBook> getBooks(JylAuthor author) {
        return books.get(author);
    }

    private void downloadAuthors() {
        authors.clear();
        try {
            String url = "http://wx.shushu.com.cn/wuxia/xssc.html";
            Connection.Response response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute();

            Document doc = response.parse();

            Elements elements = doc.body().select("a");
            for (Element element : elements) {
                JylAuthor author = new JylAuthor();
                author.setAuthor(element.text().replace(" ", ""));
                author.setUrl(element.absUrl("href"));
                authors.add(author);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadBooks(JylAuthor author) {
        try {
            Connection.Response response = Jsoup.connect(author.getUrl()).followRedirects(true).timeout(5000).execute();
            Document doc = response.parse();

            String series = null;
            Elements items = doc.body().select("td");
            List<JylBook> myBooks = new ArrayList<>();
            for (Element item : items) {
                if (item.hasAttr("rowspan")) {
                    series = item.text().replace(" ", "");
                } else {
                    JylBook book = new JylBook();
                    book.setAuthor(author.getAuthor());
                    book.setSeries(series);
                    book.setTitle(item.text().replace(" ", ""));
                    Elements as = item.select("a");
                    if (as.size() != 0) {
                        book.setUrl(as.first().absUrl("href"));
                    }
                    myBooks.add(book);
                }
            }
            books.put(author, myBooks);
            Log.i("JYL", "subDoc");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startDownloadAuthors() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadAuthors();
                Intent intent = new Intent();
                intent.setAction(Utils.ACTION_JYL_AUTHORS_DOWNLOAD_FINISH);
                context.sendBroadcast(intent);
            }
        }).start();
    }

    public void startDownloadBooks(final JylAuthor author) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadBooks(author);
                Intent intent = new Intent();
                intent.setAction(Utils.ACTION_JYL_AUTHOR_BOOKS_DOWNLOAD_FINISH);
                context.sendBroadcast(intent);
            }
        }).start();
    }
}
