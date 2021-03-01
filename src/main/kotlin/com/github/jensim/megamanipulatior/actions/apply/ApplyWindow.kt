package com.github.jensim.megamanipulatior.actions.apply

import com.github.jensim.megamanipulatior.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulatior.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulatior.ui.uiProtectedOperation
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.Color
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
            uiProtectedOperation(title = "Applying changes") {
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
        }
        resultList.addListSelectionListener {
            resultList.selectedValuesList.firstOrNull()?.let {
                details.text = it.getFullDescription()
            }
        }
    }

    override fun refresh() {
        // not implemented
        // Not needed?
    }

    override val index: Int = 2
}
