package sixue.naivereader.helper;

import android.content.Context;
import android.graphics.Bitmap;

import sixue.naivereader.Utils;
import sixue.naivereader.data.Book;

public class LocalTextHelper implements BookHelper {

    private final Book book;
    private Bitmap cover;

    public LocalTextHelper(Book book) {
        this.book = book;
    }

    @Override
    public boolean reloadContent(Context context) {
        return true;
    }

    @Override
    public void downloadContent(Context context) {

    }

    @Override
    public Bitmap loadCoverBitmap(Context context) {
        if (cover == null) {
            cover = Utils.getAutoCover(context, book.getTitle(), book.getAuthor(), 1);
        }
        return cover;
    }
}
