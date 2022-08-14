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
import com.github.jensim.megamanipulator.ui.TableMenu
import com.github.jensim.megamanipulator.ui.TableMenu.MenuItem
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.components.BorderLayoutPanel
import org.slf4j.LoggerFactory
import java.awt.Dimension
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

class CloneHistoryWindow(val project: Project) : ToolWindowTab {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val state: MegaManipulatorSettingsState by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val cloneOperator: CloneOperator by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }
    private val dialogGenerator: DialogGenerator by lazy { project.service() }

    private val attemptSelector = GeneralKtDataTable(
        type = CloneAttempt::class,
        columns = listOf("Time" to { it.time.toString() }),
        colorizer = { it.results.any { repo -> !repo.success } },
        minSize = Dimension(500, 200),
        selectionMode = ListSelectionModel.SINGLE_SELECTION,
    )
    private val attemptMenu = TableMenu<CloneAttempt?>(
        attemptSelector,
        listOf(
            MenuItem(
                header = { if (it?.results?.any { !it.success } == true) "Retry failed" else "Nothing to retry" },
                isEnabled = { it?.results?.any { !it.success } == true }
            ) {
                dialogGenerator.showConfirm(
                    title = "Retry?",
                    message = "Retry failed clones (${it?.results?.count { !it.success } ?: 0}/${it?.results?.size ?: 0})?",
                    focusComponent = attemptSelector
                ) {
                    retryClone(it?.results?.filter { !it.success }.orEmpty())
                }
            }
        )
    )
    private val resultSelector = GeneralKtDataTable(
        type = CloneAttemptResult::class,
        columns = listOf("Repo" to { it.repo.asPathString() }),
        colorizer = { !it.success },
        minSize = Dimension(300, 200),
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
    )
    private val resultMenu = TableMenu<List<CloneAttemptResult>>(
        resultSelector,
        listOf(
            MenuItem(
                header = { if (it.isEmpty()) "Nothing selected" else "Retry" },
                isEnabled = { it.isNotEmpty() }
            ) {
                dialogGenerator.showConfirm("Retry?", "Retry the selected repos (${it.size})?", resultSelector) {
                    retryClone(it)
                }
            }
        )
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
    }
    private val actionSplitter = JBSplitter(0.5f).apply {
        firstComponent = JBScrollPane(actionSelector)
        secondComponent = JBScrollPane(actionText)
    }
    private val resultSplitter = JBSplitter(0.333f).apply {
        firstComponent = JBScrollPane(resultSelector)
        secondComponent = actionSplitter
    }
    private val attemptSplitter = JBSplitter(0.25f).apply {
        firstComponent = JBScrollPane(attemptSelector)
        secondComponent = resultSplitter
    }

    override val content = BorderLayoutPanel().apply {
        addToCenter(attemptSplitter)
    }

    init {
        attemptSelector.apply {
            this.addListSelectionListener {
                val selected = attemptSelector.selectedValuesList
                val results: List<CloneAttemptResult> = selected.firstOrNull()?.results ?: emptyList()
                resultSelector.clearSelection()
                resultSelector.setListData(results)
                resultSelector.selectLast()
                updateTextField()
            }
            this.addClickListener { mouseEvent, _ ->
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    val selected = attemptSelector.selectedValuesList.firstOrNull()
                    attemptMenu.show(mouseEvent, selected)
                }
            }
        }
        resultSelector.apply {
            this.addListSelectionListener {
                val selected = resultSelector.selectedValuesList
                val actions = selected.firstOrNull()?.actions ?: emptyList()
                actionSelector.clearSelection()
                actionSelector.setListData(actions)
                actionSelector.selectLast()
                updateTextField()
            }
            this.addClickListener { mouseEvent, _ ->
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    val selected = resultSelector.selectedValuesList
                    resultMenu.show(mouseEvent, selected)
                }
            }
        }
        actionSelector.apply {
            this.addListSelectionListener {
                updateTextField()
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
