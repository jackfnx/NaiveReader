package sixue.naivereader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import sixue.naivereader.ReaderView.OnTurnPageOverListener
import sixue.naivereader.data.Book
import sixue.naivereader.data.BookKind
import sixue.naivereader.data.Chapter
import java.util.*

class ReadActivity : AppCompatActivity(), OnTouchListener, GestureDetector.OnGestureListener {
    private lateinit var detector: GestureDetector
    private lateinit var readerView: ReaderView
    private lateinit var batteryReceiver: BroadcastReceiver
    private lateinit var receiver: BroadcastReceiver
    private lateinit var book: Book
    private lateinit var smartDownloader: SmartDownloader
    private lateinit var actionBar: ActionBar
    private var chapter: Chapter = emptyChapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        actionBar = supportActionBar!!
        BookLoader.reload(this, true)
        book = BookLoader.getBook(0)!!
        readerView = findViewById(R.id.text_area)
        val maskView = findViewById<ImageView>(R.id.page_mask)
        val loading = findViewById<View>(R.id.loading_mask)
        val title = findViewById<TextView>(R.id.title)
        val subtitle = findViewById<TextView>(R.id.subtitle)
        val progress = findViewById<TextView>(R.id.progress)
        val battery = findViewById<TextView>(R.id.battery)
        detector = GestureDetector(this, this)
        detector.setIsLongpressEnabled(true)
        readerView.setOnTouchListener(this)
        readerView.setPageMask(maskView)
        readerView.setLoadingMask(loading)
        readerView.setOnPageChangeListener(object : ReaderView.OnPageChangeListener() {
            override fun onPageChanged(v: ReaderView?) {
                val maxPages = readerView.maxPages
                val currentPage = readerView.currentPage
                progress.text = String.format(Locale.CHINA, "%d/%s", currentPage + 1, maxPages)
                book.currentPosition = readerView.currentPosition
                val (update, chapterTitle) = book.buildHelper().updateChapterTitleOnPageChange()
                if (update) {
                    subtitle.text = chapterTitle
                }
                BookLoader.save()
            }
        })
        readerView.setOnTurnPageOverListener(object : OnTurnPageOverListener() {
            override fun onTurnPageOver(step: Int) {
                val (notOver, i, j) = book.buildHelper().calcTurnPageNewIndex(step)
                if (notOver) {
                    when {
                        book.kind === BookKind.Online -> {
                            loadNetChapter(i, j)
                        }
                        book.kind === BookKind.Archive -> {
                            loadArchiveChapter(i, j)
                        }
                        book.kind === BookKind.Packet -> {
                            loadPackChapter(i, j)
                        }
                    }
                    return
                }
                if (step < 0) {
                    Toast.makeText(this@ReadActivity, R.string.msg_first_page, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ReadActivity, R.string.msg_last_page, Toast.LENGTH_SHORT).show()
                }
            }
        })
        val newIndex = intent.getIntExtra(Utils.INTENT_PARA_CHAPTER_INDEX, -1)
        val newPosition = intent.getIntExtra(Utils.INTENT_PARA_CURRENT_POSITION, -1)
        if (newPosition >= 0) {
            book.currentPosition = newPosition
        }
        title.text = "?"
        subtitle.text = "?"
        val myFilter = IntentFilter()
        myFilter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH)
        myFilter.addAction(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return
                when (action) {
                    Utils.ACTION_DOWNLOAD_CONTENT_FINISH -> if (book.id == intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID)) {
                        loadNetChapter(newIndex, 0)
                    }
                    Utils.ACTION_DOWNLOAD_CHAPTER_FINISH -> if (book.id == intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID) && chapter.id == intent.getStringExtra(Utils.INTENT_PARA_CHAPTER_ID)) {
                        title.text = book.title
                        subtitle.text = chapter.title
                        val text = book.buildHelper().readText(chapter, context)
                        readerView.importText(text, book.currentPosition)
                    }
                    else -> {}
                }
            }
        }
        registerReceiver(receiver, myFilter)
        battery.text = "?"
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                    val level = intent.getIntExtra("level", 0)
                    val scale = intent.getIntExtra("scale", 100)
                    battery.text = String.format(Locale.CHINA, "%d%%", level * 100 / scale)
                }
            }
        }
        registerReceiver(batteryReceiver, filter)
        smartDownloader = SmartDownloader(this, book)
        if (book.buildHelper().reloadContent(this)) {
            when {
                book.kind === BookKind.Archive -> {
                    loadArchiveChapter(newIndex, newPosition)
                }
                book.kind === BookKind.Packet -> {
                    loadPackChapter(newIndex, newPosition)
                }
                book.kind === BookKind.Online -> {
                    val intent = Intent(Utils.ACTION_DOWNLOAD_CONTENT_FINISH)
                    intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
                    sendBroadcast(intent)
                }
                book.kind === BookKind.LocalText -> {
                    val intent = Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH)
                    intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
                    intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, "")
                    intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, book.currentPosition)
                    sendBroadcast(intent)
                }
            }
        } else {
            readerView.setLoading(isLoading = true, anim = false)
            smartDownloader.startDownloadContent()
        }
    }

    private fun loadPackChapter(newIndex: Int, newPosition: Int) {
        if (newIndex >= 0 && newIndex < book.chapterList.size) {
            if (book.currentChapterIndex != newIndex) {
                book.currentChapterIndex = newIndex
                book.currentPosition = newPosition
            }
        }
        if (book.chapterList.isEmpty()) {
            return
        }
        chapter = book.chapterList[book.currentChapterIndex]
        val intent = Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH)
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
        intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.id)
        intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, book.currentPosition)
        sendBroadcast(intent)
    }

    private fun loadNetChapter(newIndex: Int, newPosition: Int) {
        readerView.setLoading(isLoading = true, anim = false)
        if (newIndex >= 0 && newIndex < book.chapterList.size) {
            if (book.currentChapterIndex != newIndex) {
                book.currentChapterIndex = newIndex
                book.currentPosition = newPosition
            }
        }
        if (book.chapterList.isEmpty()) {
            return
        }
        if (book.currentChapterIndex < 0) {
            book.currentChapterIndex = 0
        }
        if (book.currentChapterIndex >= book.chapterList.size) {
            book.currentChapterIndex = book.chapterList.size - 1
        }
        chapter = book.chapterList[book.currentChapterIndex]
        if (smartDownloader.isDownloaded(chapter)) {
            val intent = Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH)
            intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
            intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.id)
            intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, book.currentPosition)
            sendBroadcast(intent)
        } else {
            smartDownloader.startDownloadChapter(chapter)
        }
    }

    private fun loadArchiveChapter(newIndex: Int, newPosition: Int) {
        if (newIndex >= 0 && newIndex < book.chapterList.size) {
            if (book.currentChapterIndex != newIndex) {
                book.currentChapterIndex = newIndex
                book.currentPosition = newPosition
            }
        }
        if (book.chapterList.isEmpty()) {
            return
        }
        chapter = book.chapterList[book.currentChapterIndex]
        val intent = Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH)
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
        intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.id)
        intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, book.currentPosition)
        sendBroadcast(intent)
    }

    public override fun onStart() {
        super.onStart()
        actionBar.setDisplayShowTitleEnabled(false)
        val color = ContextCompat.getColor(this, R.color.colorPrimary)
        val transparentColor = Color.argb(0x99, Color.red(color), Color.green(color), Color.blue(color))
        actionBar.setBackgroundDrawable(ColorDrawable(transparentColor))
        actionBar.hide()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.read, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.refresh).isEnabled = book.isRefreshable()
        menu.findItem(R.id.browse_it).isEnabled = book.isViewableInBrowser()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.content -> {
                val intent = Intent(this, ContentActivity::class.java)
                startActivity(intent)
                actionBar.hide()
                return true
            }
            R.id.refresh -> {
                run {
                    if (book.isRefreshable()) {
                        Utils.deleteFile(chapter.savePath)
                        loadNetChapter(book.currentChapterIndex, 0)
                        return true
                    }
                }
                run {
                    if (book.isRefreshable()) {
                        val url = smartDownloader.getChapterUrl(chapter)
                        val intent = Intent("android.intent.action.VIEW")
                        intent.data = Uri.parse(url)
                        startActivity(intent)
                        actionBar.hide()
                        return true
                    }
                }
            }
            R.id.browse_it -> {
                if (book.isViewableInBrowser()) {
                    val url = smartDownloader.getChapterUrl(chapter)
                    val intent = Intent("android.intent.action.VIEW")
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                    actionBar.hide()
                    return true
                }
            }
            else -> {}
        }
        return onOptionsItemSelected(menuItem)
    }

    public override fun onDestroy() {
        unregisterReceiver(receiver)
        unregisterReceiver(batteryReceiver)
        super.onDestroy()
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        //Log.i(getClass().toString(), "onDown");
        return true
    }

    override fun onShowPress(motionEvent: MotionEvent) {
        //Log.i(getClass().toString(), "onShowPress");
    }

    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        //Log.i(getClass().toString(), "onSingleTapUp");
        if (actionBar.isShowing) {
            actionBar.hide()
        } else {
            val metrics = windowManager.currentWindowMetrics
            val widthPixels = metrics.bounds.width()
            val heightPixels = metrics.bounds.height()
            val x = motionEvent.rawX
            val y = motionEvent.rawY
            Log.d(javaClass.toString(), "Display:width=$widthPixels,height=$heightPixels; Touch:x=$x,y=$y")
            if (x < widthPixels / 3 && y < heightPixels / 2) {
                readerView.turnPage(-1)
            } else if (x > widthPixels / 3 && x < widthPixels * 2 / 3 && y < heightPixels / 2) {
                actionBar.show()
            } else {
                readerView.turnPage(1)
            }
        }
        return true
    }

    override fun onScroll(motionEvent: MotionEvent, motionEvent1: MotionEvent, vX: Float, vY: Float): Boolean {
        //Log.i(getClass().toString(), "onScroll");
        return false
    }

    override fun onLongPress(motionEvent: MotionEvent) {
        //Log.i(getClass().toString(), "onLongPress");
        actionBar.show()
    }

    override fun onFling(motionEvent: MotionEvent, motionEvent1: MotionEvent, vX: Float, vY: Float): Boolean {
        //Log.i(getClass().toString(), "onFling");
        Log.d(javaClass.toString(), "Fling:vX=$vX,vY=$vY")
        if (vY < 2000 && vY > -2000) {
            if (vX > 0) {
                actionBar.hide()
                readerView.turnPage(-1)
            } else {
                actionBar.hide()
                readerView.turnPage(1)
            }
            //        } else {
//            if (vX < 2000 && vX > -2000) {
//                if (vY < 0) {
//                    Intent intent = new Intent(this, ContentActivity.class);
//                    startActivity(intent);
//                }
//            }
        }
        return true
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        //Log.i(getClass().toString(), "onTouch");
        readerView.isGesture = detector.onTouchEvent(motionEvent)
        return readerView.performClick()
    }

    companion object {
        private val emptyChapter = Chapter(id = "", title = "")
    }
}