package com.github.jensim.megamanipulatior.ui

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JOptionPane
import javax.swing.JOptionPane.OK_CANCEL_OPTION
import javax.swing.JOptionPane.OK_OPTION
import javax.swing.JOptionPane.QUESTION_MESSAGE

open class CreatePullRequestDialog {
    protected val titleField = JBTextField(30)
    private val descriptionField = JBTextArea(5, 30)
    protected val descriptionScrollArea = JBScrollPane(descriptionField)
    protected open val title = "Create pull request"
    protected open val panel: DialogPanel = panel {
        row(label = "PR Title") { component(titleField) }
        row(label = "PR Description") { component(descriptionScrollArea) }
    }

    var prTitle: String?
        get() = titleField.text
        set(value) {
            titleField.text = value
        }
    var prDescription: String?
        get() = descriptionField.text
        set(value) {
            descriptionField.text = value
        }

    private fun show(): Int {
        return JOptionPane.showConfirmDialog(null, panel, title, OK_CANCEL_OPTION, QUESTION_MESSAGE, null)
    }

    fun showAndGet(): Boolean = show() == OK_OPTION && !prTitle.isNullOrBlank() && !prDescription.isNullOrBlank()
}
