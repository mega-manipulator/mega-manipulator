package com.github.jensim.megamanipulatior.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.panel
import javax.swing.JButton

object SettingsWindow {

    private val label = JBLabel()
    private val button = JButton("Validate config")
    val content = panel {
        row {
            component(button)
        }
        row {
            component(label)
        }
    }

    init {
        button.addActionListener {
            button.isEnabled = false
            SettingsFileOperator.readSettings()
            label.text = SettingsFileOperator.validationText
            button.isEnabled = true
        }
    }
}