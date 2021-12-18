package sixue.naivereader.helper

import sixue.naivereader.data.Book
import sixue.naivereader.data.BookKind
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*
import java.util.regex.Pattern

object LocalTextLoader {
    @JvmStatic
    fun createBook(uri: String): Book {
        var name = uri.substring(uri.lastIndexOf("/") + 1).lowercase(Locale.getDefault())
        if (name.endsWith(".txt")) {
            name = name.substring(0, name.length - 4)
        }
        try {
            name = URLDecoder.decode(name, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        name = name.substring(name.lastIndexOf("/") + 1).lowercase(Locale.getDefault())
        val title: String
        val l1 = name.indexOf("【")
        val r1 = name.indexOf("】")
        title = if (l1 != -1 && r1 != -1 && l1 < r1) {
            name.substring(l1 + 1, r1)
        } else {
            name
        }
        val author: String
        val a1 = name.indexOf("作者：")
        author = if (a1 != -1) {
            name.substring(a1 + "作者：".length)
        } else {
            "*"
        }
        return Book(
            id = uri,
            title = title,
            author = author,
            kind = BookKind.LocalText,
            localPath = uri,
        )
    }

    @JvmStatic
    fun calcChapterNodes(text: String?): List<Int> {
        val chapterNodes: MutableList<Int> = ArrayList()
        if (text == null) {
            return chapterNodes
        }
        val pattern = Pattern.compile(
            "^(?<=[\\S\\n\\r\\f])[\\d\\uFF10-\\uFF19]+\\b|\\b番外篇\\b|\\b第[\\d\\uFF10-\\uFF19一二三四五六七八九十百千零]+[部章节篇集卷]\\b"
        )
        val matcher = pattern.matcher(text)
        var last = -1
        while (matcher.find()) {
            val i = matcher.start()
            if (last >= 0 && !text.substring(last, i).contains("\n")) {
                continue
            }
            last = i
            chapterNodes.add(i)
        }
        return chapterNodes
    }
}