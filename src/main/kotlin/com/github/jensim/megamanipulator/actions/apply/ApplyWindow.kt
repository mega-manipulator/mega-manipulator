package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.onboarding.OnboardingButton
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
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
import java.awt.Dimension
import java.io.File
import javax.swing.JButton

class ApplyWindow(private val project: Project) : ToolWindowTab {

    private val applyOperator: ApplyOperator by lazy { project.service() }
    private val onboardingOperator: OnboardingOperator by lazy { project.service() }
    private val dialogGenerator: DialogGenerator by lazy { project.service() }

    private val resultList = GeneralKtDataTable(
        ApplyOutput::class,
        listOf(
            "Directory" to { it.dir },
            "Exit code" to { it.exitCode.toString() },
            "Output (last line)" to { it.lastLine },
        )
    ) { it.exitCode != 0 }
    private val scrollableResult = JBScrollPane(resultList)
    private val details = JBTextArea()
    private val scrollableDetails = JBScrollPane(details)
    private val applyButton = JButton("Apply", AllIcons.Toolwindows.ToolWindowProblems)
    private val openScriptButton = JButton("Open script")
    private val split = JBSplitter(0.7f).apply {
        isLightweight
        firstComponent = scrollableResult
        secondComponent = scrollableDetails
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
        scrollableResult.preferredSize = Dimension(4000, 1000)
        scrollableDetails.preferredSize = Dimension(4000, 1000)

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
                applyButton.isEnabled = false
                resultList.clearSelection()
                details.text = ""
                try {
                    FileDocumentManager.getInstance().saveAllDocuments()
                } catch (e: Exception) {
                    e.printStackTrace().toString()
                }
                val result = applyOperator.apply()
                resultList.setListData(result)
                if (result.isNotEmpty()) {
                    resultList.selectFirst()
                }
                applyButton.isEnabled = true
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
        resultList.addListSelectionListener {
            val selected = resultList.selectedValuesList
            if (selected.isNotEmpty()) {
                details.text = selected.first().getFullDescription()
            } else {
                details.text = ""
            }
        }
    }

    override fun refresh() {
        applyButton.isEnabled = project.basePath?.let { File(it) }?.list()?.isNotEmpty() == true

        onboardingOperator.registerTarget(OnboardingId.APPLY_TAB, content)
        onboardingOperator.registerTarget(OnboardingId.APPLY_BUTTON, applyButton)
        onboardingOperator.registerTarget(OnboardingId.APPLY_SCRIPT_OPEN_BUTTON, openScriptButton)
        onboardingOperator.registerTarget(OnboardingId.APPLY_RESULT_AREA, split)
    }
}
