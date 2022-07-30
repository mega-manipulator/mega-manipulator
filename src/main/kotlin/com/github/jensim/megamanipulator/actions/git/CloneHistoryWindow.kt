package com.github.jensim.megamanipulator.actions.git

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.git.clone.CloneAttempt
import com.github.jensim.megamanipulator.actions.git.clone.CloneAttemptResult
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.components.BorderLayoutPanel
import org.slf4j.LoggerFactory
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel

class CloneHistoryWindow(val project: Project) : ToolWindowTab {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val state: MegaManipulatorSettingsState by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val cloneOperator: CloneOperator by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }
    private val dialogGenerator: DialogGenerator by lazy { project.service() }

    private val retrySelectedButton = JButton("Retry selected")
    private val retryFailedButton = JButton("Retry failed")
    private val attemptSelector = GeneralKtDataTable(
        type = CloneAttempt::class,
        columns = listOf("Time" to { it.time.toString() }),
        colorizer = { it.results.any { repo -> !repo.success } },
        minSize = Dimension(500, 200),
        selectionMode = ListSelectionModel.SINGLE_SELECTION,
    )
    private val resultSelector = GeneralKtDataTable(
        type = CloneAttemptResult::class,
        columns = listOf("Repo" to { it.repo.asPathString() }),
        colorizer = { !it.success },
        minSize = Dimension(300, 200),
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    )
    private val actionSelector = GeneralKtDataTable(
        type = Action::class,
        columns = listOf("Action" to { it.what }),
        colorizer = { it.how.exitCode != 0 },
        minSize = Dimension(300, 200),
        selectionMode = ListSelectionModel.SINGLE_SELECTION,
    )
    private val actionText = JBTextArea().apply {
        isEditable = false
        minimumSize = Dimension(300, 200)
        // preferredSize = Dimension(10_000, 10_000)
    }
    private val actionSplitter = JBSplitter(0.5f).apply {
        firstComponent = JBScrollPane(actionSelector)
        secondComponent = JBScrollPane(actionText)
    }
    private val resultSplitter = JBSplitter(0.333f).apply {
        firstComponent = JBScrollPane(resultSelector)
        secondComponent = actionSplitter
    }
    private val attempSplitter = JBSplitter(0.25f).apply {
        firstComponent = JBScrollPane(attemptSelector)
        secondComponent = resultSplitter
    }

    private val topContent: JComponent = panel {
        verticalAlign(VerticalAlign.FILL)
        horizontalAlign(HorizontalAlign.FILL)
        row {
            cell(retryFailedButton)
            cell(retrySelectedButton)
        }
    }

    override val content = BorderLayoutPanel().apply {
        addToTop(topContent)
        addToCenter(attempSplitter)
    }

    init {
        attemptSelector.apply {
            this.preferredSize = Dimension(10_000, 10_000)
            this.addListSelectionListener {
                val selected = attemptSelector.selectedValuesList
                val results: List<CloneAttemptResult> = selected.firstOrNull()?.results ?: emptyList()
                resultSelector.setListData(results)
                resultSelector.selectLast()
                retryFailedButton.isEnabled = false
                if (selected.size == 1) {
                    if (selected.firstOrNull()?.results.orEmpty().any { !it.success }) {
                        retryFailedButton.isEnabled = true
                    }
                }
                updateTextField()
            }
        }
        resultSelector.apply {
            this.preferredSize = Dimension(10_000, 10_000)
            this.addListSelectionListener {
                val selected = resultSelector.selectedValuesList
                val actions = selected.firstOrNull()?.actions ?: emptyList()
                actionSelector.setListData(actions)
                actionSelector.selectLast()

                retrySelectedButton.isEnabled = false
                if (selected.isNotEmpty()) {
                    retrySelectedButton.isEnabled = true
                }
                updateTextField()
            }
        }
        actionSelector.apply {
            this.preferredSize = Dimension(10_000, 10_000)
            this.addListSelectionListener {
                updateTextField()
            }
        }
        retrySelectedButton.apply {
            toolTipText = "Retry the selected repos"
            addActionListener {
                val selected = resultSelector.selectedValuesList
                if (selected.isEmpty()) {
                    notificationsOperator.show("No results selected", "Unable to clone, please select at least one result", WARNING)
                } else {
                    dialogGenerator.showConfirm("Retry?", "Retry the selected repos (${selected.size})?", retrySelectedButton) {
                        retryClone(selected)
                    }
                }
            }
        }
        retryFailedButton.apply {
            toolTipText = "Retry the failed repos from the selected attempt"
            addActionListener {
                val selected = attemptSelector.selectedValuesList
                if (selected.size != 1) {
                    notificationsOperator.show("No attempt selected", "Unable to clone, please select ONE attempt", WARNING)
                } else {
                    val failed = selected[0].results.filter { !it.success }
                    if (failed.isNotEmpty()) {
                        dialogGenerator.showConfirm("Retry failed?", "Retry the failed repos (${failed.size})?", retryFailedButton) {
                            retryClone(failed)
                        }
                    } else {
                        notificationsOperator.show("Nothing failed", "Unable to clone, please select an attempt with failed clones", WARNING)
                    }
                }
            }
        }
    }

    private fun updateTextField() {
        val text = actionSelector.selectedValuesList.firstOrNull()?.how?.getFullDescription() ?: ""
        actionText.text = text
    }

    private fun retryClone(result: List<CloneAttemptResult>) {
        try {
            val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
            val clones: List<Pair<CloneAttemptResult, CloneAttemptResult?>> = uiProtector
                .mapConcurrentWithProgress("Retry clone", data = result) {
                    cloneOperator.clone(settings = settings, it.repo, it.branch, false, null)
                }
            cloneOperator.reportState(clones.filter { it.second != null }.associate { it.first.repo to it.second!! })
            refresh()
        } catch (e: Exception) {
            logger.error("Failed retry clones with exception", e)
            notificationsOperator.show("Exception cloning", "${e.javaClass.simpleName} ${e.message}, more info in IDE logs", ERROR)
        }
    }

    override fun refresh() {
        state.cloneHistory.orEmpty().toList().let { cloneHistory ->
            attemptSelector.setListData(cloneHistory)
            attemptSelector.selectLast()
            resultSelector.selectLast()
            actionSelector.selectLast()
        }
    }
}
