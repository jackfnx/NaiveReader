package sixue.naivereader

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import sixue.naivereader.data.Book
import sixue.naivereader.data.BookKind
import java.util.*

class BookshelfActivity : AppCompatActivity() {
    private lateinit var myAdapter: MyAdapter
    private lateinit var editList: MutableList<Book>
    private lateinit var receiver: BroadcastReceiver
    private var isEditMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookshelf)
        Utils.verifyPermissions(this)
        BookLoader.reload(this)
        isEditMode = false
        editList = ArrayList()
        val gv = findViewById<GridView>(R.id.gridview_books)
        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        val srl = findViewById<SwipeRefreshLayout>(R.id.srl)
        myAdapter = MyAdapter()
        gv.adapter = myAdapter
        gv.onItemClickListener = OnItemClickListener { _, view, i, _ ->
            if (!isEditMode) {
                BookLoader.bookBubble(i)
                val intent = Intent(this@BookshelfActivity, ReadActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            } else {
                val selectIcon = view.findViewById<View>(R.id.select_icon)
                val checked = !selectIcon.isSelected
                checkItem(selectIcon, i, checked)
            }
        }
        gv.onItemLongClickListener = OnItemLongClickListener { _, view, i, _ ->
            if (!isEditMode) {
                setEditMode(true)
                val selectIcon = view.findViewById<View>(R.id.select_icon)
                checkItem(selectIcon, i, true)
                return@OnItemLongClickListener true
            }
            false
        }
        fab.setOnClickListener {
            if (!isEditMode) {
                val intent = Intent(this@BookshelfActivity, AddActivity::class.java)
                startActivity(intent)
            }
        }
        srl.setOnRefreshListener {
            runOnUiThread {
                refreshAllBooks()
                srl.isRefreshing = false
            }
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                myAdapter.notifyDataSetChanged()
            }
        }
        val filter = IntentFilter()
        filter.addAction(Utils.ACTION_DOWNLOAD_CONTENT_FINISH)
        filter.addAction(Utils.ACTION_DOWNLOAD_COVER_FINISH)
        registerReceiver(receiver, filter)
        refreshAllBooks()
    }

    private fun refreshAllBooks() {
        for (i in 0 until BookLoader.bookNum) {
            val book = BookLoader.getBook(i)
            if (book?.kind == BookKind.Online) {
                if (book.buildHelper().reloadContent(this)) {
                    val intent = Intent(Utils.ACTION_DOWNLOAD_CONTENT_FINISH)
                    intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.id)
                    sendBroadcast(intent)
                }
                val downloader = SmartDownloader(this, book)
                if (!book.end) {
                    downloader.startDownloadContent()
                }
//                if (book.buildHelper().loadCoverBitmap(this) == null) {
//                    downloader.startDownloadContent()
//                }
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    public override fun onStart() {
        super.onStart()
        myAdapter.notifyDataSetChanged()
    }

    private fun checkItem(selectIcon: View, i: Int, checked: Boolean) {
        selectIcon.isSelected = checked
        if (checked) {
            BookLoader.getBook(i)?.let { editList.add(it) }
        } else {
            editList.remove(BookLoader.getBook(i))
        }
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bookshelf, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_delete -> if (isEditMode) {
                BookLoader.deleteBooks(editList)
                editList.clear()
                setEditMode(false)
                return true
            }
            R.id.menu_edit -> if (editList.size == 1) {
                editItem(editList[0])
                editList.clear()
                setEditMode(false)
                return true
            }
            R.id.menu_settings -> {
                val intent = Intent(this, NetProviderManagerActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> {}
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun editItem(book: Book) {
        if (book.kind == BookKind.LocalText) {
            val v = View.inflate(this, R.layout.edit_dialog_local, null)
            val title = v.findViewById<EditText>(R.id.title)
            title.setText(book.title)
            title.clearFocus()
            val author = v.findViewById<EditText>(R.id.author)
            author.setText(book.author)
            author.clearFocus()
            val localPath = v.findViewById<EditText>(R.id.local_path)
            localPath.hint = book.localPath
            localPath.clearFocus()
            val getTextDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                if (uri != null) {
                    val takeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    book.localPath = uri.toString()
                    BookLoader.save()
                    localPath.hint = uri.toString()
                }
            }
            val browser = v.findViewById<Button>(R.id.button_browser)
            browser.setOnClickListener {
                getTextDocument.launch(arrayOf("text/plain"))
                localPath.setHint(R.string.HintTextEditing)
            }
            AlertDialog.Builder(this)
                    .setTitle("Local book")
                    .setView(v)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        book.title = title.text.toString()
                        book.author = author.text.toString()
                        BookLoader.save()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        } else if (book.kind == BookKind.Online) {
            val v = View.inflate(this, R.layout.edit_dialog_net, null)
            val title = v.findViewById<EditText>(R.id.title)
            title.setText(book.title)
            title.clearFocus()
            val author = v.findViewById<EditText>(R.id.author)
            author.setText(book.author)
            author.clearFocus()
            val sources = v.findViewById<Spinner>(R.id.sources)
            val sourceNames = Utils.convert(book.sources){ it.id }
            val adapter: SpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sourceNames)
            sources.adapter = adapter
            sources.clearFocus()
            val end = v.findViewById<CheckBox>(R.id.end)
            end.isChecked = book.end
            end.clearFocus()
            AlertDialog.Builder(this)
                    .setTitle("Net book")
                    .setView(v)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        book.title = title.text.toString()
                        book.author = author.text.toString()
                        book.end = end.isChecked
                        BookLoader.save()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val edit = menu.findItem(R.id.menu_edit)
        val delete = menu.findItem(R.id.menu_delete)
        val settings = menu.findItem(R.id.menu_settings)
        edit.isVisible = isEditMode
        delete.isVisible = isEditMode
        settings.isVisible = !isEditMode
        edit.isEnabled = editList.size == 1
        delete.isEnabled = editList.size >= 1
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        myAdapter.notifyDataSetChanged()
        supportActionBar?.setDisplayShowTitleEnabled(!editMode)
        invalidateOptionsMenu()
    }

    override fun onBackPressed() {
        if (isEditMode) {
            editList.clear()
            setEditMode(false)
            return
        }
        super.onBackPressed()
    }

    private inner class MyAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return BookLoader.bookNum
        }

        override fun getItem(i: Int): Any {
            return ""
        }

        override fun getItemId(i: Int): Long {
            return 0
        }

        override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
            val view : View?
            if (convertView == null) {
                view = LayoutInflater.from(this@BookshelfActivity).inflate(R.layout.gridviewitem_book, viewGroup, false)
                val title = view.findViewById<TextView>(R.id.title)
                val progress = view.findViewById<TextView>(R.id.progress)
                val selectIcon = view.findViewById<View>(R.id.select_icon)
                val cover = view.findViewById<ImageView>(R.id.cover)
                view.tag = ViewHolder(title, progress, selectIcon, cover)
            } else {
                view = convertView
            }
            val viewHolder : ViewHolder = (view!!.tag as ViewHolder)

            val book = BookLoader.getBook(i)
            viewHolder.title.text = book?.title
            if (book?.kind == BookKind.LocalText) {
                val cp = book.currentPosition
                val wc = book.wordCount
                if (wc <= 0) {
                    viewHolder.progress.setText(R.string.read_progress_local_unread)
                } else {
                    viewHolder.progress.text = getString(R.string.read_progress_local, cp.toFloat() / wc.toFloat() * 100f)
                }
            } else if (book?.kind == BookKind.Online) {
                val size = book.chapterList.size
                val cp = book.currentChapterIndex
                if (size <= 0) {
                    viewHolder.progress.setText(R.string.read_progress_net_predownload)
                } else if (cp + 1 == size) {
                    if (book.end) {
                        viewHolder.progress.setText(R.string.read_progress_net_end)
                    } else {
                        viewHolder.progress.setText(R.string.read_progress_net_allread)
                    }
                } else {
                    viewHolder.progress.text = getString(R.string.read_progress_net, size - cp - 1)
                }
            } else if (book?.kind == BookKind.Packet) {
                val cp = book.currentChapterIndex
                viewHolder.progress.text = getString(R.string.read_progress_net, cp)
            }
            if (!isEditMode) {
                viewHolder.selectIcon.visibility = View.INVISIBLE
            } else {
                viewHolder.selectIcon.visibility = View.VISIBLE
            }
            viewHolder.selectIcon.isSelected = editList.contains(book)
            viewHolder.cover.setImageBitmap(book?.buildHelper()?.loadCoverBitmap(this@BookshelfActivity))
            return view
        }

        inner class ViewHolder(val title: TextView, val progress: TextView, val selectIcon: View, val cover: ImageView)
    }
}