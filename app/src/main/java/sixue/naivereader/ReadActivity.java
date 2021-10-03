package sixue.naivereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import sixue.naivereader.data.Book;
import sixue.naivereader.data.BookKind;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.helper.LocalTextLoader;

public class ReadActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {

    private static final Chapter emptyChapter = new Chapter();
    private GestureDetector detector;
    private ReaderView readerView;
    private BroadcastReceiver batteryReceiver;
    private BroadcastReceiver receiver;
    private Book book;
    private Chapter chapter;
    private SmartDownloader smartDownloader;
    private List<Integer> localChapterNodes;
    private String text;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        actionBar = getSupportActionBar();

        book = BookLoader.getInstance().getBook(0);
        chapter = emptyChapter;
        text = "";
        localChapterNodes = new ArrayList<>();

        readerView = findViewById(R.id.text_area);
        ImageView maskView = findViewById(R.id.page_mask);
        View loading = findViewById(R.id.loading_mask);
        final TextView title = findViewById(R.id.title);
        final TextView subtitle = findViewById(R.id.subtitle);
        final TextView progress = findViewById(R.id.progress);
        final TextView battery = findViewById(R.id.battery);

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
                if (book.getKind() == BookKind.LocalText && localChapterNodes.size() > 0) {
                    for (int i = 0; i < localChapterNodes.size(); i++) {
                        int node = localChapterNodes.get(i);
                        int next = (i + 1) < localChapterNodes.size() ? localChapterNodes.get(i + 1) : Integer.MAX_VALUE;
                        if (book.getCurrentPosition() >= node && book.getCurrentPosition() < next) {
                            int end = text.indexOf('\n', node);
                            end = end < 0 ? text.length() : end;
                            String s = text.substring(node, end);
                            subtitle.setText(s);
                            break;
                        }
                    }
                }
                BookLoader.getInstance().save();
            }
        });
        readerView.setOnTurnPageOverListener(new ReaderView.OnTurnPageOverListener() {

            @Override
            public void onTurnPageOver(int step) {
                if (book.getKind() == BookKind.Online) {
                    int i = book.getCurrentChapterIndex() + (step > 0 ? 1 : -1);
                    if (i >= 0 && i < book.getChapterList().size()) {
                        loadNetChapter(i, step > 0 ? 0 : Integer.MAX_VALUE);
                        return;
                    }
                } else if (book.getKind() == BookKind.Packet) {
                    int i = book.getCurrentChapterIndex() + (step > 0 ? -1 : 1);
                    if (i >= 0 && i < book.getChapterList().size()) {
                        loadPackChapter(i, step > 0 ? 0 : Integer.MAX_VALUE);
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

        final int newIndex = getIntent().getIntExtra(Utils.INTENT_PARA_CHAPTER_INDEX, -1);
        final int newPosition = getIntent().getIntExtra(Utils.INTENT_PARA_CURRENT_POSITION, -1);

        if (newPosition >= 0) {
            book.setCurrentPosition(newPosition);
        }

        title.setText("?");
        subtitle.setText("?");
        IntentFilter myFilter = new IntentFilter();
        myFilter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
        myFilter.addAction(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                if (action == null) {
                    return;
                }

                switch (action) {
                    case Utils.ACTION_DOWNLOAD_CONTENT_FINISH:
                        if (book.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID))) {
                            loadNetChapter(newIndex, 0);
                        }
                        break;
                    case Utils.ACTION_DOWNLOAD_CHAPTER_FINISH:
                        if (book.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID)) &&
                                chapter.getId().equals(intent.getStringExtra(Utils.INTENT_PARA_CHAPTER_ID))) {

                            title.setText(book.getTitle());
                            subtitle.setText(chapter.getTitle());
                            if (book.getKind() == BookKind.Packet) {
                                text = Utils.readTextFromZip(book.getLocalPath(), chapter.getSavePath());
                            } else if (book.getKind() == BookKind.Online) {
                                text = Utils.readText(chapter.getSavePath());
                            } else if (book.getKind() == BookKind.LocalText) {
                                text = Utils.readExternalText(context, book.getLocalPath());
                            }
                            if (text == null) {
                                text = "Can't open file.";
                            } else {
                                if (book.getKind() == BookKind.LocalText) {
                                    localChapterNodes = LocalTextLoader.calcChapterNodes(text);
                                    book.setWordCount(text.length());
                                    BookLoader.getInstance().save();
                                }
                            }

                            readerView.importText(text, book.getCurrentPosition());
                        }
                        break;
                    default:
                        break;
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

        smartDownloader = new SmartDownloader(this, book);
        if (book.buildHelper().reloadContent(this)) {
            if (book.getKind() == BookKind.LocalText) {
                Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
                intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
                intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, "");
                intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, book.getCurrentPosition());
                sendBroadcast(intent);
            } else if (book.getKind() == BookKind.Packet) {
                loadPackChapter(newIndex, newPosition);
            } else {
                Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
                intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
                sendBroadcast(intent);
            }
        } else {
            readerView.setLoading(true, false);
            smartDownloader.startDownloadContent();
        }
    }

    private void loadPackChapter(int newIndex, int newPosition) {
        if (newIndex >= 0 && newIndex < book.getChapterList().size()) {
            if (book.getCurrentChapterIndex() != newIndex) {
                book.setCurrentChapterIndex(newIndex);
                book.setCurrentPosition(newPosition);
            }
        }

        if (book.getChapterList().size() == 0) {
            return;
        }

        chapter = book.getChapterList().get(book.getCurrentChapterIndex());

        Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
        intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.getId());
        intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, book.getCurrentPosition());
        sendBroadcast(intent);
    }

    private void loadNetChapter(int newIndex, int newPosition) {
        readerView.setLoading(true, false);

        if (newIndex >= 0 && newIndex < book.getChapterList().size()) {
            if (book.getCurrentChapterIndex() != newIndex) {
                book.setCurrentChapterIndex(newIndex);
                book.setCurrentPosition(newPosition);
            }
        }

        if (book.getChapterList().size() == 0) {
            return;
        }

        chapter = book.getChapterList().get(book.getCurrentChapterIndex());

        if (smartDownloader.isDownloaded(chapter)) {
            Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
            intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
            intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.getId());
            intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, book.getCurrentPosition());
            sendBroadcast(intent);
        } else {
            smartDownloader.startDownloadChapter(chapter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        actionBar.setDisplayShowTitleEnabled(false);
        int color = ContextCompat.getColor(this, R.color.colorPrimary);
        int transparentColor = Color.argb(0x99, Color.red(color), Color.green(color), Color.blue(color));
        actionBar.setBackgroundDrawable(new ColorDrawable(transparentColor));
        actionBar.hide();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.read, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.refresh).setEnabled(book.getKind() == BookKind.Online);
        menu.findItem(R.id.browse_it).setEnabled(book.getKind() == BookKind.Online);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.content: {
                Intent intent = new Intent(this, ContentActivity.class);
                startActivity(intent);
                actionBar.hide();
                return true;
            }
            case R.id.refresh: {
                if (book.getKind() == BookKind.Online) {
                    Utils.deleteFile(chapter.getSavePath());
                    loadNetChapter(book.getCurrentChapterIndex(), 0);
                    return true;
                }
            }
            case R.id.browse_it: {
                if (book.getKind() == BookKind.Online) {
                    String url = smartDownloader.getChapterUrl(chapter);
                    Intent intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    actionBar.hide();
                    return true;
                }
            }
            default:
                break;
        }
        return onOptionsItemSelected(menuItem);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        unregisterReceiver(batteryReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        //Log.i(getClass().toString(), "onDown");
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
        //Log.i(getClass().toString(), "onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        //Log.i(getClass().toString(), "onSingleTapUp");
        if (actionBar.isShowing()) {
            actionBar.hide();
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int widthPixels = metrics.widthPixels;
            int heightPixels = metrics.heightPixels;
            float x = motionEvent.getRawX();
            float y = motionEvent.getRawY();
            Log.d(getClass().toString(), "Display:width=" + widthPixels + ",height=" + heightPixels + "; Touch:x=" + x + ",y=" + y);

            if ((x < (widthPixels / 3)) && (y < (heightPixels / 2))) {
                readerView.turnPage(-1);
            } else if ((x > (widthPixels / 3) && (x < widthPixels * 2 / 3)) && (y < (heightPixels / 2))) {
                actionBar.show();
            } else {
                readerView.turnPage(1);
            }
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float vX, float vY) {
        //Log.i(getClass().toString(), "onScroll");
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        //Log.i(getClass().toString(), "onLongPress");
        actionBar.show();
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float vX, float vY) {
        //Log.i(getClass().toString(), "onFling");
        Log.d(getClass().toString(), "Fling:vX=" + vX + ",vY=" + vY);
        if (vY < 2000 && vY > -2000) {
            if (vX > 0) {
                actionBar.hide();
                readerView.turnPage(-1);
            } else {
                actionBar.hide();
                readerView.turnPage(1);
            }
//        } else {
//            if (vX < 2000 && vX > -2000) {
//                if (vY < 0) {
//                    Intent intent = new Intent(this, ContentActivity.class);
//                    startActivity(intent);
//                }
//            }
        }
        return true;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //Log.i(getClass().toString(), "onTouch");
        readerView.isGesture = detector.onTouchEvent(motionEvent);
        return readerView.performClick();
    }
}
