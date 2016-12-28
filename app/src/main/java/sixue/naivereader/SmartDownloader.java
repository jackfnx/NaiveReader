package sixue.naivereader;

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

import sixue.naivereader.data.Book;
import sixue.naivereader.data.Chapter;
import sixue.naivereader.provider.NetProvider;
import sixue.naivereader.provider.NetProviderCollections;

public class SmartDownloader {
    private final Book book;
    private final Context context;

    public SmartDownloader(Context context, Book book) {
        this.context = context;
        this.book = book;
    }

    public boolean reloadContent() {
        if (book.isLocal()) {
            return true;
        }

        String bookSavePath = calcBookSavePath(book);
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

    private void downloadContent() {
        try {
            String bookSavePath = calcBookSavePath(book);

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

    public boolean isDownloaded(Chapter chapter) {
        File file = new File(chapter.getSavePath());
        return file.exists();
    }

    private void downloadChapter(Chapter chapter) {
        NetProvider provider = NetProviderCollections.findProviders(book.getSiteId());
        provider.downloadChapter(book, chapter);

        Intent intent = new Intent(Utils.ACTION_DOWNLOAD_CHAPTER_FINISH);
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
        intent.putExtra(Utils.INTENT_PARA_CHAPTER_ID, chapter.getId());
        intent.putExtra(Utils.INTENT_PARA_PATH, chapter.getSavePath());
        context.sendBroadcast(intent);
    }

    private String calcBookSavePath(Book book) {
        File fileDir = context.getExternalFilesDir("books");
        if (fileDir == null) {
            return "";
        }

        return fileDir.getAbsolutePath() + "/" + book.getId() + "/" + book.getSiteId();
    }

    public void startDownloadContent() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadContent();
            }
        }).start();
    }

    public void startDownloadChapter(final Chapter chapter) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadChapter(chapter);
            }
        }).start();
    }

    public void startDownloadCover(final String coverUrl) {
        String bookSavePath = calcBookSavePath(book);
        File dir = new File(bookSavePath);
        if (!dir.exists()) {
            boolean mk = dir.mkdirs();
            Log.i(getClass().toString(), "mkdir:" + dir + ", " + mk);
        }

        final String coverSavePath = bookSavePath + "/cover.jpg";
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadCover(coverUrl, coverSavePath);
            }
        }).start();
    }

    private void downloadCover(String coverUrl, String coverSavePath) {
        try {
            if ((new File(coverSavePath)).exists()) {
                book.setCoverSavePath(coverSavePath);
                return;
            }

            Log.i(getClass().toString(), "cover:[" + coverUrl + "]=>[" + coverSavePath + "] startDownload.");
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
            Log.i(getClass().toString(), "cover:[" + coverUrl + "]=>[" + coverSavePath + "] download finished.");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        book.setCoverSavePath(coverSavePath);

        Intent intent = new Intent(Utils.ACTION_DOWNLOAD_COVER_FINISH);
        intent.putExtra(Utils.INTENT_PARA_BOOK_ID, book.getId());
        context.sendBroadcast(intent);
    }

    public boolean coverIsDownloaded() {
        String coverSavePath = book.getCoverSavePath();
        if (coverSavePath.length() == 0) {
            return false;
        }

        File f = new File(coverSavePath);
        return f.exists();
    }

    public Bitmap getNoCoverBitmap() {
        try {
            InputStream is = context.getAssets().open("NoCover.jpg");
            Bitmap noCover = BitmapFactory.decodeStream(is);
            is.close();
            return noCover;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
