package sixue.naivereader;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Book;

public class BookLoader {
    private static BookLoader instance;
    private List<Book> list;
    private String saveRootPath;

    private BookLoader() {
        list = new ArrayList<>();
    }

    public static BookLoader getInstance() {
        if (instance == null) {
            instance = new BookLoader();
        }
        return instance;
    }

    public void reload(Context context) {
        saveRootPath = Utils.getSavePathRoot(context);
        String json = Utils.readText(saveRootPath + "/books/.DIR");
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
            Utils.writeText(json, saveRootPath + "/books/.DIR");
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

    public void addBook(Book book) {
        list.add(0, book);

        save();
    }

    public void deleteBooks(List<Book> deleteList) {
        list.removeAll(deleteList);

        save();
    }

    public Book findBook(String id) {
        for (Book book : list) {
            if (book.getId().equals(id)) {
                return book;
            }
        }
        return null;
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

        save();
    }

    public void bookBubble(Book book) {
        if (list.contains(book)) {
            list.remove(book);
            list.add(0, book);
        }

        save();
    }
}
