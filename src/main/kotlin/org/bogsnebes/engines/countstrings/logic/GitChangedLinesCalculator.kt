package org.bogsnebes.engines.countstrings.logic

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository

class GitChangedLinesCalculator(private val project: Project) {
    private val log = Logger.getInstance(GitChangedLinesCalculator::class.java)

    fun calculate(repository: GitRepository): ChangedLinesCalculationResult {
        val baseReference = resolveBranchPointReference(repository)
            ?: resolveFallbackBaseReference(repository)
        val committedVsBaseLines = baseReference?.let { calculateForRange(repository, "$it...HEAD") } ?: 0
        val stagedLines = calculateForCommand(repository, "--cached")
        val unstagedLines = calculateForCommand(repository)

        return ChangedLinesCalculationResult(
            baseReference = baseReference,
            committedVsBaseLines = committedVsBaseLines,
            stagedLines = stagedLines,
            unstagedLines = unstagedLines,
            unsavedLines = 0
        )
    }

    private fun resolveBranchPointReference(repository: GitRepository): String? {
        val currentBranch = resolveCurrentBranch(repository)
        val candidates = BranchReferenceHeuristics.filterCandidates(
            listBranchReferences(repository),
            currentBranch
        )
        if (candidates.isEmpty()) {
            return null
        }

        val scores = candidates.mapNotNull { reference ->
            val mergeBaseCommit = resolveMergeBaseCommit(repository, reference) ?: return@mapNotNull null
            val mergeBaseTimestamp = resolveCommitTimestamp(repository, mergeBaseCommit) ?: return@mapNotNull null
            BranchReferenceHeuristics.BranchPointScore(
                reference = reference,
                mergeBaseTimestamp = mergeBaseTimestamp,
                isRemote = reference.startsWith(ORIGIN_PREFIX)
            )
        }

        return BranchReferenceHeuristics.selectBestCandidate(scores)
    }

    private fun resolveFallbackBaseReference(repository: GitRepository): String? {
        for (candidate in BranchReferenceHeuristics.FALLBACK_BASE_REFERENCES) {
            if (referenceExists(repository, candidate)) {
                return candidate
            }
        }
        return null
    }

    private fun resolveCurrentBranch(repository: GitRepository): String? {
        val handler = GitLineHandler(project, repository.root, GitCommand.REV_PARSE)
        handler.addParameters("--abbrev-ref", "HEAD")
        handler.setSilent(true)
        handler.setStdoutSuppressed(true)

        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            return null
        }

        val branch = result.output.firstOrNull()?.trim().orEmpty()
        if (branch.isEmpty() || branch == "HEAD") {
            return null
        }
        return branch
    }

    private fun listBranchReferences(repository: GitRepository): List<String> {
        val handler = GitLineHandler(project, repository.root, GitCommand.FOR_EACH_REF)
        handler.addParameters("--format=%(refname:short)", "refs/heads", "refs/remotes/origin")
        handler.setSilent(true)
        handler.setStdoutSuppressed(true)

        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            return emptyList()
        }

        return result.output
    }

    private fun resolveMergeBaseCommit(repository: GitRepository, reference: String): String? {
        val handler = GitLineHandler(project, repository.root, GitCommand.MERGE_BASE)
        handler.addParameters("HEAD", reference)
        handler.setSilent(true)
        handler.setStdoutSuppressed(true)

        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            return null
        }

        return result.output.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun resolveCommitTimestamp(repository: GitRepository, revision: String): Long? {
        val handler = GitLineHandler(project, repository.root, GitCommand.SHOW)
        handler.addParameters("--no-patch", "--format=%ct", revision)
        handler.setSilent(true)
        handler.setStdoutSuppressed(true)

        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            return null
        }

        return result.output.firstOrNull()?.trim()?.toLongOrNull()
    }

    private fun referenceExists(repository: GitRepository, reference: String): Boolean {
        val handler = GitLineHandler(project, repository.root, GitCommand.REV_PARSE)
        handler.addParameters("--verify", "--quiet", reference)
        handler.setSilent(true)
        handler.setStdoutSuppressed(true)

        return Git.getInstance().runCommand(handler).success()
    }

    private fun calculateForRange(repository: GitRepository, range: String): Int {
        val handler = GitLineHandler(project, repository.root, GitCommand.DIFF)
        handler.addParameters("--numstat", range)
        handler.setSilent(true)
        handler.setStdoutSuppressed(true)

        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            log.warn("Failed to calculate diff stats for ${repository.root.path} and range $range: ${result.errorOutputAsJoinedString}")
            return 0
        }

        return result.output.sumOf(::parseNumStatLine)
    }

    private fun calculateForCommand(repository: GitRepository, vararg additionalParameters: String): Int {
        val handler = GitLineHandler(project, repository.root, GitCommand.DIFF)
        handler.addParameters("--numstat", *additionalParameters)
        handler.setSilent(true)
        handler.setStdoutSuppressed(true)

        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            log.warn(
                "Failed to calculate working tree diff stats for ${repository.root.path}: ${result.errorOutputAsJoinedString}"
            )
            return 0
        }

        return result.output.sumOf(::parseNumStatLine)
    }

    private fun parseNumStatLine(line: String): Int {
        val parts = line.split('\t')
        if (parts.size < 3) {
            return 0
        }

        val insertions = parts[0].toIntOrNull() ?: 0
        val deletions = parts[1].toIntOrNull() ?: 0
        return insertions + deletions
    }

    companion object {
        private const val ORIGIN_PREFIX: String = "origin/"
    }
}
