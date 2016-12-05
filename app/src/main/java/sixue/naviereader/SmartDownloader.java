package sixue.naviereader;

import android.content.Context;
import android.content.Intent;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sixue.naviereader.data.Book;
import sixue.naviereader.data.Chapter;

public class SmartDownloader {
    private final Book book;
    private final Context context;

    public SmartDownloader(Context context, Book book) {
        this.context = context;
        this.book = book;
    }

    public boolean reloadContent() {
        if (book.isLocal()) {
            return true;
        }

        String bookSavePath = calcBookSavePath(book);
        String json = Utils.readText(bookSavePath + "/.CONTENT");
        if (json == null) {
            return false;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JavaType listType = mapper.getTypeFactory().constructParametricType(ArrayList.class, Chapter.class);
            List<Chapter> list = mapper.readValue(json, listType);
            book.setChapterList(list);
            return list.size() > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void downloadContent() {
        book.getChapterList().clear();

        String bookSavePath = calcBookSavePath(book);

        try {
            Document doc = Jsoup.connect(book.getId()).timeout(5000).get();
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

                book.getChapterList().add(chapter);
            }

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(book.getChapterList());
            Utils.writeText(json, bookSavePath + "/.CONTENT");

            Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
            intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
            context.sendBroadcast(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isDownloaded(Chapter chapter) {
        File file = new File(chapter.getSavePath());
        return file.exists();
    }

    public void downloadChapter(Chapter chapter) {
        try {
            Document doc = Jsoup.connect(book.getId() + "/" + chapter.getId()).timeout(5000).get();
            String text = doc.body().select("#htmlContent").html().replace("<br>", "").replace("&nbsp;", " ");
            Utils.writeText(text, chapter.getSavePath());

            Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
            intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
            intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.getId());
            intent.putExtra(Utils.INTENT_PARA_PATH, chapter.getSavePath());
            context.sendBroadcast(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String calcBookSavePath(Book book) {
        String s = book.getId().replace("http://", "").replace("/", "_");
        File fileDir = context.getExternalFilesDir("books");
        if (fileDir == null) {
            return "";
        }

        return fileDir.getAbsolutePath() + "/" + book.getTitle() + "/" + s;
    }

    private String calcChapterSavePath(Chapter chapter, String bookSavePath) {
        return bookSavePath + "/" + chapter.getId().replace(".html", ".txt");
    }

    public void startDownloadContent() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadContent();
            }
        }).start();
    }

    public void startDownloadChapter(final Chapter chapter) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadChapter(chapter);
            }
        }).start();
    }
}
