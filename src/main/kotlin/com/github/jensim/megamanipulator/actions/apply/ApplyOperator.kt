package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.UiProtector
import java.io.File

class ApplyOperator(
    private val settingsFileOperator: SettingsFileOperator,
    private val filesOperator: FilesOperator,
    private val processOperator: ProcessOperator,
    private val localRepoOperator: LocalRepoOperator,
    private val uiProtector: UiProtector,
) {

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
