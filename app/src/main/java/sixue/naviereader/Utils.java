package sixue.naviereader;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
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

public class Utils {
    public static final String PREFERENCE_BOOK_LIST = "PREFERENCE_BOOK_LIST";
    static final String INTENT_PARA_POSITION = "INTENT_PARA_POSITION";
    static final String INTENT_PARA_BOOKNAME = "INTENT_PARA_BOOKNAME";
    static final String INTENT_PARA_BOOKPATH = "INTENT_PARA_BOOKPATH";
    public static final String INTENT_PARA_NEXT_ACTION = "INTENT_PARA_NEXT_ACTION";
    public static final String ACTION_CHAPTER_CHANGED = "ACTION_CHAPTER_CHANGED";
    public static final String ACTION_DOWNLOAD_CONTENT_FINISH = "ACTION_DOWNLOAD_CONTENT_FINISH";
    public static final String ACTION_DOWNLOAD_CHAPTER_FINISH = "ACTION_DOWNLOAD_CHAPTER_FINISH";
    public static final String INTENT_PARA_BOOK_ID = "INTENT_PARA_BOOK_ID";
    public static final String INTENT_PARA_CHAPTER_ID = "INTENT_PARA_CHAPTER_ID";
    public static final String INTENT_PARA_NEXT_ACTION_READ = "INTENT_PARA_NEXT_ACTION_READ";
    public static final String INTENT_PARA_NEXT_ACTION_CONTENT = "INTENT_PARA_NEXT_ACTION_CONTENT";
    public static final String INTENT_PARA_PATH = "INTENT_PARA_PATH";
    public static final String INTENT_PARA_CURRENT_POSITION = "INTENT_PARA_CURRENT_POSITION";
    public static final String INTENT_PARA_CHAPTER_INDEX = "INTENT_PARA_CHAPTER_INDEX";

    public static String readText(String s) {
        File file = new File(s);
        try {
            String encoding = guessFileEncoding(file);
            if (encoding == null) {
                return null;
            }

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
                boolean mk = dir.mkdir();
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

    private static String guessFileEncoding(File file) throws IOException {
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
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * Checks if the app has permission to write to device storage
     * <p/>
     * If the app does not has permission then the user will be prompted to
     * grant permissions
     *
     * @param activity context activity
     */
    static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }
}
