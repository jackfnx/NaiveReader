package sixue.naviereader;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import sixue.naviereader.data.Book;

public class Utils {
    public static final String ACTION_DOWNLOAD_CONTENT_FINISH = "ACTION_DOWNLOAD_CONTENT_FINISH";
    public static final String ACTION_DOWNLOAD_CHAPTER_FINISH = "ACTION_DOWNLOAD_CHAPTER_FINISH";
    public static final String ACTION_DOWNLOAD_COVER_FINISH = "ACTION_DOWNLOAD_COVER_FINISH";
    public static final String INTENT_PARA_BOOK_ID = "INTENT_PARA_BOOK_ID";
    public static final String INTENT_PARA_CHAPTER_ID = "INTENT_PARA_CHAPTER_ID";
    public static final String INTENT_PARA_NEXT_ACTION_READ = "INTENT_PARA_NEXT_ACTION_READ";
    public static final String INTENT_PARA_NEXT_ACTION_CONTENT = "INTENT_PARA_NEXT_ACTION_CONTENT";
    public static final String INTENT_PARA_PATH = "INTENT_PARA_PATH";
    public static final String INTENT_PARA_CURRENT_POSITION = "INTENT_PARA_CURRENT_POSITION";
    public static final String INTENT_PARA_CHAPTER_INDEX = "INTENT_PARA_CHAPTER_INDEX";

    public static String readText(String s) {
        File file = new File(s);
        if (!file.exists()) {
            return null;
        }

        String encoding = guessFileEncoding(file);
        if (encoding == null) {
            return null;
        }

        try {
            InputStream is = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(is, encoding);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            is.close();
            return sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeText(String s, String path) {
        try {
            File file = new File(path);
            File dir = file.getParentFile();

            if (!dir.exists()) {
                boolean mk = dir.mkdirs();
                Log.i("Utils", "mkdir:" + dir + ", " + mk);
            }

            if (!file.exists()) {
                boolean cr = file.createNewFile();
                Log.i("Utils", "createNewFile:" + file + ", " + cr);
            }

            FileWriter fw = new FileWriter(file, false);
            fw.write(s);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String guessFileEncoding(File file) {
        try {
            InputStream is = new FileInputStream(file);
            UniversalDetector detector = new UniversalDetector(null);
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, n);
            }
            detector.dataEnd();
            String encoding = detector.getDetectedCharset();
            detector.reset();
            return encoding;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void verifyPermissions(Activity activity) {
        int permission1 = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
    }

    public static Book createBook(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".txt")) {
            name = name.substring(0, name.length() - 4);
        }

        String title;
        int l1 = name.indexOf("【");
        int r1 = name.indexOf("】");
        if (l1 != -1 && r1 != -1 && l1 < r1) {
            title = name.substring(l1 + 1, r1);

        } else {
            title = name;
        }

        String author;
        int a1 = name.indexOf("作者：");
        if (a1 != -1) {
            author = name.substring(a1 + "作者：".length());
        } else {
            author = "*";
        }

        Book book = new Book();
        book.setId(file.getAbsolutePath());
        book.setTitle(title);
        book.setAuthor(author);
        book.setLocal(true);
        book.setLocalPath(file.getAbsolutePath());
        return book;
    }

    public static Bitmap createCropBitmap(Bitmap unscaledBitmap, int dstWidth, int dstHeight) {
        Rect srcRect = calcSrcRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(), dstWidth, dstHeight);
        Rect dstRect = new Rect(0, 0, dstWidth, dstHeight);
        Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));
        return scaledBitmap;
    }

    private static Rect calcSrcRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        final float srcAspect = (float) srcWidth / (float) srcHeight;
        final float dstAspect = (float) dstWidth / (float) dstHeight;
        if (srcAspect > dstAspect) {
            final int srcRectWidth = (int) (srcHeight * dstAspect);
            final int srcRectLeft = (srcWidth - srcRectWidth) / 2;
            return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
        } else {
            final int srcRectHeight = (int) (srcWidth / dstAspect);
            final int scrRectTop = (srcHeight - srcRectHeight) / 2;
            return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
        }
    }
}
