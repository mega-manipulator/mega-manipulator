package com.github.jensim.megamanipulatior.actions.apply

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred

object ApplyOperator {

    suspend fun apply(): List<ApplyOutput> {
        if (!SettingsFileOperator.scriptFile.exists()) {
            return emptyList()
        }
        val clonesDir = File(project.basePath, "clones")
        val clonesPath = clonesDir.toPath()

        val gitDirs: List<File> = Files.find(clonesDir.toPath(), 3, { path, attr -> isLikelyCloneDir(clonesPath, path) })
            .map { it.toFile() }
            .collect(Collectors.toList())

        val scriptPath = SettingsFileOperator.scriptFile.absolutePath
        val futures: List<Deferred<ApplyOutput>> = gitDirs.mapNotNull {
            ProcessOperator.runCommand(it, scriptPath)?.asDeferred()
        }
        return futures.awaitAll()
    }

    private fun isLikelyCloneDir(clonesPath: Path, evalPath: Path): Boolean {
        val evalFile = evalPath.toFile()
        val gitDir = File(evalFile, ".git")
        if (gitDir.exists()) {
            val relativePath = evalFile.absolutePath.removePrefix(clonesPath.toFile().absolutePath)
            if (relativePath.count { it == File.separatorChar } == 3) {
                return true
            }
        }
        return false
    }
}