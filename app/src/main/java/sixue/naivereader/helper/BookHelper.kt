package sixue.naivereader.helper

import android.content.Context
import android.graphics.Bitmap

interface BookHelper {
    fun reloadContent(context: Context): Boolean
    fun downloadContent(context: Context)
    fun loadCoverBitmap(context: Context): Bitmap
}