package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JComponent

class CloneDialogFactory(
    private val project: Project
) {

    private val prefillOperator: PrefillStringSuggestionOperator by lazy { project.service() }

    fun showCloneDialog(focusComponent: JComponent, onOk: (branch: String, shallow: Boolean, sparseDef: String?) -> Unit) {
        try {
            val ui = CloneUi(false, project)
            prefillOperator.getPrefill(PrefillString.BRANCH)?.let {
                ui.branchTextArea.text = it
            }
            openDialog(focusComponent, ui)
            ui.cloneButton.addActionListener {
                onOk(ui.branchTextArea.text, ui.shallowBox.isSelected, if (ui.sparseDefBox.isSelected) ui.sparseDefField.text else null)
                prefillOperator.addPrefill(PrefillString.BRANCH, ui.branchTextArea.text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showCloneFromPrDialog(focusComponent: JComponent, onOk: (sparseDef: String?) -> Unit) {
        try {
            val ui = CloneUi(true, project)

            openDialog(focusComponent, ui)
            ui.cloneButton.addActionListener {
                onOk(if (ui.sparseDefBox.isSelected) ui.sparseDefField.text else null)
                prefillOperator.addPrefill(PrefillString.BRANCH, ui.branchTextArea.text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openDialog(focusComponent: JComponent, ui: CloneUi) {
        try {
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val location = popupFactory.guessBestPopupLocation(focusComponent)
            val popup = popupFactory.createDialogBalloonBuilder(ui.panel, "Clone repos?")
                .setHideOnClickOutside(true)
                .createBalloon()

            ui.cloneButton.addActionListener {
                popup.hide()
            }
            ui.cancelButton.addActionListener {
                popup.hide()
            }
            popup.show(location, Balloon.Position.above)
            popup.setDefaultButton(ui.panel, ui.cloneButton)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private class CloneUi(fromPR: Boolean, project: Project) {
        val cloneButton = JButton("Clone")
        val cancelButton = JButton("Cancel")
        val shallowBox = JBCheckBox(null, false)
        val branchTextArea = JBTextField(45).apply {
            toolTipText = "Branch"
        }
        val branchHistoryButton = PrefillHistoryButton(project, PrefillString.BRANCH, branchTextArea) {
            branchTextArea.text = it
        }
        val sparseDefField = JBTextArea(3, 45).apply {
            isEnabled = false
            text = "README.md"
            toolTipText = "Sparse checkout config"
        }
        val sparseDefBox = JBCheckBox(null, false).apply {
            addActionListener {
                sparseDefField.isEnabled = isSelected
            }
        }
        val panel: DialogPanel

        init {
            panel = panel {
                if (!fromPR) {
                    row(label = "Branch") {
                        scrollCell(branchTextArea)
                        cell(branchHistoryButton)
                    }
                    row(label = "Shallow clone?") {
                        cell(shallowBox)
                    }
                }
                row(label = "Sparse clone?") {
                    cell(sparseDefBox)
                    cell(
                        JBLabel("https://git-scm.com/docs/git-sparse-checkout").apply {
                            toolTipText = "Click to open in browser"
                            addMouseListener(object : MouseListener {
                                override fun mouseClicked(e: MouseEvent?) = try {
                                    com.intellij.ide.BrowserUtil.browse("https://git-scm.com/docs/git-sparse-checkout")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                override fun mousePressed(e: MouseEvent?) = Unit
                                override fun mouseReleased(e: MouseEvent?) = Unit
                                override fun mouseEntered(e: MouseEvent?) = Unit
                                override fun mouseExited(e: MouseEvent?) = Unit
                            })
                        }
                    )
                }
                row(label = "Sparse checkout config") {
                    scrollCell(sparseDefField)
                }
                row {
                    cell(cloneButton)
                    cell(cancelButton)
                }
            }
        }
    }
}
