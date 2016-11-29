package sixue.naviereader;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;

import sixue.naviereader.data.Book;
import sixue.naviereader.data.BookLoader;
import sixue.naviereader.data.Chapter;

public class DownloadService extends Service {
    private boolean running;
    private Thread contentThread;
    private Thread chapterThread;

    public DownloadService() {
    }

    @Override
    public void onCreate() {
        contentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (running) {
                        final Book book = BookLoader.getInstance().popContentQueue();
                        if (book == null) {
                            Thread.sleep(1000);
                            continue;
                        }

                        String bookSavePath = calcBookSavePath(book);

                        Document doc = Jsoup.connect(book.getId()).timeout(5000).get();
                        Elements list = doc.body().select(".chapterlist");
                        for (Element ch : Jsoup.parse(list.toString()).select("li:not(.volume)")) {
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
                            File file = new File(chapterSavePath);
                            chapter.setSavePath(chapterSavePath);
                            chapter.setDownloaded(file.exists());

                            Intent intent = new Intent();
                            sendBroadcast(intent);
                        }

                        Intent intent = new Intent();
                        sendBroadcast(intent);
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        });

        chapterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (running) {
                        final Chapter chapter = BookLoader.getInstance().popChapterQueue();
                        if (chapter == null) {
                            Thread.sleep(1000);
                            continue;
                        }

                        Document doc = Jsoup.connect(chapter.getBookId() + "/" + chapter.getId()).timeout(5000).get();
                        String text = doc.body().select("#htmlContent").text();
                        Utils.writeText(text, chapter.getSavePath());

                        Intent intent = new Intent();
                        sendBroadcast(intent);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        contentThread.start();
        chapterThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String calcBookSavePath(Book book) {
        String s = book.getId().replace("http://", "").replace("/", "_");
        File fileDir = this.getExternalFilesDir("books");
        if (fileDir == null) {
            return "";
        }

        return fileDir.getAbsolutePath() + "/" + s;
    }

    private String calcChapterSavePath(Chapter chapter, String bookSavePath) {
        return bookSavePath + "/" + chapter.getId().replace(".html", ".txt");
    }

}
