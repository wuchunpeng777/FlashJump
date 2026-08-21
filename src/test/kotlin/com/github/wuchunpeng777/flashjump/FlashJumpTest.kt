package com.github.wuchunpeng777.flashjump

import com.github.wuchunpeng777.flashjump.boundaries.StandardBoundaries
import com.github.wuchunpeng777.flashjump.input.JumpMode
import com.github.wuchunpeng777.flashjump.input.JumpModeTracker
import com.github.wuchunpeng777.flashjump.search.SearchProcessor
import com.github.wuchunpeng777.flashjump.search.SearchQuery
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FlashJumpTest : BasePlatformTestCase() {

    fun testLiteralSearchIsCaseInsensitiveAndEscapesRegexCharacters() {
        myFixture.configureByText(PlainTextFileType.INSTANCE, "Alpha a.b ALPHA aXb")
        val processor = SearchProcessor(listOf(myFixture.editor), StandardBoundaries.WHOLE_FILE)

        processor.search(SearchQuery.Literal("alpha"))
        assertEquals(listOf(0, 10), processor.allMatches.map { it.startOffset })

        processor.search(SearchQuery.Literal("a.b"))
        assertEquals(listOf(6), processor.allMatches.map { it.startOffset })
    }

    fun testInvalidSearchSuffixIsNotConsumed() {
        myFixture.configureByText(PlainTextFileType.INSTANCE, "far foo fizz")
        val processor = SearchProcessor(listOf(myFixture.editor), StandardBoundaries.WHOLE_FILE)

        processor.search(SearchQuery.Literal("f"))
        assertTrue(processor.appendChar('o'))
        assertEquals("fo", processor.query.rawText)
        assertEquals(listOf(4), processor.allMatches.map { it.startOffset })

        assertFalse(processor.appendChar('x'))
        assertEquals("fo", processor.query.rawText)
        assertEquals(listOf(4), processor.allMatches.map { it.startOffset })
    }

    fun testJumpModesCycleInBothDirectionsAndReset() {
        val tracker = JumpModeTracker()

        assertEquals(JumpMode.JUMP, tracker.cycle())
        assertEquals(JumpMode.JUMP_END, tracker.cycle())
        assertEquals(JumpMode.JUMP, tracker.cycle(forward = false))

        tracker.reset()
        assertEquals(JumpMode.DISABLED, tracker.current)
        assertEquals(JumpMode.TARGET, tracker.cycle(forward = false))
    }
}
