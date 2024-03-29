package sixue.naivereader.data

data class Packet(
    var title: String,
    var author: String,
    var simple: Boolean,
    var key: String,
    var summary: String,
    var timestamp: Long,
    var source: String,

    var chapters: List<Chapter>? = null,
    var regexps: List<String>? = null,
)