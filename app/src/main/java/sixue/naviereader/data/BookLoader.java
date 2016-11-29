package sixue.naviereader.data;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sixue.naviereader.Utils;

public class BookLoader {
    private static BookLoader instance;
    private List<Book> list;
    private final List<Book> contentQueue;
    private final List<Chapter> chapterQueue;
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
        saveRootPath = context.getExternalFilesDir("books").getAbsolutePath();

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

    public void save() {
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

    public void updateBookPosition(Book book, int position) {
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

    public Chapter popChapterQueue() {
        if (chapterQueue.size() == 0) {
            return null;
        }

        Chapter book = chapterQueue.get(0);
        chapterQueue.remove(book);
        return book;
    }

    public void pushContentQueue(Book book) {
        contentQueue.add(book);
    }

    public void pushChapterQueue(Chapter chapter) {
        chapterQueue.add(chapter);
    }

    public void deleteBook(int i) {
        list.remove(i);

        save();
    }

    public void deleteBooks(List<Book> deleteList) {
        list.removeAll(deleteList);

        save();
    }
}
