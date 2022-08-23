package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.MyBundle.message
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
import org.slf4j.LoggerFactory
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JButton
import javax.swing.JComponent

class CloneDialogFactory(
    private val project: Project
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val dataTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    private val prefillOperator: PrefillStringSuggestionOperator by lazy { project.service() }

    fun showCloneDialog(focusComponent: JComponent, onOk: (branch: String, shallow: Boolean, sparseDef: String?) -> Unit) {
        try {
            val ui = CloneUi(false, project)
            ui.branchTextField.text = "feature/batch_${LocalDateTime.now().format(dataTimeFormatter)}"
            openDialog(focusComponent, ui)
            ui.cloneButton.addActionListener {
                onOk(ui.branchTextField.text, ui.shallowBox.isSelected, if (ui.sparseDefBox.isSelected) ui.sparseDefField.text else null)
                prefillOperator.addPrefill(PrefillString.BRANCH, ui.branchTextField.text)
                if (ui.sparseDefBox.isSelected){
                    prefillOperator.addPrefill(PrefillString.SPARSE_HISTORY, ui.sparseDefField.text)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed opening up the Clone UI", e)
        }
    }

    fun showCloneFromPrDialog(focusComponent: JComponent, onOk: (sparseDef: String?) -> Unit) {
        try {
            val ui = CloneUi(true, project)

            openDialog(focusComponent, ui)
            ui.cloneButton.addActionListener {
                onOk(if (ui.sparseDefBox.isSelected) ui.sparseDefField.text else null)
                prefillOperator.addPrefill(PrefillString.BRANCH, ui.branchTextField.text)
            }
        } catch (e: Exception) {
            logger.error("Failed opeing the Clone UI popup for PRs", e)
        }
    }

    private fun openDialog(focusComponent: JComponent, ui: CloneUi) {
        try {
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val location = popupFactory.guessBestPopupLocation(focusComponent)
            val popup = popupFactory.createDialogBalloonBuilder(ui.panel, message("cloneDialogTitle"))
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
            logger.error("Failed creating the Clone UI popup window", e)
        }
    }

    private class CloneUi(fromPR: Boolean, project: Project) {
        private val logger = LoggerFactory.getLogger(javaClass)
        val cloneButton = JButton(message("clone"))
        val cancelButton = JButton(message("cancel"))
        val shallowBox = JBCheckBox(null, false)
        val branchTextField = JBTextField(45).apply {
            toolTipText = message("branch")
        }
        val branchHistoryButton = PrefillHistoryButton(project, PrefillString.BRANCH, branchTextField) {
            branchTextField.text = it
        }
        val sparseDefField = JBTextArea(3, 45).apply {
            isEnabled = false
            text = "README.md"
            toolTipText = message("sparseCheckoutConfig")
        }
        val sparseHistoryButton = PrefillHistoryButton(project, PrefillString.SPARSE_HISTORY, sparseDefField) {
            sparseDefField.text = it
        }

        val sparseDefBox = JBCheckBox(null, false).apply {
            addActionListener {
                sparseDefField.isEnabled = isSelected
                sparseHistoryButton.isEnabled = isSelected
            }
        }
        val panel: DialogPanel

        init {
            panel = panel {
                if (!fromPR) {
                    row(label = message("branch")) {
                        scrollCell(branchTextField)
                        cell(branchHistoryButton)
                    }
                    row(label = "${message("shallowClone")}?") {
                        cell(shallowBox)
                    }
                }
                row(label = "${message("sparseClone")}?") {
                    cell(sparseDefBox)
                    cell(
                        JBLabel(@Suppress("DialogTitleCapitalization") "https://git-scm.com/docs/git-sparse-checkout").apply {
                            toolTipText = message("clickToOpenInBrowser")
                            addMouseListener(object : MouseListener {
                                override fun mouseClicked(e: MouseEvent?) = try {
                                    com.intellij.ide.BrowserUtil.browse("https://git-scm.com/docs/git-sparse-checkout")
                                } catch (e: Exception) {
                                    logger.warn("Failed opening browser with sparse-checkout docs", e)
                                }

                                override fun mousePressed(e: MouseEvent?) = Unit
                                override fun mouseReleased(e: MouseEvent?) = Unit
                                override fun mouseEntered(e: MouseEvent?) = Unit
                                override fun mouseExited(e: MouseEvent?) = Unit
                            })
                        }
                    )
                }
                row(label = message("sparseCheckoutConfig")) {
                    scrollCell(sparseDefField)
                    cell(sparseHistoryButton)
                }
                row {
                    cell(cloneButton)
                    cell(cancelButton)
                }
            }
        }
    }
}
