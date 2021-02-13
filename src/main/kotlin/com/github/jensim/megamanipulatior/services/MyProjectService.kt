package com.github.jensim.megamanipulatior.services

import com.github.jensim.megamanipulatior.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
