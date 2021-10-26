package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.text.JTextComponent

class DialogGenerator(private val project: Project) {

    private val prefillOperator: PrefillStringSuggestionOperator by lazy { project.service() }

    fun showConfirm(
        title: String,
        message: String,
        focusComponent: JComponent,
        position: Balloon.Position = Balloon.Position.above,
        convertMultiLine: Boolean = true,
        yesText: String = "Yes",
        noText: String = "No",
        onNo: () -> Unit = {},
        onYes: () -> Unit,
    ) {
        try {
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val yesBtn = JButton(yesText)
            val noBtn = JButton(noText)
            val message1 = if (convertMultiLine) message.convertMultiLineToHtml() else message
            val panel = panel {
                row {
                    component(JBScrollPane(JBLabel(message1)))
                }
                row {
                    component(yesBtn)
                    component(noBtn)
                }
            }
            val popup = popupFactory.createDialogBalloonBuilder(panel, title)
                .setHideOnClickOutside(true)
                .createBalloon()
            yesBtn.addActionListener {
                popup.hide()
                onYes()
            }
            noBtn.addActionListener {
                popup.hide()
                onNo()
            }
            val location = popupFactory.guessBestPopupLocation(focusComponent)
            popup.show(location, position)
            yesBtn.requestFocus(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun String.convertMultiLineToHtml() = "<html>${replace("\n", "<br>\n")}</html>"

    fun askForInput(
        title: String,
        message: String,
        prefill: PrefillString?,
        field: JTextComponent = JBTextField(),
        focusComponent: JComponent,
        position: Balloon.Position = Balloon.Position.above,
        yesText: String = "Ok",
        noText: String = "Cancel",
        onNo: () -> Unit = {},
        onYes: (String) -> Unit
    ) {
        try {
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val btnYes = JButton(yesText)
            val btnNo = JButton(noText)
            val prefillButton = prefill?.let { pre ->
                PrefillHistoryButton(project, pre, field) {
                    field.text = it
                }
            }
            val panel = panel {
                row {
                    label(message)
                }
                row {
                    cell {
                        scrollPane(field)
                        prefillButton?.let {
                            component(it)
                        }
                    }
                }
                row {
                    cell {
                        component(btnYes)
                        component(btnNo)
                    }
                }
            }
            val popup = popupFactory.createDialogBalloonBuilder(panel, title)
                .createBalloon()
            btnYes.addActionListener {
                popup.hide()
                prefill?.let {
                    prefillOperator.addPrefill(it, field.text)
                }
                onYes(field.text)
            }
            btnNo.addActionListener {
                popup.hide()
                onNo()
            }
            val location: RelativePoint = popupFactory.guessBestPopupLocation(focusComponent)

            popup.show(location, position)
            field.requestFocus(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
