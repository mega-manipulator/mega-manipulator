package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.migLayout.createLayoutConstraints
import com.intellij.ui.layout.panel
import java.awt.Color
import java.io.File
import javax.swing.JButton

class ApplyWindow(private val project: Project) : ToolWindowTab {

    private val applyOperator: ApplyOperator by lazy { project.service() }
    private val onboardingOperator: OnboardingOperator by lazy { project.service() }

    private val resultList = JBList<ApplyOutput>()
    private val scrollableResult = JBScrollPane(resultList)
    private val details = JBTextArea()
    private val scrollableDetails = JBScrollPane(details)
    private val applyButton = JButton("Apply")
    private val openScriptButton = JButton("Open script")
    private val split = JBSplitter().apply {
        firstComponent = scrollableResult
        secondComponent = scrollableDetails
    }

    override val content: DialogPanel = panel {
        row {
            cell {
                component(applyButton)
                component(openScriptButton)
            }
        }
        row {
            component(split)
        }
    }

    init {

        details.isEditable = false
        resultList.addCellRenderer({
            if (it.exitCode != 0) {
                Color.ORANGE
            } else {
                null
            }
        }) { it.dir }

        applyButton.addActionListener {
            applyButton.isEnabled = false
            resultList.clearSelection()
            resultList.setListData(emptyArray())
            details.text = ""
            try {
                FileDocumentManager.getInstance().saveAllDocuments()
            } catch (e: Exception) {
                e.printStackTrace().toString()
            }
            val result = applyOperator.apply()
            resultList.setListData(result.toTypedArray())
            if (result.isNotEmpty()) {
                resultList.setSelectedValue(result.first(), true)
            }
            applyButton.isEnabled = true
            content.validate()
            content.repaint()
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
        onboardingOperator.display(OnboardingId.APPLY_TAB)
    }
}
