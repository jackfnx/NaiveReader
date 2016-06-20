package sixue.naviereader;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

import java.util.List;

public class ReaderView extends View {
    private List<String> lines;
    private int startLine;
    private int startChar;

    public ReaderView(Context context) {
        super(context);
    }

    @Override
    public void onDraw(Canvas canvas) {
        TextPaint textPaint = new TextPaint();
        textPaint.setARGB(0xFF, 0, 0, 0);
        textPaint.setTextSize(18.0f);
        String s = lines.get(startLine).substring(startChar);
        StaticLayout sl = new StaticLayout(s, textPaint, canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);

        super.onDraw(canvas);
    }
}
