package com.github.jensim.megamanipulator.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Row
import java.io.File

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

fun Row.groupPanel(title: String, dsl: Row.() -> Unit) {
    cell(
        com.intellij.ui.dsl.builder.panel {
            group(title) {
                row {
                    dsl()
                }
            }
        }
    )
}
