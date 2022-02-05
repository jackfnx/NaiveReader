package sixue.naivereader.data

import com.fasterxml.jackson.annotation.JsonIgnore
import sixue.naivereader.helper.*
import java.util.*
import kotlin.collections.ArrayList

data class Book (
    var id: String,
    var title: String,
    var author: String,
    var kind: BookKind,
    var localPath: String? = null,
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
    var localChapterNodes: List<Int> = ArrayList()
    @JsonIgnore
    var localChapterTitles: List<String> = ArrayList()
    @JsonIgnore
    var localChapterSummaries: List<String> = ArrayList()

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
        if (localPath != null) {
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

    @JsonIgnore
    fun isRefreshable(): Boolean {
        return kind === BookKind.Online
    }

    @JsonIgnore
    fun isViewableInBrowser(): Boolean {
        return when {
            kind === BookKind.Online -> true
            kind === BookKind.Archive -> true
            else -> false
        }
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
                BookKind.Archive -> {
                    ArchiveHelper(this)
                }
            }
        }
        return bookHelper
    }
}