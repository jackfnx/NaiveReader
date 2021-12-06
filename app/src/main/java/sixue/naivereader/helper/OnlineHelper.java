package sixue.naivereader.helper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.Utils;
import sixue.naivereader.data.Book;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.provider.NetProvider;
import sixue.naivereader.provider.NetProviderCollections;

public class OnlineHelper implements BookHelper {

    private final Book book;
    private Bitmap cover;

    public OnlineHelper(Book book) {
        this.book = book;
    }

    @Override
    public boolean reloadContent(Context context) {

        String bookSavePath = calcBookSavePath(context);
        String json = Utils.readText(bookSavePath + "/.CONTENT");
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

    @Override
    public void downloadContent(Context context) {

        try {
            String bookSavePath = calcBookSavePath(context);

            NetProvider provider = NetProviderCollections.findProviders(book.getSiteId());
            List<Chapter> content = provider.downloadContent(book, bookSavePath);

            if (content.size() > 0) {
                book.setChapterList(content);

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(content);
                Utils.writeText(json, bookSavePath + "/.CONTENT");
            }

            Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CONTENT_FINISH);
            intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
            context.sendBroadcast(intent);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Bitmap loadCoverBitmap(Context context) {
        if (cover == null) {
            String saveRootPath = Utils.getSavePathRoot(context);
            String coverSavePath = saveRootPath + "/" + book.getCoverSavePath();
            File f = new File(coverSavePath);

            if (book.getCoverSavePath().length() == 0 || !f.exists()) {
                cover = Utils.getAutoCover(context, book.getTitle(), book.getAuthor(), 2);
            } else {
                cover = BitmapFactory.decodeFile(coverSavePath);
            }
        }
        return cover;
    }

    private String calcBookSavePath(Context context) {
        return Utils.getSavePathRoot(context) + "/" + calcRelBookSavePath();
    }

    private String calcRelBookSavePath() {
        return "books/" + book.getId() + "/" + book.getSiteId();
    }

    public void downloadCover(Context context, String coverUrl) {

        String bookSavePath = calcBookSavePath(context);
        Utils.mkdir(bookSavePath);

        String bookRelSavePath = calcRelBookSavePath();
        String coverRelSavePath = bookRelSavePath + "/cover.jpg";
        String coverSavePath = bookSavePath + "/cover.jpg";

        try {
            if ((new File(coverSavePath)).exists()) {
                book.setCoverSavePath(coverRelSavePath);
                return;
            }

            Log.i(getClass().toString(), "cover:[" + coverUrl + "]=>[" + coverRelSavePath + "] startDownload.");
            URL url = new URL(coverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }
            InputStream is = conn.getInputStream();
            Bitmap original = BitmapFactory.decodeStream(is);
            Bitmap scaledBitmap = Utils.createCropBitmap(original, 160, 200);
            OutputStream os = new FileOutputStream(coverSavePath);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, os);
            os.close();
            is.close();
            Log.i(getClass().toString(), "cover:[" + coverUrl + "]=>[" + coverRelSavePath + "] download finished.");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        book.setCoverSavePath(coverRelSavePath);

        Intent intent = new Intent(Utils.ACTION_DOWNLOAD_COVER_FINISH);
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
        context.sendBroadcast(intent);
    }
}
