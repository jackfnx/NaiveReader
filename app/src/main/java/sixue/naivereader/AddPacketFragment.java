package sixue.naivereader;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Packet;


public class AddPacketFragment extends Fragment {

    private static final String TAG = AddPacketFragment.class.getSimpleName();
    private ListView listView;
    private View loadingProgress;
    private View offline;
    private MyAdapter adapter;

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
        adapter = new MyAdapter();
        listView.setAdapter(adapter);
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
        private List<Packet> packets;
        private Thread clientThread;

        MyAdapter() {
            this.packets = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return packets.size();
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

            Packet packet = packets.get(position);

            TextView title = convertView.findViewById(R.id.title);
            TextView author = convertView.findViewById(R.id.author);
            TextView status = convertView.findViewById(R.id.status);
            title.setText(packet.getTitle());
            author.setText(packet.getAuthor());
            status.setText(packet.getKey());
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
                        packets = PacketLoader.loadPackets(ip);
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
                        packets = new ArrayList<>();
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
