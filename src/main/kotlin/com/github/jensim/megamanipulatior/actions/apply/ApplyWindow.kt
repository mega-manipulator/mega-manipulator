package com.github.jensim.megamanipulatior.actions.apply

import com.github.jensim.megamanipulatior.settings.SettingsFileOperator.objectMapper
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import javax.swing.JButton
import javax.swing.JSplitPane
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object ApplyWindow {

    private val resultList = JBList<ApplyOutput>()
    private val scrollableResult = JBScrollPane(resultList)
    private val details = JBTextArea()
    private val scrollableDetails = JBScrollPane(details)
    private val button = JButton("Apply")
    private val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollableResult, scrollableDetails)
    val content = panel {
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
                details.text = objectMapper.writeValueAsString(it)
            }
        }
    }
}