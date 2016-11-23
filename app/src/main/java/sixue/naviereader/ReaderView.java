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
import android.widget.Toast;

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
    private boolean isRunning;

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

        pageBreaks = new ArrayList<>();
        currentPosition = 0;
        currentPage = -1;
        typesetFinished = false;
        maxWidth = -1;
        maxHeight = -1;
    }

    public void startTypesetThread() {
        isRunning = true;
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isRunning) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }

                    if (!typesetFinished && (maxWidth > 0 && maxHeight > 0)) {
                        // 显示
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(true);
                            }
                        });

                        for (int pageBreak = 0; pageBreak < text.length(); ) {
                            int i = pageBreak;
                            pageBreaks.add(i);

                            i = getPage(i, null, null);

                            // 当前页排版完毕
                            if (i > currentPosition && currentPage < 0) {
                                currentPage = pageBreaks.size() - 1;
                                // 取消loading，更新当前页码
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setLoading(false);
                                        if (onPageChangeListener != null) {
                                            onPageChangeListener.onPageChanged(ReaderView.this);
                                        }
                                    }
                                });
                            }

                            pageBreak = i;
                        }
                        typesetFinished = true;

                        // 排版完成，更新总页数
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (onPageChangeListener != null) {
                                    onPageChangeListener.onPageChanged(ReaderView.this);
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void stopTypesetThread() {
        isRunning = false;
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

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loadingMask.setVisibility(VISIBLE);
            setVisibility(INVISIBLE);
        } else {
            loadingMask.setVisibility(GONE);
            setVisibility(VISIBLE);
        }
    }

    public void turnPage(int step) {
        int i = currentPage + step;
        if (i < 0) {
            i = 0;
            if (currentPage == 0) {
                Toast.makeText(getContext(), R.string.msg_first_page, Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (i >= pageBreaks.size()) {
            i = pageBreaks.size() - 1;
            if (currentPage == pageBreaks.size() - 1) {
                Toast.makeText(getContext(), R.string.msg_last_page, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final int newPage = i;
        Log.d(TAG, "Mask:newPage=" + newPage + ",currentPage=" + currentPage + ",pagesNum=" + pageBreaks.size());

        if (pageMask != null) {
            final Animation anim = new TranslateAnimation(getRight() * step, 0, 0, 0);
            anim.setDuration(500);
            anim.setAnimationListener(new Animation.AnimationListener() {
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
                    if (onPageChangeListener != null) {
                        onPageChangeListener.onPageChanged(ReaderView.this);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            Bitmap bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            drawPage(canvas, newPage);
            pageMask.setImageBitmap(bm);
            pageMask.startAnimation(anim);
        } else {
            currentPage = newPage;
            currentPosition = pageBreaks.get(currentPage);
            invalidate();
        }
    }

    public void importText(String text, int currentPosition) {
        this.text = text;
        this.currentPosition = currentPosition;
        this.typesetFinished = false;
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

    public static abstract class OnPageChangeListener {
        public abstract void onPageChanged(ReaderView v);
    }
}
