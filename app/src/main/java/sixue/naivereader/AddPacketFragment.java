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
import android.widget.Toast;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import sixue.naivereader.data.Packet;


public class AddPacketFragment extends Fragment {

    private static final int UDP_PORT = 4999;
    private static final int HTTP_PORT = 5000;
    private static final String CODES_WORD = "Naive Reader Pack Server RUNNING.";
    private static final String TAG = "AddPacketFragment";
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
                convertView = getActivity().getLayoutInflater().inflate(R.layout.listviewitem_packet, parent, false);
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
                    try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
                        socket.setReuseAddress(true);
                        socket.setSoTimeout(3000);
                        String serverAddress = "";
                        for (int i = 0; i < 3; i++) {
                            try {
                                Log.i(TAG, "Trying to find UDP server, " + (i+1) + " times.");
                                byte[] buffer = new byte[1024];
                                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                                socket.receive(dp);
                                byte[] data = dp.getData();
                                byte[] bytes = Arrays.copyOfRange(data, 1, data[0] + 1);
                                String msg = new String(bytes, StandardCharsets.UTF_8);
                                Log.i(TAG, "UDP MSG:" + msg);
                                if (msg.equals(CODES_WORD)) {
                                    serverAddress = dp.getAddress().getHostAddress();
                                    break;
                                }
                            } catch (Exception e) {
                                Log.i(TAG, "UDP ERROR:");
                                e.printStackTrace();
                            }
                        }

                        if (serverAddress.length() == 0) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), getString(R.string.server_not_found), Toast.LENGTH_SHORT).show();
                                    loadingProgress.setVisibility(View.INVISIBLE);
                                    listView.setVisibility(View.INVISIBLE);
                                    offline.setVisibility(View.VISIBLE);
                                }
                            });
                            Log.i(TAG, "Server not found.");
                            return;
                        }

                        URL url = new URL(String.format(Locale.PRC, "http://%s:%d/books", serverAddress, HTTP_PORT));
                        Log.i(TAG, "GET:" + url);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("Connection", "close");
                        conn.setDoInput(true);
                        conn.connect();
                        int c = conn.getResponseCode();
                        if (c != HttpURLConnection.HTTP_OK) {
                            Log.i(TAG, "HTTP Error:" + c);
                            return;
                        }
                        InputStream is = conn.getInputStream();

                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader br = new BufferedReader(isr);
                        String line;
                        StringBuilder buf = new StringBuilder();
                        while ((line = br.readLine()) != null) {
                            buf.append(line);
                        }
                        String json = buf.toString();
                        ObjectMapper mapper = new ObjectMapper();
                        JavaType listType = mapper.getTypeFactory().constructParametricType(ArrayList.class, Packet.class);
                        packets = mapper.readValue(json, listType);
                        Log.i(TAG, "GET JSON: " + packets.size() + " packets.");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notifyDataSetChanged();
                                loadingProgress.setVisibility(View.INVISIBLE);
                                listView.setVisibility(View.VISIBLE);
                                offline.setVisibility(View.INVISIBLE);
                            }
                        });
                    } catch (Exception e) {
                        Log.i(TAG, "OTHER Error:");
                        e.printStackTrace();
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
        }
    }
}
