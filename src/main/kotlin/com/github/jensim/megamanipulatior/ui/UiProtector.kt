package com.github.jensim.megamanipulatior.ui

import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import javax.swing.JOptionPane
import javax.swing.JTextArea

fun uiProtectedOperation(onFailMsg: () -> String, onfailAction: (() -> Unit)? = null, action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {
        e.printStackTrace()
        val ui = panel {
            /*
             * TODO:
             * Make this output format better.
             * Currently it eats up the entire screen.
             */
            row {
                JTextArea(onFailMsg())
            }
            row {
                JBTextArea(e.stackTrace.joinToString("\n"))
            }
        }
        val ans: Int = JOptionPane.showConfirmDialog(null, ui)
        if (ans == JOptionPane.OK_OPTION && onfailAction != null) {
            try {
                onfailAction()
            } catch (e: Exception) {
                JOptionPane.showConfirmDialog(null, "Backup action failed as well..\nCannot say you are in a good spot at the moment.")
            }
        }
    }
}
