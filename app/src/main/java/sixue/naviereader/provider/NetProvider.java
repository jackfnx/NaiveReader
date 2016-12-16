package sixue.naviereader.provider;

import android.content.Context;

import java.util.List;

import sixue.naviereader.data.Book;
import sixue.naviereader.data.Chapter;

public abstract class NetProvider {
    private boolean active = true;

    public abstract String getProviderId();

    public abstract String getProviderName();

    public abstract List<Book> search(String s, Context context);

    public abstract void downloadContent(Book book, String bookSavePath);

    public abstract void downloadChapter(Book book, Chapter chapter);

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
