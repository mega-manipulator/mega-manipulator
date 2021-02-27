package com.github.jensim.megamanipulatior.actions.apply

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

object ApplyOperator {

    fun apply(): List<ApplyOutput> {
        if (!SettingsFileOperator.scriptFile.exists()) {
            return emptyList()
        }
        val gitDirs: List<File> = LocalRepoOperator.getLocalRepoFiles()

        val dispatcher: CoroutineContext = if (SettingsFileOperator.readSettings()?.forceSingleThreaded == true) {
            Dispatchers.Main
        } else {
            Dispatchers.Default
        }
        val scriptPath = SettingsFileOperator.scriptFile.absolutePath
        return gitDirs.mapConcurrentWithProgress(title = "Applying changes from script file", coroutineContext = dispatcher, cancelable = true) { dir ->
            ProcessOperator.runCommand(dir, arrayOf("/bin/bash", scriptPath))?.get() ?: ApplyOutput.dummy(dir = dir.path)
        }.map { (dir, output) ->
            output ?: ApplyOutput.dummy(dir.path)
        }
    }
}
