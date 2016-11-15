package sixue.naviereader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public class ReaderView extends View {
    private TextPaint textPaint;
    private float fontTop;
    private float fontHeight;
    private int startChar = 0;
    private int endChar = 0;
    private String text;
    public static final int MAX_LINE_LENGTH = 80;
    private OnPageChangeListener onPageChangeListener;

    private void initTextPaint() {
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setARGB(0xFF, 0, 0, 0);
        textPaint.setTextSize(50.0f);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        fontTop = Math.abs(fm.top);
        fontHeight = Math.abs(fm.ascent) + Math.abs(fm.descent) + Math.abs(fm.leading);
    }

    public ReaderView(Context context) {
        super(context);
        initTextPaint();
    }

    public ReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTextPaint();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float maxWidth = canvas.getWidth();
        float maxHeight = canvas.getHeight();
        int i = startChar;
        for (float y = fontTop; y < maxHeight; y += fontHeight) {
            int len = textPaint.breakText(text, i, i + MAX_LINE_LENGTH, true, maxWidth, null);
            if (len <= 0) {
                break;
            }

            String s = text.substring(i, i + len);
            if (s.indexOf('\n') != -1) {
                int j = s.indexOf('\n');
                s = s.substring(0, j);
                i += j + 1;
            } else {
                i += len;
            }

            canvas.drawText(s, 0, y, textPaint);
        }
        endChar = i;

        if (onPageChangeListener != null) {
            onPageChangeListener.onPageChanged(this);
        }
    }

    public void importText(String text) {
        this.text = text;
    }

    public void turnPage(int step) {
        if (step > 0) {
            startChar = endChar;
        } else {
            startChar -= (endChar - startChar);
            if (startChar < 0) {
                startChar = 0;
            }
        }
        invalidate();
    }

    public void setOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
        this.onPageChangeListener = onPageChangeListener;
    }

    public int getTextLength() {
        return text.length();
    }

    public int getStartChar() {
        return startChar;
    }

    public static abstract class OnPageChangeListener {
        public abstract void onPageChanged(ReaderView v);
    }
}
