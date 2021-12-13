package sixue.naivereader.data

import com.fasterxml.jackson.annotation.JsonIgnore
import sixue.naivereader.helper.BookHelper
import sixue.naivereader.helper.LocalTextHelper
import sixue.naivereader.helper.OnlineHelper
import sixue.naivereader.helper.PacketHelper
import java.util.*

data class Book (
    var id: String,
    var title: String,
    var author: String,
    var kind: BookKind,
    var localPath: String,
    var siteId: String? = null,
    var sitePara: String? = null,

    var sources: List<Source> = ArrayList(),
    var currentChapterIndex: Int = -1,
    var currentPosition: Int = -1,
    var wordCount: Int = -1,
    var coverSavePath: String = "",
    var end: Boolean = false,
) {
    @JsonIgnore
    var chapterList: List<Chapter> = ArrayList()

    @JsonIgnore
    private lateinit var bookHelper: BookHelper
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("id=")
        sb.append(id)
        sb.append(",")
        sb.append("title=")
        sb.append(title)
        sb.append(",")
        sb.append("author=")
        sb.append(author)
        sb.append(",")
        sb.append("kind=")
        sb.append(kind)
        if (localPath != "") {
            sb.append(",")
            sb.append("localPath=")
            sb.append(localPath)
        }
        if (siteId != null) {
            sb.append(",")
            sb.append("siteId=")
            sb.append(siteId)
        }
        if (sitePara != null) {
            sb.append(",")
            sb.append("sitePara=")
            sb.append(sitePara)
        }
        sb.append("}")
        return sb.toString()
    }

    fun buildHelper(): BookHelper {
        if (!this::bookHelper.isInitialized) {
            bookHelper = when (kind) {
                BookKind.LocalText -> {
                    LocalTextHelper(this)
                }
                BookKind.Online -> {
                    OnlineHelper(this)
                }
                BookKind.Packet -> {
                    PacketHelper(this)
                }
            }
        }
        return bookHelper
    }
}