package sixue.naivereader

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import org.jsoup.Jsoup
import org.mozilla.universalchardet.UniversalDetector
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.collections.ArrayList

object Utils {
    private val TAG = Utils::class.java.simpleName
    const val ACTION_DOWNLOAD_CONTENT_FINISH = "ACTION_DOWNLOAD_CONTENT_FINISH"
    const val ACTION_DOWNLOAD_CHAPTER_FINISH = "ACTION_DOWNLOAD_CHAPTER_FINISH"
    const val ACTION_DOWNLOAD_ALL_CHAPTER_FINISH = "ACTION_DOWNLOAD_ALL_CHAPTER_FINISH"
    const val ACTION_DOWNLOAD_COVER_FINISH = "ACTION_DOWNLOAD_COVER_FINISH"
    const val INTENT_PARA_BOOK_ID = "INTENT_PARA_BOOK_ID"
    const val INTENT_PARA_CHAPTER_ID = "INTENT_PARA_CHAPTER_ID"
    const val INTENT_PARA_PATH = "INTENT_PARA_PATH"
    const val INTENT_PARA_CURRENT_POSITION = "INTENT_PARA_CURRENT_POSITION"
    const val INTENT_PARA_CHAPTER_INDEX = "INTENT_PARA_CHAPTER_INDEX"
    fun readText(s: String): String? {
        val file = File(s)
        if (!file.exists()) {
            return null
        }
        var encoding = guessFileEncoding(file)
        if (encoding == null) {
            encoding = "utf-8"
        }
        return try {
            val `is`: InputStream = FileInputStream(file)
            val isr = InputStreamReader(`is`, encoding)
            val br = BufferedReader(isr)
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append("\n")
            }
            `is`.close()
            sb.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun writeText(s: String, path: String) {
        try {
            val file = File(path)
            val dir = file.parentFile
            if (dir != null && !dir.exists()) {
                val mk = dir.mkdirs()
                Log.i("Utils", "mkdir:$dir, $mk")
            }
            if (!file.exists()) {
                val cr = file.createNewFile()
                Log.i("Utils", "createNewFile:$file, $cr")
            }
            val fw = FileWriter(file, false)
            fw.write(s)
            fw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun guessFileEncoding(file: File): String? {
        return try {
            val `is`: InputStream = FileInputStream(file)
            guessFileEncoding(`is`)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun guessFileEncoding(resolver: ContentResolver, uri: Uri): String? {
        return try {
            val `is` = resolver.openInputStream(uri)
            guessFileEncoding(`is`)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun guessFileEncoding(`is`: InputStream?): String? {
        return try {
            val detector = UniversalDetector(null)
            val buf = ByteArray(1024)
            var n: Int
            while (`is`!!.read(buf).also { n = it } > 0 && !detector.isDone) {
                detector.handleData(buf, 0, n)
            }
            detector.dataEnd()
            val encoding = detector.detectedCharset
            detector.reset()
            encoding
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun verifyPermissions(activity: Activity) {
        val r = ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (r != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }
        val w = ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (w != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
    }

    fun createCropBitmap(unscaledBitmap: Bitmap, dstWidth: Int, dstHeight: Int): Bitmap {
        val srcRect = calcSrcRect(unscaledBitmap.width, unscaledBitmap.height, dstWidth, dstHeight)
        val dstRect = Rect(0, 0, dstWidth, dstHeight)
        val scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        return scaledBitmap
    }

    private fun calcSrcRect(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): Rect {
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val dstAspect = dstWidth.toFloat() / dstHeight.toFloat()
        return if (srcAspect > dstAspect) {
            val srcRectWidth = (srcHeight * dstAspect).toInt()
            val srcRectLeft = (srcWidth - srcRectWidth) / 2
            Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight)
        } else {
            val srcRectHeight = (srcWidth / dstAspect).toInt()
            val scrRectTop = (srcHeight - srcRectHeight) / 2
            Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight)
        }
    }

    fun <T> convert(list: List<T>, func: (T)->String): List<String> {
        val arr = ArrayList<String>(list.size)
        for (i in list.indices) {
            arr[i] = func(list[i])
        }
        return arr
    }

    fun deleteFile(path: String) {
        val file = File(path)
        val dr = file.delete()
        Log.i(TAG, "delete:$path, $dr")
    }

    fun mkdir(path: String) {
        val dir = File(path)
        if (!dir.exists()) {
            val mk = dir.mkdirs()
            Log.i(TAG, "mkdir:$dir, $mk")
        }
    }

    private fun exists(path: String): Boolean {
        val file = File(path)
        return file.exists()
    }

    fun getSavePathRoot(context: Context): String {
        return context.getExternalFilesDir(null)!!.absolutePath
    }

    fun deleteDirectory(file: File) {
        var mk = false
        if (file.exists()) {
            val files = file.listFiles() ?: return
            for (f in files) {
                if (f.isDirectory) {
                    deleteDirectory(f)
                } else {
                    mk = mk or f.delete()
                }
            }
        }
        mk = mk or file.delete()
        Log.i(TAG, "deleteDirectory:$file, $mk")
    }

    private fun paintCover(blank: Bitmap, title: String, author: String?): Bitmap {
        val minTextSize = 12
        val maxTextSize = 40
        val titlePR: PaintResult
        val regularTitle = parseRegularTitle(title)
        if (regularTitle.size == 2) {
            titlePR = paintText(blank, regularTitle.subList(0, 1), blank.height / 4, minTextSize, maxTextSize)
            paintText(blank, regularTitle.subList(1, 2), titlePR.y1, minTextSize, titlePR.textSize - 12)
        } else {
            val lines = explodeBySpecialChar(title, 6)
            titlePR = paintText(blank, lines, blank.height / 4, minTextSize, maxTextSize)
        }
        if (author != null) {
            val lines2 = explodeBySpecialChar(author, 7)
            val authorSize = if (titlePR.textSize > maxTextSize - 10) maxTextSize - 10 else titlePR.textSize - 1
            paintText(blank, lines2, blank.height * 3 / 4, minTextSize, authorSize)
        }
        return blank
    }

    private fun parseRegularTitle(title: String): List<String> {
        val regularTitle = ArrayList<String>()
        val pattens = listOf(
            Pair(Pattern.compile("【.+】(?=的作品集)"), "作品集"),
            Pair(Pattern.compile("【.+】(?=系列)"), "系列"),
            Pair(Pattern.compile("(?<=专题：)【.+】"), "专题"),
            Pair(Pattern.compile("(?<=冻结：)【.+】"), "冻结"),
        )
        for (pair in pattens) {
            val matcher = pair.first.matcher(title)
            if (matcher.find()) {
                regularTitle.add(title.substring(matcher.start(), matcher.end()))
                regularTitle.add(pair.second)
                break
            }
        }
        return regularTitle
    }

    private fun paintText(blank: Bitmap?, lines: List<String>, y0: Int, minSize: Int, maxSize: Int): PaintResult {
        val maxLine = Collections.max(lines) { s, t1 -> s.length - t1.length }
        val canvas = Canvas(blank!!)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.isDither = true
        paint.isFilterBitmap = true
        paint.color = Color.BLACK
        paint.setShadowLayer(1f, 0f, 1f, Color.LTGRAY)
        var textSize = minSize
        while (textSize < maxSize) {
            paint.textSize = textSize.toFloat()
            if (blank.width - paint.measureText(maxLine) < 10) {
                break
            }
            textSize++
        }
        var y1 : Int = y0
        for (i in lines.indices) {
            val line = lines[i]
            val bounds = Rect()
            paint.getTextBounds(line, 0, line.length, bounds)
            val advance =
                paint.getRunAdvance(line, 0, line.length, 0, line.length, false, line.length)
            val x = (blank.width - advance.toInt()) / 2
            val y = y0 + bounds.height() / 4 + i * bounds.height()
            canvas.drawText(line, x.toFloat(), y.toFloat(), paint)
            y1 = y + bounds.height()
        }
        return PaintResult(textSize, y1)
    }

    data class PaintResult(val textSize: Int, val y1: Int)

    private fun explodeBySpecialChar(title: String, maxLen: Int): List<String> {
        val list: MutableList<String> = ArrayList()
        var sb = StringBuilder()
        for (c in title.toCharArray()) {
            if (c in '0'..'9') {
                sb.append(c)
                continue
            }
            if (c in 'A'..'Z') {
                sb.append(c)
                continue
            }
            if (c in 'a'..'z') {
                sb.append(c)
                continue
            }
            val ub = Character.UnicodeBlock.of(c)
            if (ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                sb.append(c)
                continue
            } else if (ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                sb.append(c)
                continue
            } else if (ub === Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) {
                sb.append(c)
                continue
            } else if (ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                sb.append(c)
                continue
            } else if (ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                sb.append(c)
                continue
            } else if (ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C) {
                sb.append(c)
                continue
            } else if (ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D) {
                sb.append(c)
                continue
            } else if (ub === Character.UnicodeBlock.GENERAL_PUNCTUATION) {
                sb.append(c)
                continue
            } else if (ub === Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
                sb.append(c)
                continue
            }
            toMultiLines(list, sb, maxLen)
            sb = StringBuilder()
        }
        if (sb.isNotEmpty()) toMultiLines(list, sb, maxLen)
        return list
    }

    private fun toMultiLines(list: MutableList<String>, sb: StringBuilder, maxLen: Int) {
        val line = sb.toString()
        val len = line.length
        val pattern = Pattern.compile("^[a-zA-Z0-9]+$")
        val matcher = pattern.matcher(line)
        if (len <= maxLen || matcher.matches()) {
            list.add(line)
        } else if (len <= maxLen * 2) {
            list.add(line.substring(0, len / 2 + 1))
            list.add(line.substring(len / 2 + 1))
        } else {
            list.add(line.substring(0, maxLen))
            list.add("...")
        }
    }

    private fun getBlankCoverBitmap(context: Context, texture_id: Int): Bitmap {
        val w = 160
        val h = 200
        val textureName = String.format(Locale.CHINA, "texture_paper_%d.jpg", texture_id)
        val `is` = context.assets.open(textureName)
        val texture = BitmapFactory.decodeStream(`is`)
        val x = (Math.random() * (texture.width - w)).toInt()
        val y = (Math.random() * (texture.height - h)).toInt()
        val blank = Bitmap.createBitmap(texture, x, y, w, h)
        `is`.close()
        return blank
    }

    fun getAutoCover(context: Context, title: String, author: String?, texture: Int): Bitmap {
        val saveRoot = getSavePathRoot(context)
        val autoCoverRoot = "$saveRoot/AutoCover/"
        val autoCoverPath = "$autoCoverRoot$title.jpg"
        return if (exists(autoCoverPath)) {
            BitmapFactory.decodeFile(autoCoverPath)
        } else {
            val blank = getBlankCoverBitmap(context, texture)
            val cover = paintCover(blank, title, author)
            try {
                mkdir(autoCoverRoot)
                val os: OutputStream = FileOutputStream(autoCoverPath)
                cover.compress(Bitmap.CompressFormat.JPEG, 80, os)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            cover
        }
    }

    fun readTextFromZip(zipPath: String, path: String): String? {
        try {
            val zf = ZipFile(zipPath)
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val ze : ZipEntry = entries.nextElement() as ZipEntry
                if (!ze.isDirectory) {
                    if (ze.name == path) {
                        BufferedReader(
                                InputStreamReader(zf.getInputStream(ze))).use { br ->
                            val text = StringBuilder()
                            var line: String?
                            while (br.readLine().also { line = it } != null) {
                                text.append(line)
                                text.append("\n")
                            }
                            return text.toString()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun readBytesFromZip(zipPath: String, path: String): ByteArray? {
        try {
            val zf = ZipFile(zipPath)
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val ze : ZipEntry = entries.nextElement() as ZipEntry
                if (!ze.isDirectory) {
                    if (ze.name == path) {
                        BufferedInputStream(zf.getInputStream(ze)).use { `is` ->
                            val bos = ByteArrayOutputStream(ze.size.toInt())
                            val cache = ByteArray(1024)
                            var len: Int
                            while (`is`.read(cache, 0, cache.size).also { len = it } != -1) {
                                bos.write(cache, 0, len)
                            }
                            return bos.toByteArray()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun fmtTimestamp(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val f = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return f.format(date)
    }

    fun clearHtmlTag(text: String, tags: Array<String>): String {
        val textDoc = Jsoup.parse(text)
        for (t in tags) {
            for (o in textDoc.body().select(t)) {
                o.remove()
            }
        }
        return textDoc.body().html()
    }

    fun readExternalText(context: Context, uriString: String): String? {
        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        resolver.takePersistableUriPermission(uri, takeFlags)
        var encoding = guessFileEncoding(resolver, uri)
        if (encoding == null) {
            encoding = "utf-8"
        }
        try {
            BufferedReader(
                    InputStreamReader(resolver.openInputStream(uri), encoding)).use { br ->
                val text = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    text.append(line)
                    text.append("\n")
                }
                return text.toString()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

}