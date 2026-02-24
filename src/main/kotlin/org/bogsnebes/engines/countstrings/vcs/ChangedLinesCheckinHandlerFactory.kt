package org.bogsnebes.engines.countstrings.vcs

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.vcs.changes.CommitContext
import org.bogsnebes.engines.countstrings.service.ChangedLinesStatusService

class ChangedLinesCheckinHandlerFactory : CheckinHandlerFactory(), DumbAware {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {
            override fun checkinSuccessful() {
                val project = panel.project
                if (!project.isDisposed) {
                    ChangedLinesStatusService.getInstance(project).requestRefresh("git-commit")
                }
            }
        }
    }
}
