package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.settings.ProjectOperator.project
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.uiProtectedOperation
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.Color
import java.io.File
import javax.swing.JButton

object ApplyWindow : ToolWindowTab {

    private val resultList = JBList<ApplyOutput>()
    private val scrollableResult = JBScrollPane(resultList)
    private val details = JBTextArea()
    private val scrollableDetails = JBScrollPane(details)
    private val button = JButton("Apply")
    override val content: DialogPanel = panel {
        row {
            component(button)
        }
        row {
            component(scrollableResult)
            component(scrollableDetails)
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
            val result = ApplyOperator.apply()
            resultList.setListData(result.toTypedArray())
            button.isEnabled = true
        }
        resultList.addListSelectionListener {
            resultList.selectedValuesList.firstOrNull()?.let {
                details.text = it.getFullDescription()
            }
        }
    }

    override fun refresh() {
        button.isEnabled = project?.basePath?.let { File(it) }?.list()?.isNotEmpty() == true
    }

    override val index: Int = 2
}
