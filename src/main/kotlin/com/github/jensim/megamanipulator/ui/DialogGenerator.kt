package com.github.jensim.megamanipulator.ui

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JOptionPane.OK_CANCEL_OPTION
import javax.swing.JOptionPane.OK_OPTION
import javax.swing.JOptionPane.QUESTION_MESSAGE

class DialogGenerator {

    fun showConfirm(
        title: String,
        focusComponent: JComponent? = null,
        yesText: String = "Yes",
        noText: String = "No",
        onNo: () -> Unit = {},
        onYes: () -> Unit
    ) {
        try {
            val popupFactory: JBPopupFactory = try {
                JBPopupFactory.getInstance()
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
            val popup = popupFactory.createConfirmation(title, yesText, noText, onYes, onNo, 0)
            focusComponent?.let { component ->
                val location = popupFactory.guessBestPopupLocation(component)
                popup.show(location)
            } ?: popup.showInFocusCenter()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun askForInput(title: String, message: String, prefill: String? = null): String? {
        return try {
            val field = JBTextArea().apply {
                minimumSize = Dimension(500, 300)
                if (prefill != null) {
                    text = prefill
                }
            }
            val panel = panel {
                row {
                    label(message)
                    component(field)
                }
            }
            when (JOptionPane.showConfirmDialog(null, panel, title, OK_CANCEL_OPTION, QUESTION_MESSAGE, null)) {
                OK_OPTION -> field.text
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
