package sixue.naviereader;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class ReaderView extends View {
    private List<String> lines;
    private int startLine = 0;
    private int startChar = 0;

    public ReaderView(Context context) {
        super(context);
    }

    public ReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onDraw(Canvas canvas) {
        TextPaint textPaint = new TextPaint();
        textPaint.setARGB(0xFF, 0, 0, 0);
        textPaint.setTextSize(36.0f);

        int h = 0;
        for (int i = startLine; i < lines.size() && h < 1000; i++) {
            String s = lines.get(i).substring(startChar);
            canvas.drawText(s, 0, h, textPaint);
            h += 72;
        }

        super.onDraw(canvas);
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }
}
