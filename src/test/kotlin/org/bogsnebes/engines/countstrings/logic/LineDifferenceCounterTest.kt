package org.bogsnebes.engines.countstrings.logic

import kotlin.test.Test
import kotlin.test.assertEquals

class LineDifferenceCounterTest {
    @Test
    fun `returns zero for equal text`() {
        assertEquals(0, LineDifferenceCounter.countChangedLines("a\nb\nc", "a\nb\nc"))
    }

    @Test
    fun `counts line insertion`() {
        assertEquals(1, LineDifferenceCounter.countChangedLines("a\nb", "a\nx\nb"))
    }

    @Test
    fun `counts line deletion`() {
        assertEquals(1, LineDifferenceCounter.countChangedLines("a\nx\nb", "a\nb"))
    }

    @Test
    fun `counts line replacement as delete plus insert`() {
        assertEquals(2, LineDifferenceCounter.countChangedLines("a\nold\nb", "a\nnew\nb"))
    }
}
