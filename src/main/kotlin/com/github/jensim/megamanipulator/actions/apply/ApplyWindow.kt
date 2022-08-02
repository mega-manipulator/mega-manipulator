package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.onboarding.OnboardingButton
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.toolswindow.TabKey
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign.RIGHT
import com.intellij.util.ui.components.BorderLayoutPanel
import org.slf4j.LoggerFactory
import java.io.File
import javax.swing.JButton
import javax.swing.ListSelectionModel

class ApplyWindow(private val project: Project) : ToolWindowTab {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val megaManipulatorSettingsState: MegaManipulatorSettingsState by lazy { project.service() }
    private val applyOperator: ApplyOperator by lazy { project.service() }
    private val onboardingOperator: OnboardingOperator by lazy { project.service() }
    private val dialogGenerator: DialogGenerator by lazy { project.service() }

    private val attemptList = GeneralKtDataTable(
        type = ApplyAttempt::class,
        selectionMode = ListSelectionModel.SINGLE_SELECTION,
        columns = listOf("Attempt" to { it.time.toString() }),
    ) { it.result.isEmpty() || it.result.any { it.exitCode != 0 } }
    private val resultList = GeneralKtDataTable(
        type = ApplyOutput::class,
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
        columns = listOf(
            "Repo" to { it.dir },
            // "Exit code" to { it.exitCode.toString() },
            // "Output (last line)" to { it.lastLine },
        )
    ) { it.exitCode != 0 }
    private val scrollableResult = JBScrollPane(resultList)
    private val details = JBTextArea()
    private val scrollableDetails = JBScrollPane(details)
    private val applyButton = JButton("Apply", AllIcons.Toolwindows.ToolWindowProblems)
    private val openScriptButton = JButton("Open script")
    private val rerunSelectedButton = JButton("Rerun selected repos")
    private val rerunFailedButton = JButton("Rerun failed")
    private val splitRight = JBSplitter(0.5f).apply {
        firstComponent = scrollableResult
        secondComponent = scrollableDetails
    }
    private val split = JBSplitter(0.3f).apply {
        firstComponent = JBScrollPane(attemptList)
        secondComponent = splitRight
    }

    private val topContent: DialogPanel = panel {
        row {
            cell(applyButton)
            cell(openScriptButton)
            cell(rerunFailedButton)
            cell(rerunSelectedButton)

            cell(OnboardingButton(project, TabKey.tabTitleApply, OnboardingId.APPLY_TAB))
                .horizontalAlign(RIGHT)
        }
    }
    override val content = BorderLayoutPanel().apply {
        addToTop(topContent)
        addToCenter(split)
    }

    init {
        details.isEditable = false

        applyButton.addActionListener {
            dialogGenerator.showConfirm(
                title = "Apply change script?",
                message = """
                    <h1>Are you sure?</h1>
                    <b>I wont be able to stop scripts that have started.</b>
                    Take a second peek at your concurrency level before you run something intensive.
                """.trimIndent(),
                focusComponent = applyButton
            ) {
                preApply()
                val results: List<ApplyOutput> = applyOperator.apply()
                postApply(results)
                content.validate()
                content.repaint()
            }
        }
        rerunSelectedButton.apply {
            toolTipText = "Rerun selected repos with the new script content (NOT THE OLD)"
            isEnabled = false
            addActionListener {
                val selected = resultList.selectedValuesList
                    .map { File(project.basePath, it.dir) }
                if (selected.isNotEmpty()) {
                    dialogGenerator.showConfirm(
                        title = "Rerun selected (${selected.size})?",
                        message = "Rerun script for selected repos?",
                        focusComponent = rerunSelectedButton
                    ) {
                        preApply()
                        val results = applyOperator.apply(selected)
                        postApply(results)
                    }
                }
            }
        }
        rerunFailedButton.apply {
            toolTipText = "Rerun failed in selected attempt with the new script content (NOT THE OLD)"
            isEnabled = false
            addActionListener {
                val attempts = attemptList.selectedValuesList
                if (attempts.size == 1) {
                    val failed = attempts[0].result.filter { it.exitCode != 0 }
                        .map { File(project.basePath, it.dir) }
                    if (failed.isNotEmpty()) {
                        dialogGenerator.showConfirm(
                            title = "Rerun failed (${failed.size})?",
                            message = "Rerun failed in selected attempt?",
                            focusComponent = rerunSelectedButton
                        ) {
                            preApply()
                            val results = applyOperator.apply(failed)
                            postApply(results)
                        }
                    }
                }
            }
        }

        openScriptButton.addActionListener {
            VirtualFileManager.getInstance().let {
                it.findFileByNioPath(File("${project.basePath}/config/mega-manipulator.bash").toPath())
                    ?.let { file: VirtualFile ->
                        FileEditorManager.getInstance(project).openFile(file, true)
                    }
            }
        }

        attemptList.apply {
            addListSelectionListener {
                rerunFailedButton.isEnabled = false
                resultList.clearSelection()
                resultList.setListData(emptyList())
                attemptList.selectedValuesList.firstOrNull()?.let { attempt ->
                    resultList.setListData(attempt.result)
                    resultList.selectFirst()
                    if (attempt.result.any { it.exitCode != 0 }) {
                        rerunFailedButton.isEnabled = true
                    }
                }
            }
        }
        resultList.addListSelectionListener {
            val selected: List<ApplyOutput> = resultList.selectedValuesList
            rerunSelectedButton.isEnabled = selected.isNotEmpty()
            if (selected.size == 1) {
                details.text = selected.first().getFullDescription()
            } else {
                details.text = ""
            }
        }
    }

    private fun preApply() {
        try {
            FileDocumentManager.getInstance().saveAllDocuments()
        } catch (e: Exception) {
            logger.warn("Failed to save documents prior to applying scripted changes", e)
        }
        applyButton.isEnabled = false
        details.text = ""
        resultList.clearSelection()
        attemptList.clearSelection()
    }

    private fun postApply(results: List<ApplyOutput>) {
        megaManipulatorSettingsState.addApplyAttempt(ApplyAttempt(results))
        attemptList.setListData(megaManipulatorSettingsState.applyHistory.orEmpty())
        attemptList.selectLast()
        applyButton.isEnabled = true
    }

    override fun refresh() {
        applyButton.isEnabled = project.basePath?.let { File(it) }?.list()?.isNotEmpty() == true

        if (attemptList.model.rowCount == 0) {
            val items: List<ApplyAttempt> = megaManipulatorSettingsState.applyHistory.orEmpty()
            attemptList.setListData(items)
            attemptList.selectLast()
        }

        onboardingOperator.registerTarget(OnboardingId.APPLY_TAB, content)
        onboardingOperator.registerTarget(OnboardingId.APPLY_BUTTON, applyButton)
        onboardingOperator.registerTarget(OnboardingId.APPLY_SCRIPT_OPEN_BUTTON, openScriptButton)
        onboardingOperator.registerTarget(OnboardingId.APPLY_RESULT_AREA, splitRight)
    }
}
