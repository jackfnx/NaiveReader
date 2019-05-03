package sixue.naivereader.helper;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

import sixue.naivereader.PacketLoader;
import sixue.naivereader.Utils;
import sixue.naivereader.data.Book;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.data.PackChapter;
import sixue.naivereader.data.Packet;

public class PacketHelper implements BookHelper {

    private static final String TAG = PacketHelper.class.getSimpleName();
    private final Book book;

    public PacketHelper(Book book) {
        this.book = book;
    }

    @Override
    public boolean reloadContent(Context context) {

        String bookSavePath = calcPacketSavePath(context);
        String json = Utils.readTextFromZip(bookSavePath, ".META.json");
        if (json == null) {
            return false;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Packet packet = mapper.readValue(json, Packet.class);
            book.getChapterList().clear();
            for (PackChapter packChapter : packet.getChapters()) {
                Chapter chapter = new Chapter();
                chapter.setId(packChapter.getFilename());
                chapter.setTitle(packChapter.getTitle());
                chapter.setPara(packChapter.getSource());
                chapter.setSavePath(packChapter.getFilename());
                chapter.setTimestamp(packChapter.getTimestamp());
                book.getChapterList().add(chapter);
            }
            return book.getChapterList().size() > 0;
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

    public void downloadPacket(final Context context, final Func<String> callback) {
        ensurePacketSavePath(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String savePath = calcPacketSavePath(context);
                PacketLoader.getInstance().downloadPacket("/book/" + book.getId(), savePath);
                callback.exec(savePath);
            }
        }).start();
    }

    public interface Func<T> {
        void exec(T t);
    }
}
