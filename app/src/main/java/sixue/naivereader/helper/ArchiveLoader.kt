package sixue.naivereader.helper

import android.content.Context
import android.graphics.BitmapFactory
import sixue.naivereader.Utils
import sixue.naivereader.data.Book
import sixue.naivereader.data.BookKind
import sixue.naivereader.data.Chapter
import java.io.File

object ArchiveLoader {
    @JvmStatic
    fun createArchive(book: Book, context: Context): Book {
        val saveRootPath = Utils.getSavePathRoot(context)
        val bookDir = "$saveRootPath/books/${book.id}"
        val zipFile = "$saveRootPath/archives/${book.id}.zip"

        var coverPath = "$saveRootPath/books/${book.id}/${book.siteId}/cover.jpg"
        var relCoverPath = "books/${book.id}/${book.siteId}/cover.jpg"
        if (!File(coverPath).exists()) {
            coverPath = "$saveRootPath/AutoCover/${book.title}.jpg"
            relCoverPath = "autoCover.jpg"
        }

        val coverBitmap = BitmapFactory.decodeFile(coverPath)
        val markedCoverBitmap = Utils.appendArchiveMarkToBitmap(coverBitmap)

        val folder = File(bookDir)
        val files: MutableList<Pair<Any, String>> = folder
            .walk()
            .filter { it.isFile }
            .map { Pair(it, it.relativeTo(folder).path) }
            .toMutableList()
        files.removeIf { it.first is File && (it.first as File).endsWith("cover.jpg") }
        files.add(Pair(markedCoverBitmap, relCoverPath))
        Utils.zip(files, zipFile)

        val newBook = Book(
            id = book.id,
            title = book.title,
            author = book.author,
            kind = BookKind.Archive,
            localPath = zipFile,
            siteId = book.siteId,
            sitePara = book.sitePara,
            sources = book.sources,
            currentChapterIndex = book.currentChapterIndex,
            currentPosition = book.currentPosition,
            wordCount = -1,
            coverSavePath = relCoverPath,
            end = true
            )
        newBook.chapterList = book.chapterList.map {
            Chapter(
                id = it.id,
                title = it.title,
                author = it.author,
                source = it.source,
                savePath = it.savePath,
                timestamp = it.timestamp
                )
        }
        return newBook
    }
}