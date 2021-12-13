package sixue.naivereader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import java.util.*
import kotlin.math.abs

class ReaderView : View {
    var isGesture = false
    private var text: String? = null
    private var textPaint: TextPaint? = null
    private var fontTop = 0f
    private var fontHeight = 0f
    private var maxWidth = 0
    private var maxHeight = 0
    private var pageMask: ImageView? = null
    private var loadingMask: View? = null
    private var pageBreaks: MutableList<Int>? = null
    var currentPage = 0
        private set
    private var typesetFinished = false
    var currentPosition = 0
        private set
    private var onPageChangeListener: OnPageChangeListener? = null
    private var onTurnPageOverListener: OnTurnPageOverListener? = null
    private var pageAnim: TranslateAnimation? = null
    private var switchAnim = false

    constructor(context: Context?) : super(context) {
        initialize()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    private fun initialize() {
        currentPosition = 0
        pageBreaks = ArrayList()
        currentPage = -1
        typesetFinished = false
        maxWidth = -1
        maxHeight = -1
        textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint!!.setARGB(0xFF, 0, 0, 0)
        textPaint!!.textSize = TEXT_SIZE
        val fm = textPaint!!.fontMetrics
        fontTop = abs(fm.top) + LINE_SPACING
        fontHeight = abs(fm.ascent) + abs(fm.descent) + abs(fm.leading) + LINE_SPACING
    }

    private fun startTypesetThread() {
        val handler = Handler()
        Thread {
            pageBreaks!!.clear()
            currentPage = -1
            typesetFinished = false
            while (maxWidth <= 0 || maxHeight <= 0) {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            var pageBreak = 0
            while (pageBreak < text!!.length) {
                var i = pageBreak
                pageBreaks!!.add(i)
                i = getPage(i, null, null, null)

                // 当前页排版完毕
                if (currentPosition != Int.MAX_VALUE && i > currentPosition && currentPage < 0) {
                    currentPage = pageBreaks!!.size - 1
                    currentPageTypesetFinish(handler)
                }
                pageBreak = i
            }
            typesetFinished = true

            // 当前页是最后一页
            if (currentPage < 0) {
                currentPage = pageBreaks!!.size - 1
                currentPageTypesetFinish(handler)
            }

            // 排版完成
            allPagesTypesetFinish(handler)
        }.start()
    }

    // 更新总页数
    private fun allPagesTypesetFinish(handler: Handler) {
        handler.post {
            Log.d(TAG, "Typeset:pageNum=" + pageBreaks!!.size + " finished.")
            if (onPageChangeListener != null) {
                onPageChangeListener!!.onPageChanged(this@ReaderView)
            }
        }
    }

    // 取消loading，更新当前页码
    private fun currentPageTypesetFinish(handler: Handler) {
        handler.post {
            Log.d(TAG, "Typeset:currentPage=" + currentPage + ",pageNum=" + pageBreaks!!.size + " finished.")
            setLoading(isLoading = false, anim = true)
            if (onPageChangeListener != null) {
                onPageChangeListener!!.onPageChanged(this@ReaderView)
            }
        }
    }

    private fun getPage(n: Int, lines: MutableList<String>?, ys: MutableList<Float>?, lss: MutableList<Float>?): Int {
        var i = n
        var y = fontTop
        while (y < maxHeight) {
            var k = i + MAX_LINE_LENGTH
            if (k > text!!.length) {
                k = text!!.length
            }
            textPaint!!.letterSpacing = 0f
            val len = textPaint!!.breakText(text, i, k, true, (maxWidth - MIN_H_PADDING * 2).toFloat(), null)
            if (len <= 0) {
                break
            }
            var s = text!!.substring(i, i + len)
            var ls: Float
            if (s.indexOf('\n') != -1) {
                val j = s.indexOf('\n')
                s = s.substring(0, j)
                i += j + 1
                ls = 0f
            } else {
                i += len
                val length = textPaint!!.measureText(s, 0, s.length)
                ls = (maxWidth - MIN_H_PADDING * 2 - length) / (s.length - 1) / textPaint!!.textSize
            }
            if (lines != null && ys != null && lss != null) {
                lines.add(s)
                ys.add(y)
                lss.add(ls)
            }
            y += fontHeight
        }
        return i
    }

    override fun performClick(): Boolean {
        return if (isGesture) true else super.performClick()
    }

    public override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        maxWidth = r - l
        maxHeight = b - t
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pos = drawPage(canvas, currentPage)
        Log.d(TAG, "Draw pos: $pos")
    }

    private fun drawPage(canvas: Canvas, page: Int): Int {
        if (page < 0 || page >= pageBreaks!!.size) {
            return -1
        }
        var i = pageBreaks!![page]
        canvas.drawColor(Color.WHITE)
        canvas.drawLine(0f, 0f, 0f, maxHeight.toFloat(), textPaint!!)
        canvas.drawLine(maxWidth.toFloat(), 0f, maxWidth.toFloat(), maxHeight.toFloat(), textPaint!!)
        val lines: MutableList<String> = ArrayList()
        val ys: MutableList<Float> = ArrayList()
        val lss: MutableList<Float> = ArrayList()
        i = getPage(i, lines, ys, lss)
        for (j in lines.indices) {
            textPaint!!.letterSpacing = lss[j]
            canvas.drawText(lines[j], MIN_H_PADDING.toFloat(), ys[j], textPaint!!)
        }
        return i
    }

    fun setLoading(isLoading: Boolean, anim: Boolean) {
        if (isLoading) {
            switchAnim = anim
            loadingMask!!.visibility = VISIBLE
            visibility = INVISIBLE
        } else {
            if (!switchAnim) {
                loadingMask!!.visibility = GONE
                visibility = VISIBLE
            } else {
                turnPage(0)
            }
        }
    }

    fun turnPage(step: Int) {
        val newPage = currentPage + step
        if (newPage < 0 || newPage >= pageBreaks!!.size) {
            if (onTurnPageOverListener != null) {
                onTurnPageOverListener!!.onTurnPageOver(step)
            }
            return
        }
        Log.d(TAG, "Mask:newPage=" + newPage + ",currentPage=" + currentPage + ",pagesNum=" + pageBreaks!!.size)
        if (pageMask != null) {
            if (pageAnim != null) {
                pageMask!!.clearAnimation()
            }
            pageAnim = TranslateAnimation((right * step).toFloat(), 0F, 0F, 0F)
            pageAnim!!.duration = 500
            pageAnim!!.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    pageMask!!.visibility = VISIBLE
                }

                override fun onAnimationEnd(animation: Animation) {
                    currentPage = newPage
                    currentPosition = pageBreaks!![currentPage]
                    invalidate()
                    pageMask!!.visibility = INVISIBLE
                    loadingMask!!.visibility = GONE
                    visibility = VISIBLE
                    if (onPageChangeListener != null) {
                        onPageChangeListener!!.onPageChanged(this@ReaderView)
                    }
                    pageAnim = null
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            drawPage(canvas, newPage)
            pageMask!!.setImageBitmap(bm)
            pageMask!!.startAnimation(pageAnim)
        } else {
            currentPage = newPage
            currentPosition = pageBreaks!![currentPage]
            invalidate()
        }
    }

    fun importText(text: String, currentPosition: Int) {
        val init = this.text == null || this.text!!.isEmpty()
        this.text = if (text.isNotEmpty()) text else " "
        this.currentPosition = currentPosition
        setLoading(true, !init)
        startTypesetThread()
    }

    val maxPages: String
        get() = if (!typesetFinished) {
            "?"
        } else {
            pageBreaks!!.size.toString() + ""
        }

    fun setPageMask(pageMask: ImageView?) {
        this.pageMask = pageMask
    }

    fun setLoadingMask(loadingMask: View?) {
        this.loadingMask = loadingMask
    }

    fun setOnPageChangeListener(onPageChangeListener: OnPageChangeListener?) {
        this.onPageChangeListener = onPageChangeListener
    }

    fun setOnTurnPageOverListener(onTurnPageOverListener: OnTurnPageOverListener?) {
        this.onTurnPageOverListener = onTurnPageOverListener
    }

    abstract class OnPageChangeListener {
        abstract fun onPageChanged(v: ReaderView?)
    }

    abstract class OnTurnPageOverListener {
        abstract fun onTurnPageOver(step: Int)
    }

    companion object {
        private const val TAG = "ReaderView"
        private const val MAX_LINE_LENGTH = 80
        private const val MIN_H_PADDING = 32
        private const val LINE_SPACING = 5
        private const val TEXT_SIZE = 50.0f
    }
}