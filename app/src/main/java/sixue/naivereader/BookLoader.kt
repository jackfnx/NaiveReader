package sixue.naivereader

import android.content.Context
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import sixue.naivereader.data.Book
import java.io.File
import java.io.IOException
import java.util.*

internal object BookLoader {
    private var list: MutableList<Book>
    private lateinit var saveRootPath: String
    fun reload(context: Context) {
        saveRootPath = Utils.getSavePathRoot(context)
        val json = Utils.readText("$saveRootPath/books/.DIR") ?: return
        try {
            val mapper = jacksonObjectMapper()
            val listType = mapper.typeFactory.constructParametricType(ArrayList::class.java, Book::class.java)
            list = mapper.readValue(json, listType)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun save() {
        try {
            val mapper = jacksonObjectMapper()
            val json = mapper.writeValueAsString(list)
            Utils.writeText(json, "$saveRootPath/books/.DIR")
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
    }

    val bookNum: Int
        get() = list.size

    fun getBook(i: Int): Book? {
        var j = i
        if (list.size == 0) {
            return null
        }
        if (j < 0) {
            j = 0
        } else if (j >= list.size) {
            j = list.size - 1
        }
        return list[j]
    }

    fun addBook(book: Book) {
        list.add(0, book)
        save()
    }

    fun deleteBooks(deleteList: List<Book>?) {
        list.removeAll(deleteList!!)
        save()
    }

    fun findBook(id: String): Book? {
        for (book in list) {
            if (book.id == id) {
                return book
            }
        }
        return null
    }

    fun bookBubble(j: Int) {
        var i = j
        if (list.size == 0) {
            return
        }
        if (i < 0) {
            i = 0
        } else if (i >= list.size) {
            i = list.size - 1
        }
        val book = list[i]
        list.removeAt(i)
        list.add(0, book)
        save()
    }

    fun bookBubble(book: Book) {
        if (list.contains(book)) {
            list.remove(book)
            list.add(0, book)
        }
        save()
    }

    fun clearGarbage() {
        val f = File("$saveRootPath/books/")
        val files = f.listFiles() ?: return
        val favorites: MutableList<String> = ArrayList()
        for (book in list) {
            favorites.add(book.id)
        }
        for (file in files) {
            if (file.isDirectory && !favorites.contains(file.name)) {
                Utils.deleteDirectory(file)
            }
        }
    }

    fun bookIndex(book: Book): Int {
        return list.indexOf(book)
    }

    init {
        list = ArrayList()
    }
}