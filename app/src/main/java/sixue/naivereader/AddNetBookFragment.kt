package sixue.naivereader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import sixue.naivereader.data.Book
import sixue.naivereader.provider.NetProviderCollections.getProviders
import kotlin.collections.ArrayList

class AddNetBookFragment : Fragment() {
    private var list: MutableList<Book> = ArrayList()
    private lateinit var receiver: BroadcastReceiver
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_add_net_book, container, false)
        val search = v.findViewById<Button>(R.id.search)
        val searchText = v.findViewById<EditText>(R.id.search_text)
        val listBooks = v.findViewById<ListView>(R.id.list_books)
        val myAdapter: BaseAdapter = object : BaseAdapter() {
            override fun getCount(): Int {
                return list.size
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
                    view = inflater.inflate(R.layout.listviewitem_book, viewGroup, false)
                    val cover = view.findViewById<ImageView>(R.id.cover)
                    val title = view.findViewById<TextView>(R.id.title)
                    val author = view.findViewById<TextView>(R.id.author)
                    val source = view.findViewById<TextView>(R.id.source)
                    view.tag = ViewHolder(cover, title, author, source)
                } else {
                    view = convertView
                }
                val viewHolder: ViewHolder = (view!!.tag as ViewHolder)
                val book = list[i]
                viewHolder.cover.setImageBitmap(book.buildHelper().loadCoverBitmap(requireContext()))
                viewHolder.title.text = book.title
                viewHolder.author.text = book.author
                viewHolder.source.text = getString(R.string.sources, book.sources.size)
                return view!!
            }

            inner class ViewHolder(
                val cover: ImageView,
                val title: TextView,
                val author: TextView,
                val source: TextView
            )
        }
        search.setOnClickListener {
            list.clear()
            for (provider in getProviders(requireContext())) {
                if (!provider.isActive) {
                    continue
                }
                Thread {
                    val books = provider.search(searchText.text.toString(), requireContext())
                    for (book in books) {
                        val b = findSameBook(book)
                        if (b != null) {
                            b.sources.toMutableList().addAll(book.sources)
                        } else {
                            list.add(book)
                        }
                    }
                    val activity = activity
                    activity?.runOnUiThread { myAdapter.notifyDataSetChanged() }
                    Log.i(TAG, list.toString())
                }.start()
            }
        }
        listBooks.adapter = myAdapter
        listBooks.onItemClickListener = OnItemClickListener { _, _, i, _ ->
            val book = list[i]
            val b = BookLoader.findBook(book.id)
            if (b != null) {
                BookLoader.bookBubble(b)
            } else {
                BookLoader.addBook(book)
            }
            val activity = activity
            activity?.finish()
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val bookId = intent.getStringExtra(Utils.INTENT_PARA_BOOK_ID)
                for (book in list) {
                    if (book.id == bookId) {
                        myAdapter.notifyDataSetChanged()
                        break
                    }
                }
            }
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(Utils.ACTION_DOWNLOAD_COVER_FINISH)
        requireContext().registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(receiver)
    }

    private fun findSameBook(book: Book): Book? {
        for (b in list) {
            if (book.title == b.title) {
                return b
            }
        }
        return null
    }

    companion object {
        private val TAG = AddNetBookFragment::class.java.simpleName
    }
}