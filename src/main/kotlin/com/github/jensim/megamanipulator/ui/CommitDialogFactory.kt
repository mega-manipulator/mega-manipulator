package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.project.PrefillString
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JButton
import javax.swing.JComponent

class CommitDialogFactory(project: Project) {

    private val commitMessage = JBTextField(40)
    private val forceBox = JBCheckBox("Force push?")
    private val pushBox = JBCheckBox("Push?").apply {
        isSelected = true
        addActionListener {
            if (!isSelected) {
                forceBox.isSelected = false
            }
            forceBox.isEnabled = isSelected
        }
    }
    private val okBtn = JButton("Commit")
    private val cancelBtn = JButton("Cancel")
    private val panel = panel {
        row {
            label("Create commits for all changes in all checked out repositories")
        }
        row {
            cell {
                scrollPane(commitMessage)
                component(
                    PrefillHistoryButton(project, PrefillString.COMMIT_MESSAGE, commitMessage) {
                        commitMessage.text = it
                    }
                )
            }
        }
        row {
            component(pushBox)
            component(forceBox)
        }
        row {
            component(okBtn)
            component(cancelBtn)
        }
    }

    fun openCommitDialog(
        focusComponent: JComponent,
        onOk: (commitMessage: String, push: Boolean, force:Boolean) -> Unit
    ) {
        val popupFactory: JBPopupFactory = try {
            JBPopupFactory.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        val balloon = popupFactory.createDialogBalloonBuilder(panel, "Commit").createBalloon()
        okBtn.addActionListener {
            balloon.hide()
            onOk(commitMessage.text, pushBox.isSelected, pushBox.isSelected && forceBox.isSelected)
        }
        cancelBtn.addActionListener {
            balloon.hide()
        }
        val location = popupFactory.guessBestPopupLocation(focusComponent)
        balloon.show(location, Balloon.Position.above)
    }
}
