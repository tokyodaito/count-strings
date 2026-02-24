package org.bogsnebes.engines.countstrings.vcs

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.bogsnebes.engines.countstrings.service.ChangedLinesStatusService

class ChangedLinesVfsRefreshListener : BulkFileListener, DumbAware {
    override fun after(events: List<VFileEvent>) {
        if (events.isEmpty()) {
            return
        }

        for (project in ProjectManager.getInstance().openProjects) {
            if (project.isDisposed) {
                continue
            }

            val service = ChangedLinesStatusService.getInstance(project)
            if (service.shouldRefreshAfterVfsEvents(events)) {
                service.requestRefresh("vfs-refresh")
            }
        }
    }
}
