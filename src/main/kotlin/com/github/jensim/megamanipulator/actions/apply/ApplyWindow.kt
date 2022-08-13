package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.onboarding.OnboardingButton
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.toolswindow.TabKey
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.github.jensim.megamanipulator.ui.TableMenu
import com.github.jensim.megamanipulator.ui.TableMenu.MenuItem
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
import javax.swing.SwingUtilities

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
    private val attemptMenu = TableMenu<List<File>>(attemptList, listOf(
        MenuItem(header = { "Retry failed (${it.size})" }, filter = {it.isNotEmpty()}) {
            dialogGenerator.showConfirm(
                title = "Rerun failed (${it.size})?",
                message = "Rerun failed in selected attempt?",
                focusComponent = attemptList
            ) {
                preApply()
                val results = applyOperator.apply(it)
                postApply(results)
            }
        }
    ))
    private val resultList = GeneralKtDataTable(
        type = ApplyOutput::class,
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
        columns = listOf(
            "Repo" to { it.dir },
        )
    ) { it.exitCode != 0 }
    private val resultMenu = TableMenu<List<File>>(resultList, listOf(
        MenuItem(header = { "Rerun selected (${it.size})" }, filter = {it.isNotEmpty()}) { selected ->
            dialogGenerator.showConfirm(
                title = "Rerun selected (${selected.size})?",
                message = "Rerun script for selected repos?",
                focusComponent = resultList
            ) {
                preApply()
                val results = applyOperator.apply(selected)
                postApply(results)
            }
        }
    ))

    private val scrollableResult = JBScrollPane(resultList)
    private val details = JBTextArea()
    private val scrollableDetails = JBScrollPane(details)
    private val applyButton = JButton("Apply", AllIcons.Toolwindows.ToolWindowProblems)
    private val openScriptButton = JButton("Open script")
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
                resultList.clearSelection()
                resultList.setListData(emptyList())
                attemptList.selectedValuesList.firstOrNull()?.let { attempt ->
                    resultList.setListData(attempt.result)
                    resultList.selectFirst()
                }
            }
            addClickListener { mouseEvent, _: ApplyAttempt? ->
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    val attempts = attemptList.selectedValuesList
                    val failed = attempts.firstOrNull()?.result?.filter { it.exitCode != 0 }
                        ?.map { File(project.basePath, it.dir) }.orEmpty()
                    attemptMenu.show(mouseEvent, failed)
                }
            }
        }
        resultList.apply {
            addListSelectionListener {
                val selected: List<ApplyOutput> = resultList.selectedValuesList
                if (selected.size == 1) {
                    details.text = selected.firstOrNull()?.getFullDescription() ?: ""
                } else {
                    details.text = ""
                }
            }
            addClickListener { mouseEvent, _:ApplyOutput? ->
                if(SwingUtilities.isRightMouseButton(mouseEvent)){
                    val selected = resultList.selectedValuesList
                        .map { File(project.basePath, it.dir) }
                    resultMenu.show(mouseEvent, selected)
                }
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
