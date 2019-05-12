package sixue.naivereader;

import android.content.Context;
import android.content.Intent;

import java.io.File;

import sixue.naivereader.data.Book;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.provider.NetProvider;
import sixue.naivereader.provider.NetProviderCollections;

class SmartDownloader {
    private final Book book;
    private final Context context;

    SmartDownloader(Context context, Book book) {
        this.context = context;
        this.book = book;
    }

    boolean isDownloaded(Chapter chapter) {
        File file = new File(chapter.getSavePath());
        return file.exists();
    }

    private void downloadChapter(Chapter chapter) {
        NetProvider provider = NetProviderCollections.findProviders(book.getSiteId());
        provider.downloadChapter(book, chapter);

        Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
        intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.getId());
        intent.putExtra(Utils.INTENT_PARA_PATH, chapter.getSavePath());
        context.sendBroadcast(intent);
    }

    void startDownloadContent() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                book.buildHelper().downloadContent(context);
            }
        }).start();
    }

    void startDownloadChapter(final Chapter chapter) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadChapter(chapter);
            }
        }).start();
    }

    String getChapterUrl(Chapter chapter) {
        NetProvider provider = NetProviderCollections.findProviders(book.getSiteId());
        return provider.getChapterUrl(book, chapter);
    }

    void startDownloadAllChapter() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadAllChapter();
            }
        }).start();
    }

    private void downloadAllChapter() {
        for (Chapter chapter : book.getChapterList()) {
            if (!isDownloaded(chapter)) {
                downloadChapter(chapter);
            }
        }

        Intent intent = new Intent(Utils.ACTION_DOWNLOAD_ALL_CHAPTER_FINISH);
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
        context.sendBroadcast(intent);
    }
}
