package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import org.slf4j.LoggerFactory
import javax.swing.JButton
import javax.swing.JComponent

open class CreatePullRequestDialog(
    private val yesText: String = "Create PRs",
    private val title: String = "Create pull request",
    private val project: Project,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val prefillOperator: PrefillStringSuggestionOperator by lazy { project.service() }
    private val titleField = JBTextField(30)
    private val titlePane = JBScrollPane(titleField)
    protected val titlePanel = panel {
        group("PR Title") {
            row {
                cell(titlePane)
                cell(
                    PrefillHistoryButton(project, PrefillString.PR_TITLE, titlePane) {
                        titleField.text = it
                    }
                )
            }
        }
    }
    private val descriptionField = JBTextArea(5, 30)
    private val descriptionPane = JBScrollPane(descriptionField)
    protected val descriptionPanel = panel {
        group("PR Description") {
            row {
                cell(descriptionPane)
                cell(
                    PrefillHistoryButton(project, PrefillString.PR_BODY, descriptionPane) {
                        descriptionField.text = it
                    }
                )
            }
        }
    }

    private val okButton = JButton()
    private val cancelButton = JButton("Cancel")
    protected val buttonPanel = panel {
        row {
            cell(okButton)
            cell(cancelButton)
        }
    }

    protected open val panel: DialogPanel = panel {
        row {
            cell(titlePanel)
        }
        row {
            cell(descriptionPanel)
        }
        row {
            cell(buttonPanel)
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
            prefillOperator.getPrefill(PrefillString.PR_TITLE)?.let { title ->
                titleField.text = title
            }
            prefillOperator.getPrefill(PrefillString.PR_BODY)?.let { body ->
                descriptionField.text = body
            }
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val popup = popupFactory.createDialogBalloonBuilder(panel, title).createBalloon()
            okButton.addActionListener {
                prefillOperator.addPrefill(PrefillString.PR_TITLE, titleField.text)
                prefillOperator.addPrefill(PrefillString.PR_BODY, descriptionField.text)
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
            logger.error("Failed opening pull request creation dialog", e)
        }
    }
}
