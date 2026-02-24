package org.bogsnebes.engines.countstrings.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.Alarm
import com.intellij.util.messages.Topic
import git4idea.repo.GitRepositoryManager
import org.bogsnebes.engines.countstrings.logic.ChangedLinesCalculationResult
import org.bogsnebes.engines.countstrings.logic.GitChangedLinesCalculator
import org.bogsnebes.engines.countstrings.logic.UnsavedChangesEstimator

@Service(Service.Level.PROJECT)
class ChangedLinesStatusService(private val project: Project) : Disposable {
    private val gitRepositoryManager = GitRepositoryManager.getInstance(project)
    private val calculator = GitChangedLinesCalculator(project)
    private val unsavedChangesEstimator = UnsavedChangesEstimator()
    private val refreshAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    @Volatile
    private var snapshot: ChangedLinesSnapshot = ChangedLinesSnapshot(
        totalChangedLines = 0,
        baseReference = null,
        committedVsBaseLines = 0,
        stagedLines = 0,
        unstagedLines = 0,
        unsavedLines = 0
    )

    fun getSnapshot(): ChangedLinesSnapshot = snapshot

    fun requestRefresh(reason: String, debounceMs: Int = REFRESH_DEBOUNCE_MS_DEFAULT) {
        refreshAlarm.cancelAllRequests()
        refreshAlarm.addRequest(
            { refreshNow(reason) },
            debounceMs
        )
    }

    fun shouldRefreshAfterVfsEvents(events: List<VFileEvent>): Boolean {
        if (events.none { it.isFromRefresh }) {
            return false
        }

        val repositoryRoots = gitRepositoryManager.repositories.map { FileUtil.toSystemIndependentName(it.root.path) }
        if (repositoryRoots.isEmpty()) {
            return false
        }

        return events.any { event ->
            val path = FileUtil.toSystemIndependentName(event.path)
            repositoryRoots.any { root -> path == root || path.startsWith("$root/") }
        }
    }

    fun shouldRefreshAfterDocumentChange(document: Document): Boolean {
        val file = FileDocumentManager.getInstance().getFile(document) ?: return false
        val path = FileUtil.toSystemIndependentName(file.path)
        val repositoryRoots = gitRepositoryManager.repositories.map { FileUtil.toSystemIndependentName(it.root.path) }
        if (repositoryRoots.isEmpty()) {
            return false
        }

        return repositoryRoots.any { root -> path == root || path.startsWith("$root/") }
    }

    private fun refreshNow(reason: String) {
        if (project.isDisposed) {
            return
        }

        val repositories = gitRepositoryManager.repositories
        val calculations = repositories.map(calculator::calculate)
        val unsavedByRepositoryRoot = ApplicationManager.getApplication().runReadAction<Map<String, Int>> {
            unsavedChangesEstimator.calculateByRepository(repositories)
        }
        val enrichedCalculations = calculations.mapIndexed { index, calculation ->
            val repositoryRoot = FileUtil.toSystemIndependentName(repositories[index].root.path)
            calculation.withUnsavedLines(unsavedByRepositoryRoot[repositoryRoot] ?: 0)
        }
        val nextSnapshot = ChangedLinesSnapshot(
            totalChangedLines = enrichedCalculations.sumOf(ChangedLinesCalculationResult::totalChangedLines),
            baseReference = aggregateBaseReference(enrichedCalculations),
            committedVsBaseLines = enrichedCalculations.sumOf(ChangedLinesCalculationResult::committedVsBaseLines),
            stagedLines = enrichedCalculations.sumOf(ChangedLinesCalculationResult::stagedLines),
            unstagedLines = enrichedCalculations.sumOf(ChangedLinesCalculationResult::unstagedLines),
            unsavedLines = enrichedCalculations.sumOf(ChangedLinesCalculationResult::unsavedLines)
        )
        if (nextSnapshot == snapshot) {
            return
        }

        snapshot = nextSnapshot
        ApplicationManager.getApplication().invokeLater(
            {
                if (!project.isDisposed) {
                    project.messageBus.syncPublisher(TOPIC).totalChangedLinesUpdated(nextSnapshot)
                }
            },
            project.disposed
        )
    }

    override fun dispose() = Unit

    private fun aggregateBaseReference(calculations: List<ChangedLinesCalculationResult>): String? {
        val references = calculations.mapNotNull(ChangedLinesCalculationResult::baseReference).distinct()
        return when (references.size) {
            0 -> null
            1 -> references.single()
            else -> MULTIPLE_BASE_REFERENCES_LABEL
        }
    }

    companion object {
        const val REFRESH_DEBOUNCE_MS_DEFAULT: Int = 1200
        const val REFRESH_DEBOUNCE_MS_TYPING: Int = 5000
        private const val MULTIPLE_BASE_REFERENCES_LABEL: String = "multiple branches"
        val TOPIC: Topic<ChangedLinesStatusListener> = Topic.create(
            "Changed Lines Status Updates",
            ChangedLinesStatusListener::class.java
        )

        fun getInstance(project: Project): ChangedLinesStatusService = project.service()
    }
}
