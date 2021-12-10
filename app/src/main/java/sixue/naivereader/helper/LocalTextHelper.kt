package sixue.naivereader.helper

import android.content.Context
import android.graphics.Bitmap
import sixue.naivereader.Utils
import sixue.naivereader.data.Book

class LocalTextHelper(private val book: Book) : BookHelper {
    private lateinit var cover: Bitmap
    override fun reloadContent(context: Context): Boolean {
        return true
    }

    override fun downloadContent(context: Context) {}
    override fun loadCoverBitmap(context: Context): Bitmap {
        if (!this::cover.isInitialized) {
            cover = Utils.getAutoCover(context, book.title, book.author, 1)
        }
        return cover
    }
}