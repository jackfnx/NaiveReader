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
import sixue.naivereader.provider.NetProviderCollections.findProviders

class ContentActivity : AppCompatActivity() {
    private lateinit var receiver: BroadcastReceiver
    private lateinit var downloader: SmartDownloader
    private lateinit var book: Book
    private var providerIds: List<String> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)
        book = BookLoader.getBook(0)!!
        downloader = SmartDownloader(this, book)

        val listView = findViewById<ListView>(R.id.content)
        val myAdapter = MyAdapter(book)
        val srl = findViewById<SwipeRefreshLayout>(R.id.srl)
        srl.isEnabled = book.isRefreshable()
        listView.adapter = myAdapter
        listView.onItemClickListener = OnItemClickListener { _, _, i, _ ->
            val intent = Intent(this@ContentActivity, ReadActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            val (currentIdx, currentPos) = book.buildHelper().calcCurrentPosition(i)
            intent.putExtra(Utils.INTENT_PARA_CHAPTER_INDEX, currentIdx)
            intent.putExtra(Utils.INTENT_PARA_CURRENT_POSITION, currentPos)
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
                        listView.setSelection(book.buildHelper().getCurrentSeemingIndex())
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
            if (book.isRefreshable()) {
                downloader.startDownloadContent()
            }
        }
        if (book.buildHelper().reloadContent(this)) {
            listView.setSelection(book.buildHelper().getCurrentSeemingIndex())
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
            return book.buildHelper().getChapterSize()
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
            val (title, sum, gravity) = book.buildHelper().getChapterDescription(i, this@ContentActivity)
            viewHolder.title.text = title
            viewHolder.summary.text = sum
            viewHolder.summary.gravity = gravity
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
        if (!book.isRefreshable()) {
            menuProviders.isVisible = false
        } else {
            val subMenu = menuProviders.subMenu
            providerIds.toMutableList().clear()
            subMenu?.clear()
            for (source in book.sources) {
                val netProvider = findProviders(source.id, this)
                if (netProvider != null) {
                    val id = netProvider.providerId
                    var name = netProvider.providerName
                    if (book.siteId == id) {
                        name += "*"
                    }
                    providerIds.toMutableList().add(id)
                    subMenu?.add(Menu.NONE, Menu.FIRST + providerIds.size - 1, providerIds.size, name)
                }
            }
            subMenu?.add(Menu.NONE, Menu.FIRST + providerIds.size, providerIds.size + 1, R.string.menu_search_again)
        }
        val batchDownload = menu.findItem(R.id.menu_batch_download)
        if (!book.isRefreshable()) {
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
}