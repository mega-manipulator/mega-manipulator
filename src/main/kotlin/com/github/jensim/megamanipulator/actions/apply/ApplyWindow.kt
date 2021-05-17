package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.settings.passwords.ProjectOperator
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.Color
import java.io.File
import javax.swing.JButton

class ApplyWindow(
    private val applyOperator: ApplyOperator,
    private val projectOperator: ProjectOperator,
) : ToolWindowTab {

    private val resultList = JBList<ApplyOutput>()
    private val scrollableResult = JBScrollPane(resultList)
    private val details = JBTextArea()
    private val scrollableDetails = JBScrollPane(details)
    private val button = JButton("Apply")
    private val split = JBSplitter().apply {
        firstComponent = scrollableResult
        secondComponent = scrollableDetails
    }

    override val content: DialogPanel = panel {
        row {
            component(button)
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
        button.addActionListener {
            button.isEnabled = false
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
            button.isEnabled = true
            content.validate()
            content.repaint()
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
        button.isEnabled = projectOperator.project.basePath?.let { File(it) }?.list()?.isNotEmpty() == true
    }

    override val index: Int = 2
}
