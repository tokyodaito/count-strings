package org.bogsnebes.engines.countstrings.vcs

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.bogsnebes.engines.countstrings.service.ChangedLinesStatusService

class ChangedLinesDocumentListener(private val project: Project) : DocumentListener, DumbAware {
    override fun documentChanged(event: DocumentEvent) {
        if (project.isDisposed) {
            return
        }

        val service = ChangedLinesStatusService.getInstance(project)
        if (service.shouldRefreshAfterDocumentChange(event.document)) {
            service.requestRefresh(
                reason = "typing",
                debounceMs = ChangedLinesStatusService.REFRESH_DEBOUNCE_MS_TYPING
            )
        }
    }
}
