package com.github.jensim.megamanipulatior.actions.apply

import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import javax.swing.JButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object ApplyWindow {
    val textArea = JBTextArea()
    val button = JButton("Apply")
    val content = panel {
        row {
            component(button)
        }
        row {
            component(textArea)
        }
    }

    init {
        button.addActionListener {
            button.isEnabled = false
            GlobalScope.launch {
                ApplyOperator.apply(textArea.text)
                button.isEnabled = true
            }
        }
    }
}