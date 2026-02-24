package org.bogsnebes.engines.countstrings.logic

import com.intellij.ui.JBColor
import java.awt.Color

enum class ChangedLinesIndicatorState(val label: String, val color: Color) {
    GREEN("Green", JBColor(Color(0x2E7D32), Color(0x81C784))),
    YELLOW("Yellow", JBColor(Color(0xF9A825), Color(0xFBC02D))),
    RED("Red", JBColor(Color(0xC62828), Color(0xE57373)));

    companion object {
        fun fromTotalChangedLines(totalChangedLines: Int): ChangedLinesIndicatorState {
            return when {
                totalChangedLines < ChangedLinesThresholds.GREEN_MAX_EXCLUSIVE -> GREEN
                totalChangedLines <= ChangedLinesThresholds.YELLOW_MAX_INCLUSIVE -> YELLOW
                else -> RED
            }
        }
    }
}
