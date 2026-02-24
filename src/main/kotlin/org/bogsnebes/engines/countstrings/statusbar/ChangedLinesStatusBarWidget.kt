package org.bogsnebes.engines.countstrings.statusbar

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.bogsnebes.engines.countstrings.logic.ChangedLinesIndicatorState
import org.bogsnebes.engines.countstrings.service.ChangedLinesStatusListener
import org.bogsnebes.engines.countstrings.service.ChangedLinesSnapshot
import org.bogsnebes.engines.countstrings.service.ChangedLinesStatusService
import javax.swing.JComponent

class ChangedLinesStatusBarWidget(private val project: Project) : CustomStatusBarWidget, Disposable {
    private val statusService = ChangedLinesStatusService.getInstance(project)
    private val label = JBLabel()
    private var statusBar: StatusBar? = null

    init {
        label.border = JBUI.Borders.empty(0, 8)
        updateLabel(statusService.getSnapshot())

        project.messageBus.connect(this).subscribe(
            ChangedLinesStatusService.TOPIC,
            ChangedLinesStatusListener { snapshot ->
                updateLabel(snapshot)
                statusBar?.updateWidget(ID())
            }
        )
    }

    override fun ID(): String = ChangedLinesStatusBarWidgetFactory.WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun getComponent(): JComponent = label

    override fun dispose() = Unit

    private fun updateLabel(snapshot: ChangedLinesSnapshot) {
        val totalChangedLines = snapshot.totalChangedLines
        val indicatorState = ChangedLinesIndicatorState.fromTotalChangedLines(totalChangedLines)
        label.text = "Δ $totalChangedLines"
        val breakdown = "committed: ${snapshot.committedVsBaseLines}, staged: ${snapshot.stagedLines}, " +
            "unstaged: ${snapshot.unstagedLines}, unsaved: ${snapshot.unsavedLines}"
        label.toolTipText = if (snapshot.baseReference != null) {
            "Git changed lines vs ${snapshot.baseReference}: $totalChangedLines ($breakdown, ${indicatorState.label})"
        } else {
            "Git changed lines: $totalChangedLines ($breakdown, ${indicatorState.label})"
        }
        label.foreground = indicatorState.color
    }
}
