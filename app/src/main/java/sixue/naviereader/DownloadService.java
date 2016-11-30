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
                        Book book = BookLoader.getInstance().popContentQueue();
                        if (book == null) {
                            Thread.sleep(1000);
                            continue;
                        }

                        SmartDownloader downloader = new SmartDownloader(DownloadService.this, book);
                        downloader.downloadContent();

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        chapterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (running) {
                        BookLoader.ChapterTask task = BookLoader.getInstance().popChapterQueue();
                        if (task == null) {
                            Thread.sleep(1000);
                            continue;
                        }

                        SmartDownloader downloader = new SmartDownloader(DownloadService.this, task.book);
                        downloader.downloadChapter(task.chapter);
                    }
                } catch (InterruptedException e) {
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
}
