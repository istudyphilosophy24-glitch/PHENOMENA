package com.phenomena.study.learning

import com.phenomena.study.model.Beat
import com.phenomena.study.model.Lesson

/**
 * One continuous Guided programme. The sequence synthesizes the user's selected
 * lesson's existing ORIGINAL study-guide fields; no unrelated work is introduced.
 * Dividing the transcript into engine-safe speech chunks is a TTS transport detail,
 * not separate listener-selectable tracks.
 */
object GuidedTranscript {
    data class Section(val heading:String,val text:String)
    fun sections(lesson: Lesson): List<Section> {
        val sections=mutableListOf<Section>()
        fun add(heading:String,content:String) {
            val cleaned=content.trim()
            if(cleaned.isNotBlank() && sections.none { it.text==cleaned })
                sections.add(Section(heading,cleaned))
        }
        add("Orientation", "We are studying ${lesson.title}, within ${lesson.section}. Start with the central problem, then follow the argument through its explanatory and critical stages.")
        add("The central question", lesson.synopsis)
        add("Conceptual foundations", lesson.scaffold.joinToString("\n\n") {
            "${it.title}. ${it.body}"
        })
        add("Full exposition", lesson.exposition)
        add("Advanced interpretation", lesson.advanced)
        add("Argument in stages", lesson.argument.mapIndexed { index, step ->
            "Step ${index+1}: ${step.title}. ${step.detail}"
        }.joinToString("\n\n"))
        add("Worked example",lesson.example)
        add("Comparative perspective",lesson.comparison)
        add("Critical objections",lesson.critique)
        add("Teaching seminar",lesson.seminar)
        add("Dialogical challenge",lesson.debate)
        add("Research invitation",
            "Recall the central question. Identify which claims are descriptions of experience, which are explanatory inferences, and which remain contested. Restate the strongest opposing interpretation before you decide whether the argument succeeds.")
        return sections
    }
    fun transcript(lesson: Lesson): String = sections(lesson)
        .joinToString("\n\n") { "${it.heading}.\n${it.text}" }

    fun beats(lesson: Lesson, split:(String)->List<Beat>):List<Beat> =
        sections(lesson).flatMap { section -> split(section.text).mapIndexed { i, beat ->
            beat.copy(title= if(i==0) section.heading else "${section.heading} · ${i+1}",
                kind="GUIDED · ${lesson.section}")
        } }
}
