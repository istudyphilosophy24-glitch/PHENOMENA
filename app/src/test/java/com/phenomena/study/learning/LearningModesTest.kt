package com.phenomena.study.learning

import com.phenomena.study.model.*
import org.junit.Assert.*
import org.junit.Test

class LearningModesTest {
    private fun lesson() = Lesson(
        id="test01", workId="heidegger", section="§7",
        title="The Phenomenological Method", minutes=30,
        tags=listOf("phenomenology","disclosure"),
        synopsis="This is the guiding problem.",
        exposition="Here is the full exposition.",
        advanced="Here is the advanced account.",
        scaffold=listOf(Scaffold("First distinction","Begin from appearing")),
        vocabulary=listOf("phainomenon"),
        argument=listOf(ArgumentStep("Premise","The methodological claim","premise")),
        example="Consider an ordinary tool.",
        critique="A critic objects to the premise.",
        comparison="The Husserlian interpretation differs here.",
        seminar="Extended teaching seminar.",
        debate="The debate invites a challenge.",
        beats=listOf(Beat("Opening","A passage","Guide")),
        sourceNote="Edition-independent scholarly guidance"
    )
    @Test fun guidedNarrationIncludesAllSubstantiveLessonFields() {
        val text=GuidedTranscript.transcript(lesson())
        assertTrue(text.contains("This is the guiding problem."))
        assertTrue(text.contains("Here is the full exposition."))
        assertTrue(text.contains("Here is the advanced account."))
        assertTrue(text.contains("Begin from appearing"))
        assertTrue(text.contains("A critic objects"))
        assertTrue(text.indexOf("This is the guiding problem.") < text.indexOf("Here is the full exposition."))
        assertTrue(text.indexOf("Here is the full exposition.") < text.indexOf("A critic objects"))
    }
    @Test fun universityRewritesEachLessonIntoTwelveProgressiveMovements() {
        val stages=UniversityPedagogy.stages(lesson())
        assertEquals(12,stages.size)
        assertEquals(12,stages.map { it.id }.distinct().size)
        assertTrue(stages.all { it.transcript.length>120 && it.checkpoint.isNotBlank() })
        assertTrue(stages[0].title.contains("problem",ignoreCase=true))
        assertTrue(stages.last().title.contains("Teach",ignoreCase=true))
        assertTrue(UniversityPedagogy.transcript(lesson()).contains("full exposition."))
        assertEquals(12,UniversityPedagogy.caseStudies.size)
        assertTrue(UniversityPedagogy.caseStudies.all{it.scenario.length>150 && it.exercise.length>50})
    }
}
