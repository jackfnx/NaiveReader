package sixue.naviereader;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sixue.naviereader.data.Book;
import sixue.naviereader.data.Chapter;

public class BookLoader {
    private static BookLoader instance;
    private List<Book> list;
    private final List<Book> contentQueue;
    private final List<ChapterTask> chapterQueue;
    private String saveRootPath;

    private BookLoader() {
        list = new ArrayList<>();
        contentQueue = new ArrayList<>();
        chapterQueue = new ArrayList<>();
    }

    public static BookLoader getInstance() {
        if (instance == null) {
            instance = new BookLoader();
        }
        return instance;
    }

    public void reload(Context context) {
        File saveRoot = context.getExternalFilesDir("books");
        if (saveRoot == null) {
            return;
        }

        saveRootPath = saveRoot.getAbsolutePath();
        String json = Utils.readText(saveRootPath + "/.DIR");
        if (json == null) {
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JavaType listType = mapper.getTypeFactory().constructParametricType(ArrayList.class, Book.class);
            list = mapper.readValue(json, listType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(list);
            Utils.writeText(json, saveRootPath + "/.DIR");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public int getBookNum() {
        return list.size();
    }

    public Book getBook(int i) {
        if (list.size() == 0) {
            return null;
        }
        if (i < 0) {
            i = 0;
        } else if (i >= list.size()) {
            i = list.size() - 1;
        }
        return list.get(i);
    }

    public void addBook(String name, String path) {
        if (name == null || path == null) {
            return;
        }

        Book book = new Book();
        book.setTitle(name);
        book.setLocal(true);
        book.setLocalPath(path);
        list.add(book);

        save();
    }

    public Book popContentQueue() {
        if (contentQueue.size() == 0) {
            return null;
        }

        Book book = contentQueue.get(0);
        contentQueue.remove(book);
        return book;
    }

    public ChapterTask popChapterQueue() {
        if (chapterQueue.size() == 0) {
            return null;
        }

        ChapterTask task = chapterQueue.get(0);
        chapterQueue.remove(task);
        return task;
    }

    public void pushContentQueue(Book book) {
        if (!contentQueue.contains(book)) {
            contentQueue.add(book);
        }
    }

    public void pushChapterQueue(Book book, Chapter chapter) {
        ChapterTask task = new ChapterTask(book, chapter);
        if (chapterQueue.contains(task)) {
            chapterQueue.add(task);
        }
    }

    public void deleteBooks(List<Book> deleteList) {
        list.removeAll(deleteList);

        save();
    }

    public void bookBubble(int i) {
        if (list.size() == 0) {
            return;
        }
        if (i < 0) {
            i = 0;
        } else if (i >= list.size()) {
            i = list.size() - 1;
        }
        Book book = list.get(i);
        list.remove(i);
        list.add(0, book);
    }

    public class ChapterTask {
        public final Book book;
        public final Chapter chapter;

        public ChapterTask(Book book, Chapter chapter) {
            this.book = book;
            this.chapter = chapter;
        }
    }
}
