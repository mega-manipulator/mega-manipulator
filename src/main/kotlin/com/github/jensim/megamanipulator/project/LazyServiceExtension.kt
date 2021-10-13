package com.github.jensim.megamanipulator.project

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

inline fun <reified T> lazyService(project: Project, override: T?): Lazy<T> = lazy {
    override ?: project.service()
}
