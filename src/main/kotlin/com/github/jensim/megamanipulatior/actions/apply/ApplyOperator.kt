package com.github.jensim.megamanipulatior.actions.apply

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
import java.io.File

object ApplyOperator {
    fun apply(): List<ApplyOutput> {
        if (!SettingsFileOperator.scriptFile.exists()) {
            return emptyList()
        }
        val gitDirs: List<File> = LocalRepoOperator.getLocalRepoFiles()

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
