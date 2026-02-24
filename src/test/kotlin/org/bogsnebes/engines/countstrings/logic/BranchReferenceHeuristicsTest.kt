package org.bogsnebes.engines.countstrings.logic

import kotlin.test.Test
import kotlin.test.assertEquals

class BranchReferenceHeuristicsTest {
    @Test
    fun `filterCandidates removes current and technical references`() {
        val filtered = BranchReferenceHeuristics.filterCandidates(
            references = listOf(
                "feature/login",
                "origin/feature/login",
                "origin/HEAD",
                "develop",
                "origin/develop",
                " ",
                "develop"
            ),
            currentBranch = "feature/login"
        )

        assertEquals(listOf("develop", "origin/develop"), filtered)
    }

    @Test
    fun `selectBestCandidate uses newest merge base`() {
        val selected = BranchReferenceHeuristics.selectBestCandidate(
            listOf(
                BranchReferenceHeuristics.BranchPointScore("main", mergeBaseTimestamp = 1_000, isRemote = false),
                BranchReferenceHeuristics.BranchPointScore("develop", mergeBaseTimestamp = 1_500, isRemote = false),
                BranchReferenceHeuristics.BranchPointScore("origin/release/1.0", mergeBaseTimestamp = 1_200, isRemote = true)
            )
        )

        assertEquals("develop", selected)
    }

    @Test
    fun `selectBestCandidate prefers local branch on tie`() {
        val selected = BranchReferenceHeuristics.selectBestCandidate(
            listOf(
                BranchReferenceHeuristics.BranchPointScore("origin/develop", mergeBaseTimestamp = 2_000, isRemote = true),
                BranchReferenceHeuristics.BranchPointScore("develop", mergeBaseTimestamp = 2_000, isRemote = false)
            )
        )

        assertEquals("develop", selected)
    }

    @Test
    fun `fallback order keeps main-master compatibility`() {
        assertEquals(
            listOf("main", "master", "origin/main", "origin/master"),
            BranchReferenceHeuristics.FALLBACK_BASE_REFERENCES
        )
    }
}
