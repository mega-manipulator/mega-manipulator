package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import java.io.File

class ApplyOperator @NonInjectable constructor(
    project: Project,
    settingsFileOperator: SettingsFileOperator?,
    filesOperator: FilesOperator?,
    processOperator: ProcessOperator?,
    localRepoOperator: LocalRepoOperator?,
    uiProtector: UiProtector?,
) {

    constructor(project: Project) : this(project, null, null, null, null, null)

    private val settingsFileOperator: SettingsFileOperator by lazyService(project, settingsFileOperator)
    private val filesOperator: FilesOperator by lazyService(project, filesOperator)
    private val processOperator: ProcessOperator by lazyService(project, processOperator)
    private val localRepoOperator: LocalRepoOperator by lazyService(project, localRepoOperator)
    private val uiProtector: UiProtector by lazyService(project, uiProtector)

    fun apply(): List<ApplyOutput> {
        if (!settingsFileOperator.scriptFile.exists()) {
            return emptyList()
        }
        val gitDirs: List<File> = localRepoOperator.getLocalRepoFiles()
        filesOperator.refreshConf()
        filesOperator.refreshClones()
        val scriptPath = settingsFileOperator.scriptFile.absolutePath
        val settings = settingsFileOperator.readSettings()!!
        return uiProtector.mapConcurrentWithProgress(
            title = "Applying changes from script file",
            extraText1 = "Cancelling this will not terminate running processes",
            extraText2 = { "${it.parentFile.parentFile.name}/${it.parentFile.name}/${it.name}" },
            concurrent = settings.concurrency,
            data = gitDirs,
        ) { dir ->
            processOperator.runCommandAsync(dir, listOf("/bin/bash", scriptPath)).await()
        }.map { (dir, out) -> out ?: ApplyOutput.dummy(dir.path) }.also {
            filesOperator.refreshClones()
        }
    }
}
