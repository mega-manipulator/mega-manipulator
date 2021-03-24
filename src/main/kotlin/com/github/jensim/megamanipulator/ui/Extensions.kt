package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.settings.ProjectOperator.project
import java.io.File

fun File.trimProjectPath(): String {
    val projectPath: String? = project?.basePath
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
