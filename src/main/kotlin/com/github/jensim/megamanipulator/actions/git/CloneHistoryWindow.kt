package com.github.jensim.megamanipulator.actions.git

import com.github.jensim.megamanipulator.actions.git.clone.CloneAttempt
import com.github.jensim.megamanipulator.actions.git.clone.CloneAttemptResult
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.github.jensim.megamanipulator.ui.groupPanel
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import java.awt.Dimension
import javax.swing.JComponent

class CloneHistoryWindow(val project: Project) : ToolWindowTab {

    private val state: MegaManipulatorSettingsState by lazy { project.service() }
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
                val results: List<CloneAttemptResult> = attemptSelector.selectedValuesList.firstOrNull()?.results ?: emptyList()
                resultSelector.setListData(results)
                resultSelector.selectFirst()
            }
        }
        resultSelector.apply {
            this.preferredSize = Dimension(10_000, 10_000)
            this.addListSelectionListener {
                val actions = resultSelector.selectedValuesList.firstOrNull()?.actions ?: emptyList()
                actionSelector.setListData(actions)
                actionSelector.selectFirst()
            }
        }
        actionSelector.apply {
            this.preferredSize = Dimension(10_000, 10_000)
            this.addListSelectionListener {
                val text = actionSelector.selectedValuesList.firstOrNull()?.how?.getFullDescription() ?: ""
                actionText.text = text
            }
        }
    }

    override fun refresh() {
        state.cloneHistory.toList().let { cloneHistory ->
            attemptSelector.setListData(cloneHistory)
            attemptSelector.selectFirst()
        }
    }
}
