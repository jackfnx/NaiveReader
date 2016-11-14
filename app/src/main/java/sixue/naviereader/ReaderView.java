package sixue.naviereader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class ReaderView extends View {
    private TextPaint textPaint;
    private float fontTop;
    private float fontHeight;
    private int startChar = 0;
    private String text;

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
        float maxWidth = canvas.getWidth();
        float maxHeight = canvas.getHeight();
        int i = startChar;
        for (float y = fontTop; y < maxHeight; y += fontHeight) {
            int len = textPaint.breakText(text, i, i + 80, true, maxWidth, null);
            if (len <= 0) {
                break;
            }

            String s = text.substring(i, i + len);
            while (s.indexOf('\n') != -1) {
                int j = s.indexOf('\n');
                String s1 = s.substring(0, j);
                canvas.drawText(s1, 0, y, textPaint);
                s = s.substring(j + 1);
                y += fontHeight;
            }

            canvas.drawText(s, 0, y, textPaint);

            i += len;
        }
        super.onDraw(canvas);
    }

    public void setText(String text) {
        this.text = text;
    }
}
