package sixue.naivereader.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.Utils;
import sixue.naivereader.data.Book;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.data.Packet;

public class PacketHelper implements BookHelper {

    private static final String TAG = PacketHelper.class.getSimpleName();
    private final Book book;
    private Bitmap cover;

    public PacketHelper(Book book) {
        this.book = book;
    }

    @Override
    public boolean reloadContent(Context context) {

        String bookSavePath = calcPacketSavePath(context);
        String json = Utils.readTextFromZip(bookSavePath, ".CONTENT");
        if (json == null) {
            return false;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JavaType listType = mapper.getTypeFactory().constructParametricType(ArrayList.class, Chapter.class);
            List<Chapter> list = mapper.readValue(json, listType);
            book.setChapterList(list);
            return list.size() > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String calcPacketSavePath(Context context) {
        String saveRootPath = Utils.getSavePathRoot(context);
        return saveRootPath + "/packets/" + book.getId() + ".zip";
    }

    private void ensurePacketSavePath(Context context) {
        String saveRootPath = Utils.getSavePathRoot(context);
        String packetsRootPath = saveRootPath + "/packets";
        File dir = new File(packetsRootPath);
        if (!dir.exists()) {
            boolean mk = dir.mkdirs();
            Log.i(TAG, "mkdir:" + dir + ", " + mk);
        }
    }

    @Override
    public void downloadContent(Context context) {

    }

    public void downloadPacket(final Context context, final String ip, final Func<String> callback) {
        ensurePacketSavePath(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String savePath = calcPacketSavePath(context);
                PacketLoader.downloadPacket(ip, savePath, "/book/" + book.getId());
                reloadContent(context);
                book.setLocalPath(savePath);
                book.setCurrentChapterIndex(book.getChapterList().size() - 1);
                callback.exec(savePath);
            }
        }).start();
    }

    @Override
    public Bitmap loadCoverBitmap(Context context) {
        if (cover == null) {
            String bookSavePath = calcPacketSavePath(context);
            byte[] coverBytes = Utils.readBytesFromZip(bookSavePath, "cover.jpg");

            if (coverBytes == null) {
                cover = Utils.getAutoCover(context, book.getTitle());
            } else {
                cover = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.length);
            }
        }
        return cover;
    }

    public Packet loadMetaData(Context context) {

        String bookSavePath = calcPacketSavePath(context);
        String json = Utils.readTextFromZip(bookSavePath, ".META.json");
        if (json == null) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, Packet.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public interface Func<T> {
        void exec(T t);
    }
}
