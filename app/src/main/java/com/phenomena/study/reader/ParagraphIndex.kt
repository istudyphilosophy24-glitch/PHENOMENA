package com.phenomena.study.reader

import com.phenomena.study.data.ImportedDocument
import com.phenomena.study.model.Lesson

/** Text offsets refer to the extracted user-provided import or original PHENOMENA commentary.
 * They are not equivalent to published book page numbers or paragraph identifiers. */
data class ReaderParagraph(
    val id: String,
    val ordinal: Int,
    val text: String,
    val startOffset: Int,
    val endOffsetExclusive: Int,
    val provenance: String
)

data class ReaderDocument(
    val id: String,
    val title: String,
    val provenance: String,
    val sourceNote: String,
    val paragraphs: List<ReaderParagraph>
)

object ParagraphIndex {
    private val gaps = Regex("\\r?\\n[\\t ]*\\r?\\n+")
    private val sentenceGaps = Regex("(?<=[.!?])\\s+(?=[A-ZÀ-ÖØ-Þ])")
    private const val TARGET_LENGTH = 1100

    fun forImport(document: ImportedDocument): ReaderDocument = ReaderDocument(
        id = "import:${document.id}", title = document.title,
        provenance = "USER-IMPORTED ${document.format.uppercase()}",
        sourceNote = "Text was extracted from your supplied file. Paragraph boundaries are inferred, " +
            "and PDF page numbers or edition correspondences have not been verified.",
        paragraphs = split("import:${document.id}", document.content, "user_import")
    )

    fun forLesson(lesson: Lesson, section: String = "Exposition"): ReaderDocument {
        val text = when (section) {
            "Synopsis" -> lesson.synopsis
            "Advanced" -> lesson.advanced
            "Comparison" -> lesson.comparison
            "Criticism" -> lesson.critique
            else -> lesson.exposition
        }
        return ReaderDocument(
            id = "lesson:${lesson.id}:$section", title = "${lesson.section} · ${lesson.title}",
            provenance = "ORIGINAL PHENOMENA COMMENTARY · $section",
            sourceNote = lesson.sourceNote + " No passage on this screen is a quotation " +
                "from a licensed edition unless separately identified.",
            paragraphs = split("lesson:${lesson.id}:$section", text, "study_guide")
        )
    }

    /** Produces stable paragraph IDs and character offsets without altering text.
     * Very long extracted paragraphs are split on sentence boundaries; each piece
     * still has a stable byte-independent offset into the source String. */
    fun split(sourceId: String, text: String, provenance: String): List<ReaderParagraph> {
        if (text.isBlank()) return emptyList()
        val spans = mutableListOf<Pair<Int, Int>>()
        var cursor = 0
        for (m in gaps.findAll(text)) {
            if (m.range.first > cursor) spans.add(cursor to m.range.first)
            cursor = m.range.last + 1
        }
        if (cursor < text.length) spans.add(cursor to text.length)
        val splitSpans = mutableListOf<Pair<Int, Int>>()
        for ((a,b) in spans) {
            val start = text.indexOfFirstNonSpace(a, b)
            if (start == b) continue
            val end = text.indexOfLastNonSpace(start,b) + 1
            if (end - start <= TARGET_LENGTH) {
                splitSpans.add(start to end)
            } else {
                var from = start
                val range = text.substring(start, end)
                for (match in sentenceGaps.findAll(range)) {
                    val gapEnd = start + match.range.last + 1
                    if (gapEnd - from > TARGET_LENGTH && from < gapEnd) {
                        splitSpans.add(from to gapEnd)
                        from = gapEnd
                    }
                }
                if (from < end) splitSpans.add(from to end)
            }
        }
        return splitSpans.mapIndexed { i, (a,b) ->
            ReaderParagraph("$sourceId:p${(i+1).toString().padStart(4,'0')}", i+1,
                text.substring(a,b),a,b,provenance)
        }
    }
    private fun String.indexOfFirstNonSpace(start: Int, end: Int): Int {
        var i=start
        while (i<end && this[i].isWhitespace()) i++
        return i
    }
    private fun String.indexOfLastNonSpace(start: Int, end: Int): Int {
        var i=end-1
        while (i>=start && this[i].isWhitespace()) i--
        return i
    }
}
