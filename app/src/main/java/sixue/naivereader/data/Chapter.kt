package sixue.naivereader.data

data class Chapter(
    var id: String,
    var title: String,
    var author: String = "",
    var source: String = "",
    var savePath: String = "",
    var timestamp: Long = 0,
)