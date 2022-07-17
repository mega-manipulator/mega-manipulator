package com.github.jensim.megamanipulator.ui

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import javax.swing.JButton
import javax.swing.JComponent

object ClosePRDialogFactory {

    fun openCommitDialog(
        relativeComponent: JComponent,
        onOk: (removeBranches: Boolean, removeStaleForks: Boolean) -> Unit
    ) {
        try {
            val pruneBranchBox = JBCheckBox("Delete source branches?").apply {
                isSelected = true
            }
            val pruneForkBox = JBCheckBox("Delete forks?").apply {
                isSelected = false
            }
            val okBtn = JButton("Close PRs")
            val cancelBtn = JButton("Cancel")
            val panel = panel {
                row {
                    label(
                        """
                        <html>
                        <h3>No undo path available I'm afraid..</h3>
                        <h3>Decline selected PRs?</h3>
                        </html>
                        """.trimIndent()
                    )
                }
                row { cell(pruneBranchBox) }
                row { cell(pruneForkBox) }
                row {
                    cell(okBtn)
                    cell(cancelBtn)
                }
            }
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val balloon = popupFactory.createDialogBalloonBuilder(panel, "Close PRs").createBalloon()
            okBtn.addActionListener {
                balloon.hide()
                onOk(pruneBranchBox.isSelected, pruneForkBox.isSelected)
            }
            cancelBtn.addActionListener {
                balloon.hide()
            }
            val location = popupFactory.guessBestPopupLocation(relativeComponent)
            balloon.show(location, Balloon.Position.above)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
