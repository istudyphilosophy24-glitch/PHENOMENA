package com.phenomena.study.data

import org.junit.Assert.*
import org.junit.Test

class InstituteRegressionTest {
    @Test fun selectedWorkNeverInheritsFirstGlobalSeminar() {
        val modules=listOf(
            "M01" to "time", "M02" to "husserl",
            "M15" to "heidegger", "M16" to "heidegger", "M17" to "sartre")
        assertEquals(listOf("M15","M16"),SeminarTopicSelection.initialIds("heidegger",modules))
        assertEquals(listOf("M02"),SeminarTopicSelection.initialIds("husserl",modules))
        assertTrue(SeminarTopicSelection.initialIds(null,modules).isEmpty())
        assertTrue(SeminarTopicSelection.initialIds("unknown",modules).isEmpty())
    }
    @Test fun explicitSelectionCapacityIsBoundedAndIdsAreStable() {
        val pairs=(1..50).map { "M$it" to "heidegger" } + ("M1" to "heidegger")
        val chosen=SeminarTopicSelection.initialIds("heidegger",pairs)
        assertEquals(24,chosen.size)
        assertEquals(24,chosen.distinct().size)
    }
    @Test fun authoredLecturesHaveDistinctIdsAndProvenance() {
        val ids=InstituteMaterials.lectures.map{it.id}
        assertEquals(ids.size,ids.distinct().size)
        assertTrue(InstituteMaterials.lectures.all{
            it.id.startsWith("v3-") && it.paragraphs.isNotEmpty() &&
            it.reference.isNotBlank() && it.words>100
        })
        assertTrue(InstituteMaterials.dossiers.all{
            it.lessonId.isNotBlank() && it.sourceLocator.isNotBlank() &&
            it.reconstruction.size >= 3
        })
    }
}
