package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JButton
import javax.swing.JComponent

open class CreatePullRequestDialog(
    private val yesText: String = "Create PRs",
    private val title: String = "Create pull request",
) {
    private val titleField = JBTextField(30)
    protected val titlePane = JBScrollPane(titleField)
    private val descriptionField = JBTextArea(5, 30)
    protected val descriptionPane = JBScrollPane(descriptionField)

    protected val okButton = JButton()
    protected val cancelButton = JButton("Cancel")

    protected open val panel: DialogPanel = panel {
        row(label = "PR Title") { component(titlePane) }
        row(label = "PR Description") { component(descriptionPane) }
        row {
            component(okButton)
            component(cancelButton)
        }
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

    fun show(
        focusComponent: JComponent,
        onNo: () -> Unit = {},
        onYes: (title: String, description: String) -> Unit
    ) {
        try {
            okButton.text = yesText
            PrefillStringSuggestionOperator.getPrefill(PrefillString.PR_TITLE)?.let { title ->
                titleField.text = title
            }
            PrefillStringSuggestionOperator.getPrefill(PrefillString.PR_BODY)?.let { body ->
                descriptionField.text = body
            }
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val popup = popupFactory.createDialogBalloonBuilder(panel, title).createBalloon()
            okButton.addActionListener {
                PrefillStringSuggestionOperator.setPrefill(PrefillString.PR_TITLE, titleField.text)
                PrefillStringSuggestionOperator.setPrefill(PrefillString.PR_BODY, descriptionField.text)
                popup.hide()
                onYes(titleField.text, descriptionField.text)
            }
            cancelButton.addActionListener {
                popup.hide()
                onNo()
            }

            val location = popupFactory.guessBestPopupLocation(focusComponent)
            popup.show(location, Balloon.Position.above)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
