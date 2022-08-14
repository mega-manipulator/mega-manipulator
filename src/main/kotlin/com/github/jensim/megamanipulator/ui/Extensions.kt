package com.github.jensim.megamanipulator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import java.io.File
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JRootPane

fun File.trimProjectPath(project: Project): String {
    val projectPath: String? = project.basePath
    return if (projectPath != null && this.path.startsWith(projectPath)) {
        this.path.drop(projectPath.length).let {
            if (it.startsWith('/')) {
                it.drop(1)
            } else {
                it
            }
        }
    } else {
        this.path
    }
}

fun Row.groupPanel(title: String, dsl: Row.() -> Unit): Cell<DialogPanel> {
    return cell(
        com.intellij.ui.dsl.builder.panel {
            group(title) {
                row {
                    dsl()
                }
            }
        }
    )
}

fun Balloon.setDefaultButton(panel: JComponent, button: JButton) {
    val rootPane: JRootPane? = panel.rootPane
    rootPane?.defaultButton = button
    val listener = object : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
            super.onClosed(event)
            rootPane?.defaultButton = null
        }
    }
    addListener(listener)
}
