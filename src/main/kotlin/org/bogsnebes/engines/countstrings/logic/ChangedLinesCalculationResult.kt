package org.bogsnebes.engines.countstrings.logic

data class ChangedLinesCalculationResult(
    val baseReference: String?,
    val committedVsBaseLines: Int,
    val stagedLines: Int,
    val unstagedLines: Int,
    val unsavedLines: Int
) {
    val totalChangedLines: Int
        get() = committedVsBaseLines + stagedLines + unstagedLines + unsavedLines

    fun withUnsavedLines(value: Int): ChangedLinesCalculationResult = copy(unsavedLines = value)
}
