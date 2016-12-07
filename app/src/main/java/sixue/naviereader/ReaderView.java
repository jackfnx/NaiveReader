package sixue.naviereader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class ReaderView extends View {
    private static final String TAG = "ReaderView";

    private static final int MAX_LINE_LENGTH = 80;
    private static final int H_PADDING = 16;

    private String text;
    private TextPaint textPaint;
    private float fontTop;
    private float fontHeight;
    private int maxWidth;
    private int maxHeight;
    private ImageView pageMask;
    private View loadingMask;
    private List<Integer> pageBreaks;
    private int currentPage;
    private boolean typesetFinished;
    private int currentPosition;
    private OnPageChangeListener onPageChangeListener;
    private OnTurnPageOverListener onTurnPageOverListener;
    private TranslateAnimation pageAnim;
    private boolean switchAnim;

    public ReaderView(Context context) {
        super(context);
        initialize();
    }

    public ReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setARGB(0xFF, 0, 0, 0);
        textPaint.setTextSize(50.0f);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        fontTop = Math.abs(fm.top);
        fontHeight = Math.abs(fm.ascent) + Math.abs(fm.descent) + Math.abs(fm.leading);

        currentPosition = 0;
        pageBreaks = new ArrayList<>();
        currentPage = -1;
        typesetFinished = false;
        maxWidth = -1;
        maxHeight = -1;
    }

    private void startTypesetThread() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                pageBreaks.clear();
                currentPage = -1;
                typesetFinished = false;

                while (maxWidth <= 0 || maxHeight <= 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                for (int pageBreak = 0; pageBreak < text.length(); ) {
                    int i = pageBreak;
                    pageBreaks.add(i);

                    i = getPage(i, null, null);

                    // 当前页排版完毕
                    if (currentPosition != Integer.MAX_VALUE && i > currentPosition && currentPage < 0) {
                        currentPage = pageBreaks.size() - 1;
                        currentPageTypesetFinish(handler);
                    }

                    pageBreak = i;
                }
                typesetFinished = true;

                // 当前页是最后一页
                if (currentPage < 0) {
                    currentPage = pageBreaks.size() - 1;
                    currentPageTypesetFinish(handler);
                }

                // 排版完成
                allPagesTypesetFinish(handler);
            }
        }).start();
    }

    // 更新总页数
    private void allPagesTypesetFinish(Handler handler) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Typeset:pageNum=" + pageBreaks.size() + " finished.");
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageChanged(ReaderView.this);
                }
            }
        });
    }

    // 取消loading，更新当前页码
    private void currentPageTypesetFinish(Handler handler) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Typeset:currentPage=" + currentPage + ",pageNum=" + pageBreaks.size() + " finished.");
                setLoading(false, true);
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageChanged(ReaderView.this);
                }
            }
        });
    }

    private int getPage(int i, List<String> lines, List<Float> ys) {
        for (float y = fontTop; y < maxHeight; y += fontHeight) {
            int k = i + MAX_LINE_LENGTH;
            if (k > text.length()) {
                k = text.length();
            }
            int len = textPaint.breakText(text, i, k, true, maxWidth - H_PADDING * 2, null);
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

            if (lines != null && ys != null) {
                lines.add(s);
                ys.add(y);
            }
        }
        return i;
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        maxWidth = r - l;
        maxHeight = b - t;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawPage(canvas, currentPage);
    }

    private int drawPage(Canvas canvas, int page) {
        if (page < 0 || page >= pageBreaks.size()) {
            return -1;
        }

        int i = pageBreaks.get(page);

        canvas.drawColor(Color.WHITE);
        canvas.drawLine(0, 0, 0, maxHeight, textPaint);
        canvas.drawLine(maxWidth, 0, maxWidth, maxHeight, textPaint);

        List<String> lines = new ArrayList<>();
        List<Float> ys = new ArrayList<>();
        i = getPage(i, lines, ys);
        for (int j = 0; j < lines.size(); j++) {
            canvas.drawText(lines.get(j), H_PADDING, ys.get(j), textPaint);
        }

        return i;
    }

    private void setLoading(boolean isLoading, boolean anim) {
        if (isLoading) {
            switchAnim = anim;
            loadingMask.setVisibility(VISIBLE);
            setVisibility(INVISIBLE);
        } else {
            if (!switchAnim) {
                loadingMask.setVisibility(GONE);
                setVisibility(VISIBLE);
            } else {
                turnPage(0);
            }
        }
    }

    public void turnPage(int step) {
        final int newPage = currentPage + step;
        if (newPage < 0 || newPage >= pageBreaks.size()) {
            if (onTurnPageOverListener != null) {
                onTurnPageOverListener.onTurnPageOver(step);
            }
            return;
        }
        Log.d(TAG, "Mask:newPage=" + newPage + ",currentPage=" + currentPage + ",pagesNum=" + pageBreaks.size());

        if (pageMask != null) {
            if (pageAnim != null) {
                pageMask.clearAnimation();
            }

            pageAnim = new TranslateAnimation(getRight() * step, 0, 0, 0);
            pageAnim.setDuration(500);
            pageAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    pageMask.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    currentPage = newPage;
                    currentPosition = pageBreaks.get(currentPage);
                    invalidate();
                    pageMask.setVisibility(View.INVISIBLE);
                    loadingMask.setVisibility(GONE);
                    setVisibility(VISIBLE);
                    if (onPageChangeListener != null) {
                        onPageChangeListener.onPageChanged(ReaderView.this);
                    }
                    pageAnim = null;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            Bitmap bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            drawPage(canvas, newPage);
            pageMask.setImageBitmap(bm);
            pageMask.startAnimation(pageAnim);
        } else {
            currentPage = newPage;
            currentPosition = pageBreaks.get(currentPage);
            invalidate();
        }
    }

    public void importText(String text, int currentPosition) {
        boolean init = (this.text == null) || (this.text.length() == 0);
        this.text = text;
        this.currentPosition = currentPosition;
        setLoading(true, !init);
        startTypesetThread();
    }

    public String getMaxPages() {
        if (!typesetFinished) {
            return "?";
        } else {
            return pageBreaks.size() + "";
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setPageMask(ImageView pageMask) {
        this.pageMask = pageMask;
    }

    public void setLoadingMask(View loadingMask) {
        this.loadingMask = loadingMask;
    }

    public void setOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
        this.onPageChangeListener = onPageChangeListener;
    }

    public void setOnTurnPageOverListener(OnTurnPageOverListener onTurnPageOverListener) {
        this.onTurnPageOverListener = onTurnPageOverListener;
    }

    public static abstract class OnPageChangeListener {
        public abstract void onPageChanged(ReaderView v);
    }

    public static abstract class OnTurnPageOverListener {
        public abstract void onTurnPageOver(int step);
    }
}
