package sixue.naviereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import sixue.naviereader.data.Book;
import sixue.naviereader.data.Chapter;

public class LoadingActivity extends AppCompatActivity {

    private Book loadingBook;
    private BroadcastReceiver receiver;
    private String nextAction;
    private Chapter loadingChapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        nextAction = getIntent().getStringExtra(Utils.INTENT_PARA_NEXT_ACTION);

        if (nextAction.equals(Utils.INTENT_PARA_NEXT_ACTION_CONTENT)) {
            loadingBook = BookLoader.getInstance().getBook(0);
            SmartDownloader downloader = new SmartDownloader(this, loadingBook);
            if (downloader.reloadContent()) {
                if (loadingBook.isLocal()) {
                    Intent intent = new Intent(this, ReadActivity.class);
                    intent.putExtra(Utils.INTENT_PARA_BOOK_ID, loadingBook.getId());
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(this, ContentActivity.class);
                    intent.putExtra(Utils.INTENT_PARA_BOOK_ID, loadingBook.getId());
                    startActivity(intent);
                    finish();
                }
            } else {
                downloader.downloadContent();
            }

        } else if (nextAction.equals(Utils.INTENT_PARA_NEXT_ACTION_READ)) {

        }
/*
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_CHAPTER_CHANGED);
        filter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
        filter.addAction(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (nextAction != null && nextAction.equals(Utils.INTENT_PARA_NEXT_ACTION_READ)) {
                    if (loadingBook != null && loadingBook.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID))) {
                        loadingChapter = loadingBook.getChapterList().get(0);
                        if (loadingChapter.isDownloaded()) {
                            Intent i2 = new Intent(LoadingActivity.this, ReadActivity.class);
                            i2.putExtra(Utils.INTENT_PARA_PATH, loadingChapter.getSavePath());
                            i2.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);
                            startActivity(i2);
                            finish();
                        } else {
                            BookLoader.getInstance().pushChapterQueue(loadingChapter);
                        }
                    } else if (loadingChapter != null && loadingChapter.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_CHAPTER_ID))) {
                        Intent i2 = new Intent(LoadingActivity.this, ReadActivity.class);
                        i2.putExtra(Utils.INTENT_PARA_PATH, loadingChapter.getSavePath());
                        i2.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);
                        startActivity(i2);
                        finish();
                    }
                }
            }
        };
        registerReceiver(receiver, filter);*/

        if (loadingBook.isLocal()) {
            Intent i = new Intent(LoadingActivity.this, ReadActivity.class);
            i.putExtra(Utils.INTENT_PARA_PATH, loadingBook.getLocalPath());
            i.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);
            startActivity(i);
            finish();
        } else {
            if (loadingBook.getChapterList().size() != 0) {
                int num = loadingBook.getChapterList().size();
                int i = loadingBook.getCurrentChapterIndex();
                if (i < 0 || i >= num) {
                    i = 0;
                }
                Chapter currentChapter = loadingBook.getChapterList().get(i);
                String currentChapterId = loadingBook.getCurrentChapterId();
                if (!currentChapterId.equals(currentChapter.getId())) {
                    boolean found = false;
                    for (Chapter ch : loadingBook.getChapterList()) {
                        if (currentChapterId.equals(ch.getId())) {
                            currentChapter = ch;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        currentChapter = loadingBook.getChapterList().get(0);
                    }
                }
                if (currentChapter.isDownloaded()) {
                    Intent i2 = new Intent(LoadingActivity.this, ReadActivity.class);
                    i2.putExtra(Utils.INTENT_PARA_PATH, currentChapter.getSavePath());
                    i2.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);
                    startActivity(i2);
                    finish();
                } else {
                    loadingChapter = currentChapter;
                    //BookLoader.getInstance().pushChapterQueue(currentChapter);
                }
            } else {
                BookLoader.getInstance().pushContentQueue(loadingBook);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
