package sixue.naivereader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import sixue.naivereader.data.Book
import sixue.naivereader.data.BookKind
import sixue.naivereader.helper.LocalTextLoader.calcChapterNodes
import sixue.naivereader.provider.NetProviderCollections.findProviders
import java.util.*
import kotlin.collections.ArrayList

class ContentActivity : AppCompatActivity() {
    private lateinit var receiver: BroadcastReceiver
    private lateinit var downloader: SmartDownloader
    private lateinit var book: Book
    private lateinit var localText: String
    private var localChapterNodes: List<Int> = ArrayList()
    private var currentLocalChapter = 0
    private var providerIds: List<String> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)
        book = BookLoader.getBook(0)!!
        downloader = SmartDownloader(this, book)

        val listView = findViewById<ListView>(R.id.content)
        val myAdapter = MyAdapter(book)
        val srl = findViewById<SwipeRefreshLayout>(R.id.srl)
        listView.adapter = myAdapter
        listView.onItemClickListener = OnItemClickListener { _, _, i, _ ->
            val intent = Intent(this@ContentActivity, ReadActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            when {
                book.kind === BookKind.Online -> {
                    val index = book.chapterList.size - i - 1
                    intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, index)
                    intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0)
                }
                book.kind === BookKind.Packet -> {
                    intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, i)
                    intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, 0)
                }
                book.kind === BookKind.LocalText -> {
                    intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, 0)
                    intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, localChapterNodes[i])
                }
            }
            startActivity(intent)
            finish()
        }
        val filter = IntentFilter()
        filter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH)
        filter.addAction(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH)
        filter.addAction(Utils.ACTION_DOWNLOAD_ALL_CHAPTER_FINISH)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return
                when (action) {
                    Utils.ACTION_DOWNLOAD_CONTENT_FINISH -> if (book.id == intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID)) {
                        srl.isRefreshing = false
                        listView.setSelection(book.chapterList.size - book.currentChapterIndex - 1)
                        myAdapter.notifyDataSetChanged()
                    }
                    Utils.ACTION_DOWNLOAD_CHAPTER_FINISH -> if (book.id == intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID)) {
                        myAdapter.notifyDataSetChanged()
                    }
                    Utils.ACTION_DOWNLOAD_ALL_CHAPTER_FINISH -> if (book.id == intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID)) {
                        Toast.makeText(this@ContentActivity, R.string.msg_batch_download_finish, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
        registerReceiver(receiver, filter)
        srl.setOnRefreshListener {
            if (book.kind === BookKind.Online) {
                downloader.startDownloadContent()
            }
        }
        if (book.buildHelper().reloadContent(this)) {
            if (book.kind === BookKind.LocalText) {
                localText = Utils.readText(book.localPath)!!
                localChapterNodes = calcChapterNodes(localText)
                currentLocalChapter = 0
                for (i in localChapterNodes.indices) {
                    val node = localChapterNodes[i]
                    val next = if (i + 1 < localChapterNodes.size) localChapterNodes[i + 1] else Int.MAX_VALUE
                    if (book.currentPosition in node until next) {
                        currentLocalChapter = i
                        break
                    }
                }
                listView.setSelection(currentLocalChapter)
            } else if (book.kind === BookKind.Online) {
                listView.setSelection(book.chapterList.size - book.currentChapterIndex - 1)
            } else if (book.kind === BookKind.Packet) {
                listView.setSelection(book.currentChapterIndex)
            }
        } else {
            srl.isRefreshing = true
            downloader.startDownloadContent()
        }
    }

    public override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private inner class MyAdapter(private val book: Book) : BaseAdapter() {
        override fun getCount(): Int {
            return if (book.kind === BookKind.LocalText) localChapterNodes.size else book.chapterList.size
        }

        override fun getItem(i: Int): Any {
            return ""
        }

        override fun getItemId(i: Int): Long {
            return 0
        }

        override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
            val view: View?
            if (convertView == null) {
                view = LayoutInflater.from(this@ContentActivity).inflate(R.layout.listviewitem_content, viewGroup, false)
                view.setPadding(20, 20, 20, 20)
                val title = view.findViewById<TextView>(R.id.title)
                val summary = view.findViewById<TextView>(R.id.summary)
                view.tag = ViewHolder(title, summary)
            } else {
                view = convertView
            }
            val viewHolder : ViewHolder = (view!!.tag as ViewHolder)
            if (book.kind === BookKind.LocalText) {
                val node = localChapterNodes[i]
                val length = localText.length
                var end = localText.indexOf('\n', node)
                end = if (end < 0) length else end
                var s = localText.substring(node, end)
                if (i == currentLocalChapter) {
                    s += "*"
                }
                viewHolder.title.text = s
                val sumStart = node + s.length
                val sumEnd = if (sumStart + MAX_SUMMARY_LENGTH > length) length else sumStart + MAX_SUMMARY_LENGTH
                val sum = localText.substring(sumStart, sumEnd).trim { it <= ' ' }.replace('\n', ' ')
                viewHolder.summary.text = sum
                viewHolder.summary.gravity = Gravity.START
            } else {
                val index: Int = if (book.kind === BookKind.Online) book.chapterList.size - i - 1 else i
                val chapter = book.chapterList[index]
                var s = chapter.title
                if (index == book.currentChapterIndex) {
                    s += "*"
                }
                viewHolder.title.text = s
                if (downloader.isDownloaded(chapter)) {
                    viewHolder.summary.setText(R.string.download)
                } else {
                    viewHolder.summary.text = ""
                }
                viewHolder.summary.gravity = Gravity.END
            }
            return view
        }

        inner class ViewHolder(val title: TextView, val summary: TextView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.content, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val menuProviders = menu.findItem(R.id.menu_providers)
        if (book.kind !== BookKind.Online) {
            menuProviders.isVisible = false
        } else {
            val subMenu = menuProviders.subMenu
            providerIds.toMutableList().clear()
            subMenu.clear()
            for (source in book.sources) {
                val netProvider = findProviders(source.id, this)
                if (netProvider != null) {
                    val id = netProvider.providerId
                    var name = netProvider.providerName
                    if (book.siteId == id) {
                        name += "*"
                    }
                    providerIds.toMutableList().add(id)
                    subMenu.add(Menu.NONE, Menu.FIRST + providerIds.size - 1, providerIds.size, name)
                }
            }
            subMenu.add(Menu.NONE, Menu.FIRST + providerIds.size, providerIds.size + 1, R.string.menu_search_again)
        }
        val batchDownload = menu.findItem(R.id.menu_batch_download)
        if (book.kind !== BookKind.Online) {
            batchDownload.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.menu_batch_download) {
            downloader.startDownloadAllChapter()
            return true
        }
        val i = menuItem.itemId - Menu.FIRST
        if (i < providerIds.size) {
            val providerId = providerIds[i]
            for (source in book.sources) {
                if (source.id == providerId) {
                    book.siteId = source.id
                    book.sitePara = source.para
                    BookLoader.save()
                    invalidateOptionsMenu()
                    return true
                }
            }
            return true
        } else if (i == providerIds.size) {
            Toast.makeText(this, R.string.not_implemented, Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }

    companion object {
        private const val MAX_SUMMARY_LENGTH = 40
    }
}