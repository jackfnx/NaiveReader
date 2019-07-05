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
import java.util.Date;
import java.util.List;

import sixue.naivereader.Utils;
import sixue.naivereader.data.Book;
import sixue.naivereader.data.BookKind;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.data.Source;
import sixue.naivereader.helper.OnlineHelper;

public class QbxsProvider extends NetProvider {
    private static final String TAG = QbxsProvider.class.getSimpleName();

    @Override
    public String getProviderId() {
        return "www.quanben.io";
    }

    @Override
    public String getProviderName() {
        return "全本小说";
    }

    @Override
    public List<Book> search(String s, Context context) {
        List<Book> list = new ArrayList<>();
        try {
            String key = URLEncoder.encode(s, "UTF-8");
            String url = "http://www.quanben.io/index.php?c=book&a=search&keywords=" + key;
            Connection.Response response = Jsoup.connect(url).followRedirects(true).timeout(5000).execute();
            if (!url.equals(response.url().toString())) {
                Log.d(TAG, response.url().toString());
                // cant reach.
            } else {
                Document doc = response.parse();

                for (Element item : doc.body().select(".list2")) {
                    Element titleEle = item.select("h3 a[itemprop=url]").first();
                    Element authorEle = item.select("p span[itemprop=author]").first();
                    Element coverEle = item.select("img").first();

                    String title = titleEle.text();
                    String id = titleEle.text();
                    String author = authorEle.text();
                    String para = titleEle.attr("href");
                    String coverUrl = coverEle.attr("src");

                    Book book = new Book();
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
        String para = book.getSitePara();
        String contentUrl = String.format("http://www.quanben.io%slist.html", para);
        List<Chapter> content = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(contentUrl).timeout(5000).get();
            Elements elements = doc.body().select(".list3");
            for (Element ch : Jsoup.parse(elements.toString()).select("li[itemprop=itemListElement]")) {
                String title = ch.select("a").text();
                String url = ch.select("a").attr("href").trim();
                if (url.length() == 0) {
                    continue;
                }

                String id = url.substring(url.lastIndexOf("/"));

                final Chapter chapter = new Chapter();
                chapter.setId(url);
                chapter.setTitle(title);
                String chapterSavePath = calcChapterSavePath(chapter, bookSavePath);
                chapter.setSavePath(chapterSavePath);

                content.add(chapter);
            }
        } catch (IOException e) {
            Log.e(TAG, "downloadContent ERROR: " + contentUrl);
        }
        return content;
    }

    @Override
    public void downloadChapter(Book book, Chapter chapter) {

        String chapterUrl = getChapterUrl(book, chapter);
        try {
            String myJs = null;
            Document doc = Jsoup.connect(chapterUrl).timeout(5000).get();
            for (Element jsEle : doc.body().select("script[type=text/javascript]")) {
                String js = jsEle.html();
                if (js.startsWith("setTimeout(\"ajax_post(")) {
                    myJs = js;
                    break;
                }
            }
            if (myJs == null) {
                return;
            }

            myJs = myJs.substring("setTimeout(\"ajax_post(".length());
            myJs = myJs.substring(0, myJs.indexOf("\""));

            String[] paras = myJs.replace("'", "").split(",");
            if (paras.length < 10) {
                return;
            }

            String ajaxUrl = String.format("http://www.quanben.io/index.php?c=%s&a=%s", paras[0], paras[1]);
            String tm = "" + new Date().getTime();
            Document doc2 = Jsoup.connect(ajaxUrl)
                    .referrer(chapterUrl)
                    .data(paras[2], paras[3]) // pinyin, duorushenyuan
                    .data(paras[4], paras[5]) // id, 1
                    .data(paras[6], paras[7]) // "sky", "bce7cae095b38a070b33746694953743"
                    .data(paras[8], paras[9]) // "t", "1562330872"
                    .data("_type", "ajax")
                    .data("rndval", tm)
                    .timeout(5000).post();
            String text = doc2.html();
            String plainText = Utils.clearHtmlTag(text, new String[] {})
                    .replace("<p>", "").replace("</p>", "\n");
            Utils.writeText(plainText, chapter.getSavePath());
        } catch (IOException e) {
            Log.e(TAG, "downloadChapter ERROR: " + chapterUrl);
        }
    }

    @Override
    public String getChapterUrl(Book book, Chapter chapter) {
        return "http://www.quanben.io" + chapter.getId();
    }

    private String calcChapterSavePath(Chapter chapter, String bookSavePath) {
        return bookSavePath + "/" + chapter.getId().replace("/", "_").replace(".html", ".txt");
    }
}
