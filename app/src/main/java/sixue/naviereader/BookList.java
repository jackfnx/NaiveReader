package sixue.naviereader;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BookList {
    private static BookList instance;
    private List<Book> bookList;

    private BookList() {
        bookList = new ArrayList<>();
    }

    public static BookList getInstance() {
        if (instance == null) {
            instance = new BookList();
        }
        return instance;
    }

    public void reload(Context context) {
        SharedPreferences sp = context.getSharedPreferences("BookList", Context.MODE_PRIVATE);
        String json = sp.getString("s", "");
        ObjectMapper mapper = new ObjectMapper();
        JavaType listType = mapper.getTypeFactory().constructParametricType(ArrayList.class, Book.class);
        try {
            bookList = mapper.readValue(json, listType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getBookNum() {
        return bookList.size();
    }

    public Book getBook(int i) {
        if (i < 0) {
            i = 0;
        } else if (i >= bookList.size()) {
            i = bookList.size() - 1;
        }
        return bookList.get(i);
    }

    public void addBook(String name, String path, Context context) {
        if (name == null || path == null) {
            return;
        }

        Book book = new Book();
        book.setTitle(name);
        book.setLocal(true);
        book.setLocalPath(path);
        bookList.add(book);

        ObjectMapper mapper = new ObjectMapper();
        try {
            String s = mapper.writeValueAsString(bookList);

            SharedPreferences.Editor editor = context.getSharedPreferences("BookList", Context.MODE_PRIVATE).edit();
            editor.putString("s", s);
            editor.apply();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public class Book {
        private String title;
        private boolean isLocal;
        private String localPath;
        private String url;
        private int current;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public boolean isLocal() {
            return isLocal;
        }

        public void setLocal(boolean local) {
            isLocal = local;
        }

        public String getLocalPath() {
            return localPath;
        }

        public void setLocalPath(String localPath) {
            this.localPath = localPath;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            this.current = current;
        }
    }
}
