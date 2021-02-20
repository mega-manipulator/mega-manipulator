package com.github.jensim.megamanipulatior.actions.apply

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import java.io.File
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred

object ApplyOperator {

    suspend fun apply(): List<ApplyOutput> {
        if (!SettingsFileOperator.scriptFile.exists()) {
            return emptyList()
        }
        val gitDirs: List<File> = LocalRepoOperator.getLocalRepos()

        val scriptPath = SettingsFileOperator.scriptFile.absolutePath
        val futures: List<Deferred<ApplyOutput>> = gitDirs.mapNotNull {
            ProcessOperator.runCommand(it, scriptPath)?.asDeferred()
        }
        return futures.awaitAll()
    }
}
