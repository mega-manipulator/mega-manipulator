package com.github.jensim.megamanipulator.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

object ProjectOperator {

    private var projectPrivate: Project? = null
    var project: Project?
        get() = projectPrivate ?: projectBackup()
        set(value) {
            if (value != null) {
                projectPrivate = value
            }
        }

    private fun projectBackup(): Project? = try {
        ProjectManager.getInstance().openProjects.firstOrNull()
    } catch (e: NullPointerException) {
        null
    }
}
