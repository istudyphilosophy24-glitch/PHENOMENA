package com.phenomena.study.reader

import org.junit.Assert.*
import org.junit.Test

class ParagraphIndexTest {
    @Test fun sourceOffsetsRemainExact() {
        val source = "  Consciousness intends something. \n\n We describe the appearing world.\n\nEidetic variation helps. "
        val parts = ParagraphIndex.split("lesson:test",source,"study_guide")
        assertEquals(3,parts.size)
        for (p in parts) assertEquals(p.text,source.substring(p.startOffset,p.endOffsetExclusive))
        assertEquals("lesson:test:p0001",parts.first().id)
        assertEquals("lesson:test:p0003",parts.last().id)
    }
    @Test fun emptyTextHasNoParagraphs() {
        assertTrue(ParagraphIndex.split("test"," \n\n  ","import").isEmpty())
    }
    @Test fun noContentLostInLongParagraph() {
        val source = (1..200).joinToString(" ") { "Phenomenological method attends to the appearing world." }
        val parts = ParagraphIndex.split("long",source,"import")
        assertTrue(parts.isNotEmpty())
        for (p in parts) assertEquals(p.text,source.substring(p.startOffset,p.endOffsetExclusive))
        assertEquals(1,parts.first().ordinal)
    }
}
