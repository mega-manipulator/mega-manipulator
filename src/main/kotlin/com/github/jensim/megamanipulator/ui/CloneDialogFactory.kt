package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JButton
import javax.swing.JComponent

class CloneDialogFactory(
    project: Project
) {

    private val prefillOperator: PrefillStringSuggestionOperator by lazy { project.service() }

    fun show(focusComponent: JComponent, onOk: (branch: String, shallow: Boolean) -> Unit) {
        try {
            val cloneButton = JButton("Clone")
            val cancelButton = JButton("Cancel")
            val shallowBox = JBCheckBox(null, true)
            val branchTextArea = JBTextField(65)
            val panel: DialogPanel by lazy {
                panel {
                    row {
                        label("Branch")
                        scrollPane(branchTextArea)
                    }
                    row {
                        label("Shallow clone?")
                        component(shallowBox)
                    }
                    row {
                        buttonGroup {
                            component(cloneButton)
                            component(cancelButton)
                        }
                    }
                }
            }
            branchTextArea.toolTipText = "Branch"
            prefillOperator.getPrefill(PrefillString.BRANCH)?.let {
                branchTextArea.text = it
            }
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val location = popupFactory.guessBestPopupLocation(focusComponent)
            val popup = popupFactory.createDialogBalloonBuilder(panel, "Clone repos?")
                .setHideOnClickOutside(true)
                .createBalloon()

            cloneButton.addActionListener {
                popup.hide()
                onOk(branchTextArea.text, shallowBox.isSelected)
                prefillOperator.setPrefill(PrefillString.BRANCH, branchTextArea.text)
            }
            cancelButton.addActionListener {
                popup.hide()
            }
            popup.show(location, Balloon.Position.above)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
