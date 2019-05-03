package sixue.naivereader.helper;

import android.content.Context;

import sixue.naivereader.data.Book;

public class LocalTextHelper implements BookHelper {

    private final Book book;

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
}
