package org.bogsnebes.engines.countstrings.statusbar

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager

class ChangedLinesStatusBarWidgetFactory : StatusBarWidgetFactory, DumbAware {
    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "Git Changed Lines Indicator"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return ChangedLinesStatusBarWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget as Disposable)
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun isConfigurable(): Boolean = true

    override fun isEnabledByDefault(): Boolean = true

    companion object {
        const val WIDGET_ID: String = "GitChangedLinesStatusWidget"

        fun refreshStatusBar(project: Project) {
            WindowManager.getInstance().getStatusBar(project)?.updateWidget(WIDGET_ID)
        }
    }
}
