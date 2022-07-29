package com.github.jensim.megamanipulator.actions.git

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.git.clone.CloneAttempt
import com.github.jensim.megamanipulator.actions.git.clone.CloneAttemptResult
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.github.jensim.megamanipulator.ui.UiProtector
import com.github.jensim.megamanipulator.ui.groupPanel
import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import org.slf4j.LoggerFactory
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent

class CloneHistoryWindow(val project: Project) : ToolWindowTab {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val state: MegaManipulatorSettingsState by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val cloneOperator: CloneOperator by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }

    private val retrySelectedButton = JButton("Retry selected")
    private val retryFailedButton = JButton("Retry failed")
    private val attemptSelector = GeneralKtDataTable(
        type = CloneAttempt::class,
        columns = listOf("Time" to { it.time.toString() }),
        colorizer = { it.results.any { repo -> !repo.success } },
    )
    private val resultSelector = GeneralKtDataTable(
        type = CloneAttemptResult::class,
        columns = listOf("Repo" to { it.repo.asPathString() }),
        colorizer = { !it.success },
    )
    private val actionSelector = GeneralKtDataTable(
        type = Action::class,
        columns = listOf("Action" to { it.what }),
        colorizer = { it.how.exitCode != 0 },
    )
    private val actionText = JBTextArea().apply {
        isEditable = false
        preferredSize = Dimension(10_000, 10_000)
    }

    override val content: JComponent = panel {
        verticalAlign(VerticalAlign.FILL)
        horizontalAlign(HorizontalAlign.FILL)
        row {
            cell(retrySelectedButton)
            cell(retryFailedButton)
        }
        row {
            scrollCell(attemptSelector)
                .verticalAlign(VerticalAlign.TOP)
            scrollCell(resultSelector)
                .verticalAlign(VerticalAlign.TOP)
            scrollCell(actionSelector)
                .verticalAlign(VerticalAlign.TOP)
            groupPanel("Text") {
                scrollCell(actionText)
                    .verticalAlign(VerticalAlign.TOP)
            }
        }
    }

    init {
        attemptSelector.apply {
            this.preferredSize = Dimension(10_000, 10_000)
            this.addListSelectionListener {
                val selected = attemptSelector.selectedValuesList
                val results: List<CloneAttemptResult> = selected.firstOrNull()?.results ?: emptyList()
                resultSelector.setListData(results)
                resultSelector.selectFirst()
                retryFailedButton.isEnabled = false
                if (selected.size == 1) {
                    if (selected.firstOrNull()?.results.orEmpty().any { !it.success }) {
                        retryFailedButton.isEnabled = true
                    }
                }
            }
        }
        resultSelector.apply {
            this.preferredSize = Dimension(10_000, 10_000)
            this.addListSelectionListener {
                val selected = resultSelector.selectedValuesList
                val actions = selected.firstOrNull()?.actions ?: emptyList()
                actionSelector.setListData(actions)
                actionSelector.selectFirst()

                retrySelectedButton.isEnabled = false
                if (selected.isNotEmpty()) {
                    retrySelectedButton.isEnabled = true
                }
            }
        }
        actionSelector.apply {
            this.preferredSize = Dimension(10_000, 10_000)
            this.addListSelectionListener {
                val text = actionSelector.selectedValuesList.firstOrNull()?.how?.getFullDescription() ?: ""
                actionText.text = text
            }
        }
        retrySelectedButton.addActionListener {
            val selected = resultSelector.selectedValuesList
            if (selected.isEmpty()) {
                notificationsOperator.show("No results selected", "Unable to clone, please select at least one result", WARNING)
            } else {
                retryClone(selected)
            }
        }
        retryFailedButton.addActionListener {
            val selected = attemptSelector.selectedValuesList
            if (selected.size != 1) {
                notificationsOperator.show("No attempt selected", "Unable to clone, please select ONE attempt", WARNING)
            } else {
                val failed = selected[0].results.filter { !it.success }
                if (failed.isNotEmpty()) {
                    retryClone(failed)
                } else {
                    notificationsOperator.show("Nothing failed", "Unable to clone, please select an attempt with failed clones", WARNING)
                }
            }
        }
    }

    private fun retryClone(result: List<CloneAttemptResult>) {
        try {
            val settings = settingsFileOperator.readSettings()!!
            uiProtector.mapConcurrentWithProgress("Retry clone", data = result) {
                cloneOperator.clone(settings = settings, it.repo, it.branch, false, null)
            }
        } catch (e: Exception) {
            logger.error("Failed retry clones with exception", e)
            notificationsOperator.show("Exception cloning", "${e.javaClass.simpleName} ${e.message}, more info in IDE logs", ERROR)
        }
    }

    override fun refresh() {
        state.cloneHistory.toList().let { cloneHistory ->
            attemptSelector.setListData(cloneHistory)
            attemptSelector.selectFirst()
        }
    }
}
