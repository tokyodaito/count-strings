package org.bogsnebes.engines.countstrings.service

fun interface ChangedLinesStatusListener {
    fun totalChangedLinesUpdated(snapshot: ChangedLinesSnapshot)
}
