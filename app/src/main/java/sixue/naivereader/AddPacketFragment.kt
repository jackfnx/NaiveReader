package sixue.naivereader

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import sixue.naivereader.data.Packet
import sixue.naivereader.helper.PacketHelper
import sixue.naivereader.helper.PacketLoader.createBook
import sixue.naivereader.helper.PacketLoader.loadPackets
import kotlin.collections.ArrayList

class AddPacketFragment : Fragment() {
    private lateinit var listView: ListView
    private lateinit var loadingProgress: View
    private lateinit var offline: View
    private var adapter: MyAdapter = MyAdapter()
    private var list: List<Packet> = ArrayList()
    private var ip: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_add_packet, container, false)
        loadingProgress = v.findViewById(R.id.loading)
        offline = v.findViewById(R.id.offline)
        listView = v.findViewById(R.id.list_packets)
        listView.adapter = adapter
        listView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val packet = list[position]
            val b = BookLoader.findBook(packet.key)
            if (b != null) {
                val helper = b.buildHelper() as PacketHelper
                helper.reloadContent(requireContext())
                val read = b.chapterList.size - 1 - b.currentChapterIndex
                val currentPacket = helper.loadMetaData(requireContext())
                if (currentPacket == null || currentPacket.summary != packet.summary) {
                    helper.downloadPacket(requireActivity(), ip!!, object : PacketHelper.Func<String> {
                        override fun exec(t: String) {
                            var idx = b.chapterList.size - 1 - read
                            if (idx < 0) {
                                idx = 0
                            }
                            if (idx >= b.chapterList.size) {
                                idx = b.chapterList.size - 1
                            }
                            b.currentChapterIndex = idx
                            BookLoader.bookBubble(b)
                            val activity: Activity? = activity
                            activity?.runOnUiThread {
                                Toast.makeText(activity, R.string.msg_packet_download, Toast.LENGTH_SHORT).show()
                                adapter.notifyDataSetChanged()
                            }
                        }
                    })
                }
            } else {
                val book = createBook(packet)
                val helper = book.buildHelper() as PacketHelper
                helper.downloadPacket(requireActivity(), ip!!, object : PacketHelper.Func<String> {
                    override fun exec(t: String) {
                        BookLoader.addBook(book)
                        val activity: Activity? = activity
                        activity?.runOnUiThread {
                            Toast.makeText(activity, R.string.msg_packet_download, Toast.LENGTH_SHORT).show()
                            adapter.notifyDataSetChanged()
                        }
                    }
                })
            }
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        adapter.startLoading()
    }

    override fun onPause() {
        super.onPause()
        adapter.stopLoading()
    }

    private inner class MyAdapter : BaseAdapter() {
        private var clientThread: Thread? = null
        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Any {
            return ""
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View?
            if (convertView == null) {
                view = LayoutInflater.from(activity)
                    .inflate(R.layout.listviewitem_packet, parent, false)
                val title = view.findViewById<TextView>(R.id.title)
                val author = view.findViewById<TextView>(R.id.author)
                val time = view.findViewById<TextView>(R.id.timestamp)
                val status = view.findViewById<TextView>(R.id.status)
                view.tag = ViewHolder(title, author, time, status)
            } else {
                view = convertView
            }
            val viewHolder: ViewHolder = view!!.tag as ViewHolder
            val packet = list[position]
            viewHolder.title.text = packet.title
            viewHolder.author.text = packet.author
            viewHolder.time.text = Utils.fmtTimestamp(packet.timestamp)
            val b = BookLoader.findBook(packet.key)
            if (b == null) {
                viewHolder.status.setText(R.string.not_download)
                viewHolder.status.setTextAppearance(R.style.SecondaryText)
            } else {
                val helper = b.buildHelper() as PacketHelper
                val currentPacket = helper.loadMetaData(requireContext())
                if (currentPacket?.summary == packet.summary) {
                    viewHolder.status.setText(R.string.no_changes)
                    viewHolder.status.setTextAppearance(R.style.PeaceText)
                } else {
                    viewHolder.status.setText(R.string.new_changes)
                    viewHolder.status.setTextAppearance(R.style.EmphasizeText)
                }
            }
            return view
        }

        inner class ViewHolder(
            val title: TextView,
            val author: TextView,
            val time: TextView,
            val status: TextView
        )

        fun startLoading() {
            loadingProgress.visibility = View.VISIBLE
            listView.visibility = View.INVISIBLE
            offline.visibility = View.INVISIBLE
            clientThread = Thread {
                ip = ScanDeviceTool.scan()
                val activity: Activity? = activity
                if (activity != null) {
                    if (ip != null) {
                        list = loadPackets(ip!!)
                        activity.runOnUiThread(Runnable {
                            notifyDataSetChanged()
                            loadingProgress.visibility = View.INVISIBLE
                            listView.visibility = View.VISIBLE
                            offline.visibility = View.INVISIBLE
                        })
                    } else {
                        list.toMutableList().clear()
                        activity.runOnUiThread(Runnable {
                            notifyDataSetChanged()
                            loadingProgress.visibility = View.INVISIBLE
                            listView.visibility = View.INVISIBLE
                            offline.visibility = View.VISIBLE
                        })
                    }
                }
            }
            clientThread?.start()
        }

        fun stopLoading() {
            if (clientThread?.isAlive == true) {
                Log.i(TAG, "STOP Loading: interrupt thread.")
                clientThread!!.interrupt()
                clientThread = null
            }
            ip = null
//            loadingProgress.setVisibility(View.INVISIBLE);
//            listView.setVisibility(View.INVISIBLE);
//            offline.setVisibility(View.VISIBLE);
        }
    }

    companion object {
        private val TAG = AddPacketFragment::class.java.simpleName
    }
}