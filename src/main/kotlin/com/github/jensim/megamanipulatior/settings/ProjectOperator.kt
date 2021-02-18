package com.github.jensim.megamanipulatior.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

object ProjectOperator {

    private var projectPrivate: Project? = null
    var project: Project
        get() = projectPrivate ?: projectBackup
        set(value) {
            projectPrivate = value
        }
    private val projectBackup: Project by lazy {
        ProjectManager.getInstance().openProjects.first()
    }
}