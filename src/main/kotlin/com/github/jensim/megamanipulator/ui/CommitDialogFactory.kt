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

object CommitDialogFactory {

    fun openCommitDialog(
        focusComponent: JComponent,
        project: Project,
        onOk: (commitMessage: String, push: Boolean) -> Unit
    ) {
        val commitMessage = JBTextField(40)
        val pushBox = JBCheckBox("Push?")
        pushBox.isSelected = true
        val okBtn = JButton("Commit")
        val cancelBtn = JButton("Cancel")
        val panel = panel {
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
            row { component(pushBox) }
            row {
                component(okBtn)
                component(cancelBtn)
            }
        }
        val popupFactory: JBPopupFactory = try {
            JBPopupFactory.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        val balloon = popupFactory.createDialogBalloonBuilder(panel, "Commit").createBalloon()
        okBtn.addActionListener {
            balloon.hide()
            onOk(commitMessage.text, pushBox.isSelected)
        }
        cancelBtn.addActionListener {
            balloon.hide()
        }
        val location = popupFactory.guessBestPopupLocation(focusComponent)
        balloon.show(location, Balloon.Position.above)
    }
}
