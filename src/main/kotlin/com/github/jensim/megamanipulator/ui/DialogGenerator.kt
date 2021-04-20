package com.github.jensim.megamanipulator.ui

import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.Dimension
import javax.swing.JOptionPane
import javax.swing.JOptionPane.OK_CANCEL_OPTION
import javax.swing.JOptionPane.OK_OPTION
import javax.swing.JOptionPane.QUESTION_MESSAGE

class DialogGenerator {

    companion object {

        val instance by lazy {
            DialogGenerator()
        }
    }

    fun showConfirm(title: String, message: String, onCancel: () -> Unit = {}, messageType: Int = QUESTION_MESSAGE, optionType: Int = OK_CANCEL_OPTION, onOk: () -> Unit) {
        try {
            when (JOptionPane.showConfirmDialog(null, message, title, optionType, messageType, null)) {
                OK_OPTION -> onOk()
                else -> onCancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun askForInput(title: String, message: String, onCancel: () -> Unit = {}, messageType: Int = QUESTION_MESSAGE, optionType: Int = OK_CANCEL_OPTION, onOk: (String) -> Unit) {
        try {
            val field = JBTextArea().apply {
                minimumSize = Dimension(500, 300)
            }
            val panel = panel {
                row {
                    label(message)
                    component(field)
                }
            }
            when (JOptionPane.showConfirmDialog(null, panel, title, optionType, messageType, null)) {
                OK_OPTION -> onOk(field.text ?: "")
                else -> onCancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
