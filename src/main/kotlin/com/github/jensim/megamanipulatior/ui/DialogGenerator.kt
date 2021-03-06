package com.github.jensim.megamanipulatior.ui

import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JOptionPane.OK_CANCEL_OPTION
import javax.swing.JOptionPane.OK_OPTION
import javax.swing.JOptionPane.QUESTION_MESSAGE
import javax.swing.text.JTextComponent

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

    fun askForInputs(title: String, message: String, values: List<Pair<String, JTextComponent>>, preferredSize: Dimension = Dimension(500, 400), onCancel: () -> Unit = {}, messageType: Int = QUESTION_MESSAGE, optionType: Int = OK_CANCEL_OPTION, onOk: (Map<String, String>) -> Unit) {
        try {
            val dialog = object : DialogWrapper(project, true) {
                init {
                    this.title = title
                }

                override fun createCenterPanel(): JComponent {
                    val panel = panel {
                        row {
                            label(message)
                        }
                        values.forEach { (k, v) ->
                            row {
                                label(k)
                                component(v)
                            }
                        }
                    }
                    panel.preferredSize = preferredSize
                    return panel
                }
            }
            if (dialog.showAndGet()) {
                onOk(values.map { it.first to it.second.text }.toMap())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
