package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.mapConcurrentWithProgress
import java.io.File

object ApplyOperator {
    fun apply(): List<ApplyOutput> {
        if (!SettingsFileOperator.scriptFile.exists()) {
            return emptyList()
        }
        val gitDirs: List<File> = LocalRepoOperator.getLocalRepoFiles()
        FilesOperator.refreshClones()
        val scriptPath = SettingsFileOperator.scriptFile.absolutePath
        val settings = SettingsFileOperator.readSettings()!!
        return gitDirs.mapConcurrentWithProgress(
            title = "Applying changes from script file",
            extraText1 = "Cancelling this will not terminate running processes",
            extraText2 = { "${it.parentFile.parentFile.name}/${it.parentFile.name}/${it.name}" },
            concurrent = settings.concurrency
        ) { dir ->
            ProcessOperator.runCommandAsync(dir, listOf("/bin/bash", scriptPath)).await()
        }.map { (dir, out) -> out ?: ApplyOutput.dummy(dir.path) }
    }
}
