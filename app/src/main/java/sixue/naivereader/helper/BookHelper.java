package sixue.naivereader.helper;

import android.content.Context;
import android.graphics.Bitmap;

public interface BookHelper {
    boolean reloadContent(Context context);

    void downloadContent(Context context);

    Bitmap loadCoverBitmap(Context context);
}
