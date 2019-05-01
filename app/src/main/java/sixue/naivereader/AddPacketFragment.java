package sixue.naivereader;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Book;
import sixue.naivereader.data.Packet;


public class AddPacketFragment extends Fragment {

    private static final String TAG = AddPacketFragment.class.getSimpleName();
    private ListView listView;
    private View loadingProgress;
    private View offline;
    private MyAdapter adapter;
    private List<Book> list;

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

                Book book = list.get(position);

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

            Book book = list.get(position);

            TextView title = convertView.findViewById(R.id.title);
            TextView author = convertView.findViewById(R.id.author);
            TextView status = convertView.findViewById(R.id.status);
            title.setText(book.getTitle());
            author.setText(book.getAuthor());
            status.setText(book.getId());
            return convertView;
        }

        void startLoading() {
            loadingProgress.setVisibility(View.VISIBLE);
            listView.setVisibility(View.INVISIBLE);
            offline.setVisibility(View.INVISIBLE);

            clientThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    final String ip = ScanDeviceTool.scan();
                    if (ip != null) {
                        list = PacketLoader.loadPackets(ip);
                        getActivity().runOnUiThread(new Runnable() {
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
                        getActivity().runOnUiThread(new Runnable() {
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
            });
            clientThread.start();
        }

        void stopLoading() {
            if (clientThread != null && clientThread.isAlive()) {
                Log.i(TAG, "STOP Loading: interrupt thread.");
                clientThread.interrupt();
                clientThread = null;
            }
            loadingProgress.setVisibility(View.INVISIBLE);
            listView.setVisibility(View.INVISIBLE);
            offline.setVisibility(View.VISIBLE);
        }
    }
}
