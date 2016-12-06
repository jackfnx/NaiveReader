package sixue.naviereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import sixue.naviereader.data.Book;
import sixue.naviereader.data.Chapter;

public class ReadActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {

    private static final Chapter emptyChapter = new Chapter();
    private GestureDetector detector;
    private ReaderView readerView;
    private BroadcastReceiver batteryReceiver;
    private BroadcastReceiver receiver;
    private Book book;
    private Chapter chapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        book = BookLoader.getInstance().getBook(0);
        chapter = emptyChapter;

        readerView = (ReaderView) findViewById(R.id.text_area);
        ImageView maskView = (ImageView) findViewById(R.id.page_mask);
        View loading = findViewById(R.id.loading_mask);
        final TextView title = (TextView) findViewById(R.id.title);
        final TextView subtitle = (TextView) findViewById(R.id.subtitle);
        final TextView progress = (TextView) findViewById(R.id.progress);
        final TextView battery = (TextView) findViewById(R.id.battery);

        detector = new GestureDetector(this, this);
        detector.setIsLongpressEnabled(true);

        readerView.setOnTouchListener(this);
        readerView.setPageMask(maskView);
        readerView.setLoadingMask(loading);

        readerView.setOnPageChangeListener(new ReaderView.OnPageChangeListener() {
            @Override
            public void onPageChanged(ReaderView v) {
                String maxPages = readerView.getMaxPages();
                int currentPage = readerView.getCurrentPage();
                progress.setText(String.format(Locale.CHINA, "%d/%s", currentPage + 1, maxPages));
                book.setCurrentPosition(readerView.getCurrentPosition());
                BookLoader.getInstance().updateBook(book);
            }
        });
        readerView.setOnTurnPageOverListener(new ReaderView.OnTurnPageOverListener() {

            @Override
            public void onTurnPageOver(int step) {
                if (!book.isLocal()) {
                    int i = book.getCurrentChapterIndex() + (step > 0 ? 1 : -1);
                    if (i >= 0 && i < book.getChapterList().size()) {
                        loadNetChapter(i);
                        return;
                    }
                }
                if (step < 0) {
                    Toast.makeText(ReadActivity.this, R.string.msg_first_page, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ReadActivity.this, R.string.msg_last_page, Toast.LENGTH_SHORT).show();
                }
            }
        });

        title.setText("?");
        subtitle.setText("?");
        IntentFilter myFilter = new IntentFilter(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if (book.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID)) &&
                        chapter.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_CHAPTER_ID))) {

                    title.setText(book.getTitle());
                    subtitle.setText(chapter.getTitle());
                    String path = intent.getStringExtra(Utils.INTENT_PARA_PATH);
                    String text = Utils.readText(path);
                    if (text == null) {
                        text = "Can't open file.";
                    }

                    readerView.importText(text, book.getCurrentPosition());
                }
            }
        };
        registerReceiver(receiver, myFilter);

        battery.setText("?");
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                    int level = intent.getIntExtra("level", 0);
                    int scale = intent.getIntExtra("scale", 100);
                    battery.setText(String.format(Locale.CHINA, "%d%%", level * 100 / scale));
                }
            }
        };
        registerReceiver(batteryReceiver, filter);

        int newIndex = getIntent().getIntExtra(Utils.INTENT_PARA_CHAPTER_INDEX, 0);
        if (book.isLocal()) {
            loadLocalChapter();
        } else {
            loadNetChapter(newIndex);
        }
    }

    private void loadLocalChapter() {
        Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
        intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, "");
        intent.putExtra(Utils.INTENT_PARA_PATH, book.getLocalPath());
        intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, book.getCurrentPosition());
        sendBroadcast(intent);
    }

    private void loadNetChapter(int newIndex) {
        if (newIndex >= 0 && newIndex < book.getChapterList().size()) {
            if (book.getCurrentChapterIndex() != newIndex) {
                book.setCurrentChapterIndex(newIndex);
                book.setCurrentPosition(0);
            }
        }

        chapter = book.getChapterList().get(book.getCurrentChapterIndex());
        SmartDownloader smartDownloader = new SmartDownloader(this, book);
        if (smartDownloader.isDownloaded(chapter)) {
            Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
            intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
            intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.getId());
            intent.putExtra(Utils.INTENT_PARA_PATH, chapter.getSavePath());
            intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, book.getCurrentPosition());
            sendBroadcast(intent);
        } else {
            smartDownloader.startDownloadChapter(chapter);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        unregisterReceiver(batteryReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        Log.i(getClass().toString(), "onDown");
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
        Log.i(getClass().toString(), "onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        Log.i(getClass().toString(), "onSingleTapUp");

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        float x = motionEvent.getRawX();
        float y = motionEvent.getRawY();
        Log.d(getClass().toString(), "Display:width=" + widthPixels + ",height=" + heightPixels + "; Touch:x=" + x + ",y=" + y);

        if ((x < (widthPixels / 2)) && (y < (heightPixels / 2))) {
            readerView.turnPage(-1);
        } else {
            readerView.turnPage(1);
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float vX, float vY) {
        Log.i(getClass().toString(), "onScroll");
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        Log.i(getClass().toString(), "onLongPress");
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float vX, float vY) {
        Log.i(getClass().toString(), "onFling");
        Log.d(getClass().toString(), "Fling:vX=" + vX + ",vY=" + vY);
        if (vX > 0) {
            readerView.turnPage(-1);
        } else {
            readerView.turnPage(1);
        }
        return true;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.i(getClass().toString(), "onTouch");
        return detector.onTouchEvent(motionEvent);
    }
}
