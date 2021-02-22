package com.github.jensim.megamanipulatior.actions.apply

import com.github.jensim.megamanipulatior.toolswindow.ToolWindowTab
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import javax.swing.JButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
            GlobalScope.launch {
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
        // TODO("not implemented")
    }

    override val index: Int = 2
}
