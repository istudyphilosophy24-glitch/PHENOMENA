package com.phenomena.study.player

import com.phenomena.study.data.InstituteMaterials
import com.phenomena.study.model.Lesson
import kotlin.random.Random

/**
 * The one listening catalogue. Sections within a guide keep their argumentative
 * order. Radio shuffles programmes, not the sentences in a philosophical argument.
 *
 * Sources:
 *  guided:<lessonId> => original synopsis/exposition/advanced/example/critique
 *  <seminar module id> => preserved long-form curriculum
 *  v3-* => new authored lectures / dialogues.
 */
object UnifiedPrograms {
    const val GUIDED = "guided"
    const val LONG = "long"
    const val RADIO = "radio"
    const val GUIDED_PREFIX = "guided:"

    data class Section(val title: String, val text: String)
    data class Programme(val id: String, val workId: String, val title: String, val kind: String)

    fun guidedSections(lesson: Lesson): List<Section> = listOf(
        Section("01 · Synopsis", lesson.synopsis),
        Section("02 · Exposition", lesson.exposition),
        Section("03 · Advanced analysis", lesson.advanced),
        Section("04 · Illustrative example", lesson.example),
        Section("05 · Comparative perspective", lesson.comparison),
        Section("06 · Critical examination", lesson.critique)
    ).filter { it.text.isNotBlank() }
      .distinctBy { it.text.trim().lowercase() }

    fun radioProgrammes(
        guides: List<Programme>,
        longForm: List<Programme>,
        authored: List<Programme>
    ): List<Programme> = (guides + longForm + authored).filter { it.id.isNotBlank() }
        .distinctBy { it.id }

    /** Full-cycle shuffle: programmes are not repeated until every one was offered. */
    fun shuffleIds(programmes: List<Programme>, seed: Int): List<String> =
        programmes.map { it.id }.distinct().shuffled(Random(seed))

    /** Defers the next segment until the next selected programme, not a random sentence. */
    fun nextProgrammeIndex(ids: List<String>, current: Int, direction: Int): Int {
        if (ids.isEmpty()) return -1
        val position = current.coerceIn(0, ids.lastIndex)
        val base = ids[position]
        var i = position + if (direction < 0) -1 else 1
        while (i in ids.indices && ids[i] == base) i += if (direction < 0) -1 else 1
        return i.takeIf { it in ids.indices } ?: position
    }

    fun sectionWordCount(sections: List<Section>): Int =
        sections.sumOf { it.text.trim().split(Regex("\\s+")).count(String::isNotBlank) }
}
