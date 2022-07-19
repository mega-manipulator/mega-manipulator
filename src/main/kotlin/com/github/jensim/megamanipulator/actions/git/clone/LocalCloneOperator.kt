package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable

class LocalCloneOperator @NonInjectable constructor(
    private val project: Project,
    filesOperator: FilesOperator?,
) {
    constructor(project: Project) : this(
        project = project,
        filesOperator = null,
    )

    private val filesOperator: FilesOperator by lazyService(project, filesOperator)

    /**
     * If the repo is configured for local copy over remote clone, try to find the repo locally,
     * and create a clone from that.
     */
    fun copyIf(settings: MegaManipulatorSettings, repo: SearchResult): Boolean {
        TODO()
    }

    /**
     * Take a copy of a clone after it has been pulled into the configured location
     */
    fun takeCopy(settings: MegaManipulatorSettings, repo: SearchResult) {
        TODO()
        /*
         * Remove sparse checkout config?
         */
    }

    private fun existsLocally(settings: MegaManipulatorSettings, repo: SearchResult): Boolean {
        TODO()
    }
}
