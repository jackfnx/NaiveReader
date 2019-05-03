package sixue.naivereader;

import android.util.Log;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import sixue.naivereader.data.Book;
import sixue.naivereader.data.BookKind;
import sixue.naivereader.data.Packet;
import sixue.naivereader.helper.PacketHelper;

public class PacketLoader {
    private static final String TAG = PacketLoader.class.getSimpleName();

    public static final int HTTP_PORT = 5000;
    public static final String INIT_URL = "/books";

    private static PacketLoader instance;

    private String ip;

    public static PacketLoader getInstance() {
        if (instance == null) {
            instance = new PacketLoader();
        }
        return instance;
    }

    private PacketLoader() {

    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<Book> loadPackets() {

        List<Book> list = new ArrayList<>();
        try {
            URL url = new URL(String.format(Locale.PRC, "http://%s:%d%s", ip, HTTP_PORT, INIT_URL));
            Log.i(TAG, "GET:" + url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Connection", "close");
            conn.setDoInput(true);
            conn.connect();
            int c = conn.getResponseCode();
            if (c != HttpURLConnection.HTTP_OK) {
                Log.i(TAG, "HTTP Error:" + c);
                return list;
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
            List<Packet> packets = mapper.readValue(json, listType);
            Log.i(TAG, "GET JSON: " + packets.size() + " packets.");
            for (Packet packet : packets) {
                Book book = new Book();
                book.setId(packet.getKey());
                book.setTitle(packet.getTitle());
                book.setAuthor(packet.getAuthor());
                book.setKind(BookKind.Packet);
                list.add(book);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void downloadPacket(String packetUrl, String savePath) {

        try {
            URL url = new URL(String.format(Locale.PRC, "http://%s:%d%s", ip, HTTP_PORT, packetUrl));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }
            try (InputStream inputStream = conn.getInputStream()) {
                byte[] buffer = new byte[1024];
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    for (int len; (len = inputStream.read(buffer)) != -1; ) {
                        bos.write(buffer, 0, len);
                    }
                    byte[] getData = bos.toByteArray();
                    File file = new File(savePath);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(getData);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
