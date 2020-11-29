package sixue.naivereader;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.data.Book;

class BookLoader {
    private static BookLoader instance;
    private List<Book> list;
    private String saveRootPath;

    private BookLoader() {
        list = new ArrayList<>();
    }

    static BookLoader getInstance() {
        if (instance == null) {
            instance = new BookLoader();
        }
        return instance;
    }

    void reload(Context context) {
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

    void save() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(list);
            Utils.writeText(json, saveRootPath + "/books/.DIR");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    int getBookNum() {
        return list.size();
    }

    Book getBook(int i) {
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

    void addBook(Book book) {
        list.add(0, book);

        save();
    }

    void deleteBooks(List<Book> deleteList) {
        list.removeAll(deleteList);

        save();
    }

    Book findBook(String id) {
        for (Book book : list) {
            if (book.getId().equals(id)) {
                return book;
            }
        }
        return null;
    }

    void bookBubble(int i) {
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

    void bookBubble(Book book) {
        if (list.contains(book)) {
            list.remove(book);
            list.add(0, book);
        }

        save();
    }

    void clearGarbage() {
        File f = new File(saveRootPath + "/books/");
        File[] files = f.listFiles();
        if (files == null)
        {
            return;
        }
        List<String> favorites = new ArrayList<>();
        for (Book book : list) {
            favorites.add(book.getId());
        }
        for (File file : files) {
            if (file.isDirectory() && !favorites.contains(file.getName())) {
                Utils.deleteDirectory(file);
            }
        }
    }

    public int bookIndex(Book book) {
        return list.indexOf(book);
    }
}
