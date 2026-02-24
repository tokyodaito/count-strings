package org.bogsnebes.engines.countstrings.lifecycle

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.editor.EditorFactory
import org.bogsnebes.engines.countstrings.service.ChangedLinesStatusService
import org.bogsnebes.engines.countstrings.vcs.ChangedLinesDocumentListener

class ChangedLinesStartupActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            ChangedLinesDocumentListener(project),
            project
        )
        ChangedLinesStatusService.getInstance(project).requestRefresh("project-open")
    }
}
