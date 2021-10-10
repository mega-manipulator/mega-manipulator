package com.github.jensim.megamanipulator.ui

import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.Dimension
import javax.swing.JOptionPane
import javax.swing.JOptionPane.OK_CANCEL_OPTION
import javax.swing.JOptionPane.OK_OPTION
import javax.swing.JOptionPane.QUESTION_MESSAGE

class DialogGenerator {

    fun showConfirm(title: String, message: String): Boolean {
        return try {
            when (JOptionPane.showConfirmDialog(null, message, title, OK_CANCEL_OPTION, QUESTION_MESSAGE, null)) {
                OK_OPTION -> true
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
