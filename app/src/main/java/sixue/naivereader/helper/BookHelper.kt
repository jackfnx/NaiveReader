package sixue.naivereader.helper

import android.content.Context
import android.graphics.Bitmap
import sixue.naivereader.data.Chapter

interface BookHelper {
    data class CurrentPosition(val currentIdx: Int, val currentPos: Int)
    data class ChapterDescription(val title: String, val summary: String, val summaryGravity: Int)
    data class UpdateChapterTitleEvent(val update: Boolean, val chapterTitle: String)
    data class TurnPageNewIndex(val notOver: Boolean, val newIndex: Int, val newPosition: Int)
    fun reloadContent(context: Context): Boolean
    fun downloadContent(context: Context)
    fun loadCoverBitmap(context: Context): Bitmap
    fun calcCurrentPosition(seemingIndex: Int): CurrentPosition
    fun getCurrentSeemingIndex(): Int
    fun getChapterSize(): Int
    fun getChapterDescription(seemingIndex: Int, context: Context): ChapterDescription
    fun updateChapterTitleOnPageChange(): UpdateChapterTitleEvent
    fun calcTurnPageNewIndex(step: Int): TurnPageNewIndex
    fun progressText(context: Context): String
    fun readText(chapter: Chapter, context: Context): String
}

