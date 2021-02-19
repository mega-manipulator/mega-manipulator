package com.github.jensim.megamanipulatior.actions.apply

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred

object ApplyOperator {

    suspend fun apply(command: String) {
        val clonesPath = File(project.basePath, "clones").toPath()
        val gitDirs: List<File> = Files.find(clonesPath, 3, { evalPath, attr -> evalPath.fileName?.toString().orEmpty() == ".git" && attr.isDirectory })
            .map { it.toFile().parentFile }
            .collect(Collectors.toList())
        val futures: List<Deferred<Process>> = gitDirs.mapNotNull {
            ProcessOperator.runCommand(it, command)?.asDeferred()
        }
        futures.awaitAll()
    }
}