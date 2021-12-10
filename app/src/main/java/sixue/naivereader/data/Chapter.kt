package sixue.naivereader.data

data class Chapter(
    var id: String,
    var title: String,
    var author: String?=null,
    var source: String?=null,
    var savePath: String?=null,
    var timestamp: Long=0,
)