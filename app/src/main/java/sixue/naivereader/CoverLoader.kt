package sixue.naivereader

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import android.widget.ImageView
import sixue.naivereader.data.Book
import java.util.concurrent.Executors

class CoverLoader(private val context: Context) {

    private val memoryCache = object : LruCache<String, Bitmap>(50 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount
    }

    private val executor = Executors.newFixedThreadPool(3)

    fun loadCover(book: Book, target: ImageView) {
        val cacheKey = "cover_${book.id}"

        // 1. 检查内存缓存
        memoryCache.get(cacheKey)?.let {
            target.setImageBitmap(it)
            return
        }

        // 2. 设置默认图
        target.setImageResource(R.drawable.default_cover)
        target.tag = cacheKey

        // 3. 异步加载
        executor.submit {
            val bitmap = loadCoverInternal(book)
            bitmap.let {
                memoryCache.put(cacheKey, it)

                target.post {
                    if (target.tag == cacheKey) {
                        target.setImageBitmap(it)
                    }
                }
            }
        }
    }

    private fun loadCoverInternal(book: Book): Bitmap {
        return book.buildHelper().loadCoverBitmap(context)
    }

    fun release() {
        executor.shutdown()
        memoryCache.evictAll()
    }
}