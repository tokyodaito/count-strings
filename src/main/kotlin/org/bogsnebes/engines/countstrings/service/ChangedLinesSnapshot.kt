package org.bogsnebes.engines.countstrings.service

data class ChangedLinesSnapshot(
    val totalChangedLines: Int,
    val baseReference: String?,
    val committedVsBaseLines: Int,
    val stagedLines: Int,
    val unstagedLines: Int,
    val unsavedLines: Int
)
