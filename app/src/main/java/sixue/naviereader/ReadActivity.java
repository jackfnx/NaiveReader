package sixue.naviereader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class ReadActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {

    private static final String TAG = "ReadActivity";
    private GestureDetector detector;
    private ReaderView readerView;
    private TextView progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        Utils.verifyStoragePermissions(this);
        String text = Utils.readText("test.txt");
        if (text == null) {
            text = "Can't open file.";
        }

        readerView = (ReaderView) findViewById(R.id.textArea);
        readerView.importText(text);

        readerView.setOnTouchListener(this);

        detector = new GestureDetector(this, this);
        detector.setIsLongpressEnabled(true);

        readerView.setOnPageChangeListener(new ReaderView.OnPageChangeListener() {
            @Override
            public void onPageChanged(ReaderView v) {
                int textLength = readerView.getTextLength();
                int charCount = readerView.getCharCount();
                int startChart = readerView.getStartChar();
                if (textLength <= 0) {
                    progress.setText("0");
                    return;
                }

                if (charCount <= 0) {
                    progress.setText("?");
                    return;
                }

                progress.setText(String.format(Locale.CHINA, "%d/%d", startChart / charCount + 1, textLength / charCount + 1));
            }
        });

        TextView title = (TextView) findViewById(R.id.title);
        title.setText(readerView.getTextTitle());

        progress = (TextView) findViewById(R.id.progress);
        progress.setText(readerView.generateProgress());
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        Log.i(TAG, "onDown");
        readerView.turnPage(1);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
        Log.i(TAG, "onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        Log.i(TAG, "onSingleTapUp");
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        Log.i(TAG, "onScroll");
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        Log.i(TAG, "onLongPress");
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        Log.i(TAG, "onFling");
        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.i(TAG, "onTouch");
        return detector.onTouchEvent(motionEvent);
    }
}
