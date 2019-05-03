package sixue.naivereader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static final String ACTION_DOWNLOAD_CONTENT_FINISH = "ACTION_DOWNLOAD_CONTENT_FINISH";
    public static final String ACTION_DOWNLOAD_CHAPTER_FINISH = "ACTION_DOWNLOAD_CHAPTER_FINISH";
    public static final String ACTION_DOWNLOAD_ALL_CHAPTER_FINISH = "ACTION_DOWNLOAD_ALL_CHAPTER_FINISH";
    public static final String ACTION_DOWNLOAD_COVER_FINISH = "ACTION_DOWNLOAD_COVER_FINISH";
    public static final String INTENT_PARA_BOOK_ID = "INTENT_PARA_BOOK_ID";
    public static final String INTENT_PARA_CHAPTER_ID = "INTENT_PARA_CHAPTER_ID";
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
            encoding = "utf-8";
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
        int r = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (r != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }

        int w = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (w != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
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

    public static <T> String[] convert(List<T> list, Func<T> func) {
        String[] arr = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = func.exec(list.get(i));
        }
        return arr;
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        boolean dr = file.delete();
        Log.i(TAG, "delete:" + path + ", " + dr);
    }

    public static void mkdir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean mk = dir.mkdirs();
            Log.i(TAG, "mkdir:" + dir + ", " + mk);
        }

    }

    public static boolean exists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static String getSavePathRoot(Context context) {
        String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
        return sdcard + "/" + context.getPackageName();
    }

    public static void deleteDirectory(File file) {
        boolean mk = false;
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    mk |= f.delete();
                }
            }
        }
        mk |= file.delete();
        Log.i(TAG, "deleteDirectory:" + file.toString() + ", " + mk);
    }

    private static Bitmap paintCover(Bitmap blank, String title) {
        int MIN_TEXT_SIZE = 12;
        int MAX_TEXT_SIZE = 40;
        List<String> lines = explodeBySpecialChar(title);
        String maxLine = Collections.max(lines, new Comparator<String>() {
            @Override
            public int compare(String s, String t1) { return s.length() - t1.length(); }
        });
        Canvas canvas = new Canvas(blank);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setDither(true);
        paint.setFilterBitmap(true);
        paint.setColor(Color.BLACK);
        paint.setShadowLayer(1f, 0f, 1f, Color.LTGRAY);
        for (int i = MIN_TEXT_SIZE; i < MAX_TEXT_SIZE; i++) {
            paint.setTextSize(i);
            if ((blank.getWidth() - paint.measureText(maxLine)) < 10) {
                break;
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Rect bounds = new Rect();
            paint.getTextBounds(line, 0, line.length(), bounds);
            int x = (blank.getWidth() - bounds.width()) / 2;
            int y = (blank.getHeight() + bounds.height()) / 4 + i * bounds.height();
            canvas.drawText(line, x, y, paint);
        }
        return  blank;
    }

    private static List<String> explodeBySpecialChar(String title) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (char c : title.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(c);
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                sb.append(c);
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                sb.append(c);
                continue;
            }
            Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
            if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                sb.append(c);
                continue;
            } else if (ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                sb.append(c);
                continue;
            } else if (ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) {
                sb.append(c);
                continue;
            } else if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                sb.append(c);
                continue;
            } else if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                sb.append(c);
                continue;
            } else if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C) {
                sb.append(c);
                continue;
            } else if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D) {
                sb.append(c);
                continue;
            } else if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
                sb.append(c);
                continue;
            } else if (ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
                sb.append(c);
                continue;
            }
            list.add(sb.toString());
            sb = new StringBuilder();
        }
        if (sb.length() != 0)
            list.add(sb.toString());
        return list;
    }

    private static Bitmap getBlankCoverBitmap(Context context) {
        try {
            int WIDTH = 160;
            int HEIGHT = 200;
            InputStream is = context.getAssets().open("texture_paper.jpg");
            Bitmap texture = BitmapFactory.decodeStream(is);
            int x = (int)(Math.random() * (texture.getWidth() - WIDTH));
            int y = (int)(Math.random() * (texture.getHeight() - HEIGHT));
            Bitmap blank = Bitmap.createBitmap(texture, x, y, WIDTH, HEIGHT);
            is.close();
            return blank;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getAutoCover(Context context, String title) {
        String saveRoot = getSavePathRoot(context);
        String autoCoverRoot = saveRoot + "/AutoCover/";
        String autoCoverPath = autoCoverRoot + title + ".jpg";
        if (exists(autoCoverPath)) {
            return BitmapFactory.decodeFile(autoCoverPath);
        } else {
            Bitmap blank = getBlankCoverBitmap(context);
            Bitmap cover = paintCover(blank, title);
            try {
                mkdir(autoCoverRoot);
                OutputStream os = new FileOutputStream(autoCoverPath);
                cover.compress(Bitmap.CompressFormat.JPEG, 80, os);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return cover;
        }
    }

    public static String readTextFromZip(String zipPath, String path) {

        try {
            ZipFile zf = new ZipFile(zipPath);
            InputStream in = new BufferedInputStream(new FileInputStream(zipPath));
            ZipInputStream zin = new ZipInputStream(in);
            for (ZipEntry ze; (ze = zin.getNextEntry()) != null; ) {
                if (!ze.isDirectory()) {
                    if (ze.getName().equals(path)) {
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(zf.getInputStream(ze)))) {
                            StringBuilder text = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                text.append(line);
                                text.append("\n");
                            }
                            return text.toString();
                        }
                    }
                }
            }
            zin.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface Func<T> {
        String exec(T t);
    }
}
