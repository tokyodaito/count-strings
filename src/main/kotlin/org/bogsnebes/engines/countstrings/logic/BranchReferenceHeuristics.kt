package org.bogsnebes.engines.countstrings.logic

internal object BranchReferenceHeuristics {
    val FALLBACK_BASE_REFERENCES: List<String> = listOf(
        "main",
        "master",
        "origin/main",
        "origin/master"
    )

    fun filterCandidates(references: List<String>, currentBranch: String?): List<String> {
        val normalizedCurrentBranch = currentBranch?.trim().orEmpty().ifEmpty { null }
        return references.asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .filterNot { it == ORIGIN_HEAD_REFERENCE }
            .filterNot { reference ->
                reference == normalizedCurrentBranch ||
                    (normalizedCurrentBranch != null && reference == "$ORIGIN_PREFIX$normalizedCurrentBranch")
            }
            .distinct()
            .toList()
    }

    fun selectBestCandidate(scores: List<BranchPointScore>): String? {
        return scores.sortedWith(
            compareByDescending<BranchPointScore> { it.mergeBaseTimestamp }
                .thenBy { it.isRemote }
                .thenBy { it.reference }
        ).firstOrNull()?.reference
    }

    data class BranchPointScore(
        val reference: String,
        val mergeBaseTimestamp: Long,
        val isRemote: Boolean
    )

    private const val ORIGIN_PREFIX: String = "origin/"
    private const val ORIGIN_HEAD_REFERENCE: String = "origin/HEAD"
}
