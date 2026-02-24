package org.bogsnebes.engines.countstrings.vcs

import com.intellij.openapi.project.DumbAware
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import org.bogsnebes.engines.countstrings.service.ChangedLinesStatusService

class ChangedLinesGitRepositoryListener : GitRepositoryChangeListener, DumbAware {
    override fun repositoryChanged(repository: GitRepository) {
        val project = repository.project
        if (!project.isDisposed) {
            ChangedLinesStatusService.getInstance(project).requestRefresh("git-repository-change")
        }
    }
}
