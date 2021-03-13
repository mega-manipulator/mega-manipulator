package com.github.jensim.megamanipulatior.ui

import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import java.io.File

fun File.trimProjectPath(): String = if (this.path.startsWith(project.basePath!!)) {
    this.path.drop(project.basePath!!.length).let {
        if (it.startsWith('/')) {
            it.drop(1)
        } else {
            it
        }
    }
} else {
    this.path
}
