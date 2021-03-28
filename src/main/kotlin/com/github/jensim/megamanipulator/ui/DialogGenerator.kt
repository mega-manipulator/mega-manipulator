package com.github.jensim.megamanipulator.ui

import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JOptionPane
import javax.swing.JOptionPane.OK_CANCEL_OPTION
import javax.swing.JOptionPane.OK_OPTION
import javax.swing.JOptionPane.QUESTION_MESSAGE

object DialogGenerator {

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

    fun askForInput(title: String, message: String, values: List<String>, onCancel: () -> Unit = {}, messageType: Int = QUESTION_MESSAGE, optionType: Int = OK_CANCEL_OPTION, onOk: (Map<String, String>) -> Unit) {
        try {
            val inputs = values.map {
                Pair(it, JBTextField())
            }.toMap()
            val panel = panel {
                row {
                    label(message)
                }
                inputs.forEach { (k, v) ->
                    row {
                        label(k)
                        component(v)
                    }
                }
            }
            when (JOptionPane.showConfirmDialog(null, panel, title, optionType, messageType, null)) {
                OK_OPTION -> onOk(inputs.mapValues { it.value.text })
                else -> onCancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
