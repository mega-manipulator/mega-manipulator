package com.github.jensim.megamanipulator.ui

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

object DialogGenerator {

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
        prefill: String? = null,
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
            val scrollPane = JBScrollPane(field)
            if (prefill != null) {
                field.text = prefill
            }
            val panel = panel {
                row {
                    label(message)
                }
                row {
                    component(scrollPane)
                }
                row {
                    component(btnYes)
                    component(btnNo)
                }
            }
            val popup = popupFactory.createDialogBalloonBuilder(panel, title)
                .createBalloon()
            btnYes.addActionListener {
                popup.hide()
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
