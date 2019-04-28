package sixue.naivereader.provider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sixue.naivereader.Utils;
import sixue.naivereader.data.Book;
import sixue.naivereader.data.BookKind;

public class LocalTextProvider {

    public static Book createBook(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".txt")) {
            name = name.substring(0, name.length() - 4);
        }

        String title;
        int l1 = name.indexOf("【");
        int r1 = name.indexOf("】");
        if (l1 != -1 && r1 != -1 && l1 < r1) {
            title = name.substring(l1 + 1, r1);

        } else {
            title = name;
        }

        String author;
        int a1 = name.indexOf("作者：");
        if (a1 != -1) {
            author = name.substring(a1 + "作者：".length());
        } else {
            author = "*";
        }

        Book book = new Book();
        book.setId(file.getAbsolutePath());
        book.setTitle(title);
        book.setAuthor(author);
        book.setKind(BookKind.LocalText);
        book.setLocalPath(file.getAbsolutePath());
        return book;
    }

    public static List<Integer> calcChapterNodes(String text) {
        List<Integer> chapterNodes = new ArrayList<>();
        if (text == null) {
            return chapterNodes;
        }

        String[] patterns = new String[]{
                "\\b第[\\d\\uFF10-\\uFF19一二三四五六七八九十百千零]+[部章节篇集卷]\\b",
                "\\b[\\d\\uFF10-\\uFF19]+\\b"
        };
        for (String ps : patterns) {
            Pattern pattern = Pattern.compile(ps);
            Matcher matcher = pattern.matcher(text);
            int last = -1;
            while (matcher.find()) {
                int i = matcher.start();
                if (last >= 0 && !text.substring(last, i).contains("\n")) {
                    continue;
                }
                last = i;
                chapterNodes.add(i);
            }
            if (chapterNodes.size() != 0) {
                break;
            }
        }
        return chapterNodes;
    }
}
