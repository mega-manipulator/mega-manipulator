package com.github.jensim.megamanipulatior.actions.apply

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
import java.io.File
import kotlinx.coroutines.future.asDeferred

object ApplyOperator {
    fun apply(): List<ApplyOutput> {
        if (!SettingsFileOperator.scriptFile.exists()) {
            return emptyList()
        }
        val gitDirs: List<File> = LocalRepoOperator.getLocalRepoFiles()

        val scriptPath = SettingsFileOperator.scriptFile.absolutePath
        val settings = SettingsFileOperator.readSettings()!!
        return gitDirs.mapConcurrentWithProgress(title = "Applying changes from script file", concurrent = settings.concurrency) { dir ->
            ProcessOperator.runCommand(dir, arrayOf("/bin/bash", scriptPath))?.asDeferred()?.await()
        }.map { (dir, out) -> out ?: ApplyOutput.dummy(dir.path) }
    }
}
