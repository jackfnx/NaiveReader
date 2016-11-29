package sixue.naviereader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

public class ReadActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {

    private GestureDetector detector;
    private ReaderView readerView;
    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        String path = getIntent().getStringExtra(Utils.INTENT_PARA_PATH);
        int currentPosition = getIntent().getIntExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0);

        //Utils.verifyStoragePermissions(this);
        //path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.txt";
        String text = Utils.readText(path);
        if (text == null) {
            text = "Can't open file.";
        }

        readerView = (ReaderView) findViewById(R.id.textArea);
        ImageView maskView = (ImageView) findViewById(R.id.pageMask);
        View loading = findViewById(R.id.loadingMask);

        readerView.importText(text, currentPosition);
        readerView.setOnTouchListener(this);

        readerView.setPageMask(maskView);
        readerView.setLoadingMask(loading);

        readerView.startTypesetThread();

        detector = new GestureDetector(this, this);
        detector.setIsLongpressEnabled(true);

        final TextView progress = (TextView) findViewById(R.id.progress);
        readerView.setOnPageChangeListener(new ReaderView.OnPageChangeListener() {
            @Override
            public void onPageChanged(ReaderView v) {
                String maxPages = readerView.getMaxPages();
                int currentPage = readerView.getCurrentPage();
                progress.setText(String.format(Locale.CHINA, "%d/%s", currentPage + 1, maxPages));
                //BookLoader.getInstance().updateBookPosition(book, readerView.getCurrentPosition(), ReadActivity.this);
            }
        });

        final TextView battery = (TextView) findViewById(R.id.battery);
        battery.setText("?");
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
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(batteryReceiver);
        readerView.stopTypesetThread();
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
