package org.bogsnebes.engines.countstrings.logic

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtilCore
import git4idea.repo.GitRepository
import java.io.IOException

class UnsavedChangesEstimator {
    fun calculateByRepository(repositories: List<GitRepository>): Map<String, Int> {
        if (repositories.isEmpty()) {
            return emptyMap()
        }

        val normalizedRoots = repositories.map { it.root.path.normalizePath() }
        val rootsByPath = normalizedRoots.associateWith { 0 }.toMutableMap()
        val rootsByLengthDesc = normalizedRoots.sortedByDescending(String::length)

        val fileDocumentManager = FileDocumentManager.getInstance()
        for (document in fileDocumentManager.unsavedDocuments) {
            val file = fileDocumentManager.getFile(document) ?: continue
            if (!file.isInLocalFileSystem) {
                continue
            }
            if (file.fileType.isBinary) {
                continue
            }
            if (file.length > MAX_TEXT_FILE_BYTES) {
                continue
            }

            val filePath = file.path.normalizePath()
            val repositoryRoot = resolveRepositoryRoot(filePath, rootsByLengthDesc) ?: continue

            val onDiskText = try {
                VfsUtilCore.loadText(file)
            } catch (_: IOException) {
                continue
            }

            val inMemoryText = document.text
            val unsavedDelta = LineDifferenceCounter.countChangedLines(onDiskText, inMemoryText)
            if (unsavedDelta == 0) {
                continue
            }

            rootsByPath.merge(repositoryRoot, unsavedDelta, Int::plus)
        }

        return rootsByPath
    }

    private fun resolveRepositoryRoot(filePath: String, rootsByLengthDesc: List<String>): String? {
        return rootsByLengthDesc.firstOrNull { root ->
            filePath == root || filePath.startsWith("$root/")
        }
    }

    private fun String.normalizePath(): String = replace('\\', '/')

    companion object {
        private const val MAX_TEXT_FILE_BYTES: Long = 1_000_000L
    }
}
