package sixue.naivereader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Book;
import sixue.naivereader.data.Packet;
import sixue.naivereader.helper.PacketHelper;
import sixue.naivereader.helper.PacketLoader;


public class AddPacketFragment extends Fragment {

    private static final String TAG = AddPacketFragment.class.getSimpleName();
    private ListView listView;
    private View loadingProgress;
    private View offline;
    private MyAdapter adapter;
    private List<Packet> list;
    private String ip;

    public AddPacketFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_add_packet, container, false);
        loadingProgress = v.findViewById(R.id.loading);
        offline = v.findViewById(R.id.offline);
        listView = v.findViewById(R.id.list_packets);
        list = new ArrayList<>();
        adapter = new MyAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final Packet packet = list.get(position);
                final Book b = BookLoader.getInstance().findBook(packet.getKey());
                if (b != null) {
                    PacketHelper helper = (PacketHelper) b.buildHelper();
                    helper.reloadContent(getContext());
                    final int read = b.getChapterList().size() - 1 - b.getCurrentChapterIndex();
                    Packet currentPacket = helper.loadMetaData(getContext());
                    if (!currentPacket.getSummary().equals(packet.getSummary())) {
                        helper.downloadPacket(getActivity(), ip, new PacketHelper.Func<String>() {
                            @Override
                            public void exec(final String savePath) {

                                int idx = b.getChapterList().size() - 1 - read;
                                if (idx < 0)
                                {
                                    idx = 0;
                                }
                                if (idx >= b.getChapterList().size())
                                {
                                    idx = b.getChapterList().size() - 1;
                                }
                                b.setCurrentChapterIndex(idx);
                                BookLoader.getInstance().bookBubble(b);
                                final Activity activity = getActivity();
                                if (activity != null) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(activity, R.string.msg_packet_download, Toast.LENGTH_SHORT).show();
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                                }
                            }
                        });
                    }
                } else {
                    final Book book = PacketLoader.createBook(packet);
                    final PacketHelper helper = (PacketHelper) book.buildHelper();
                    helper.downloadPacket(getActivity(), ip, new PacketHelper.Func<String>() {
                        @Override
                        public void exec(final String savePath) {

                            BookLoader.getInstance().addBook(book);
                            final Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(activity, R.string.msg_packet_download, Toast.LENGTH_SHORT).show();
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    });
                }

            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.startLoading();
    }

    @Override
    public void onPause() {
        super.onPause();
        adapter.stopLoading();
    }

    private class MyAdapter extends BaseAdapter {
        private Thread clientThread;

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.listviewitem_packet, parent, false);
            }

            Packet packet = list.get(position);

            TextView title = convertView.findViewById(R.id.title);
            TextView author = convertView.findViewById(R.id.author);
            TextView time = convertView.findViewById(R.id.timestamp);
            TextView status = convertView.findViewById(R.id.status);
            title.setText(packet.getTitle());
            author.setText(packet.getAuthor());
            time.setText(Utils.fmtTimestamp(packet.getTimestamp()));
            Book b = BookLoader.getInstance().findBook(packet.getKey());
            if (b == null) {
                status.setText(R.string.not_download);
                status.setTextAppearance(R.style.SecondaryText);
            } else {
                PacketHelper helper = (PacketHelper) b.buildHelper();
                Packet currentPacket = helper.loadMetaData(getContext());
                if (currentPacket.getSummary().equals(packet.getSummary())) {
                    status.setText(R.string.no_changes);
                    status.setTextAppearance(R.style.PeaceText);
                } else {
                    status.setText(R.string.new_changes);
                    status.setTextAppearance(R.style.EmphasizeText);
                }
            }
            return convertView;
        }

        void startLoading() {
            loadingProgress.setVisibility(View.VISIBLE);
            listView.setVisibility(View.INVISIBLE);
            offline.setVisibility(View.INVISIBLE);

            clientThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ip = ScanDeviceTool.scan();
                    Activity activity = getActivity();
                    if (activity != null) {
                        if (ip != null) {
                            list = PacketLoader.loadPackets(ip);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                    loadingProgress.setVisibility(View.INVISIBLE);
                                    listView.setVisibility(View.VISIBLE);
                                    offline.setVisibility(View.INVISIBLE);
                                }
                            });
                        } else {
                            list.clear();
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                    loadingProgress.setVisibility(View.INVISIBLE);
                                    listView.setVisibility(View.INVISIBLE);
                                    offline.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                }
            });
            clientThread.start();
        }

        void stopLoading() {
            if (clientThread != null && clientThread.isAlive()) {
                Log.i(TAG, "STOP Loading: interrupt thread.");
                clientThread.interrupt();
                clientThread = null;
            }
            ip = null;
//            loadingProgress.setVisibility(View.INVISIBLE);
//            listView.setVisibility(View.INVISIBLE);
//            offline.setVisibility(View.VISIBLE);
        }
    }
}
