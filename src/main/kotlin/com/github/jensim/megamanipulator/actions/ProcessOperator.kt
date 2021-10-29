package com.github.jensim.megamanipulator.actions

import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.ui.trimProjectPath
import com.intellij.openapi.project.Project
import java.io.File
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred

class ProcessOperator(private val project: Project) {

    fun runCommandAsync(workingDir: File, command: List<String>): Deferred<ApplyOutput> {
        val builder = ProcessBuilder(command)
            .directory(workingDir)
        builder.redirectErrorStream(true)
        val proc = builder.start()
        return proc.onExit().thenApply {
            ApplyOutput(
                std = it.inputStream.readAllBytes().decodeToString(),
                err = "",
                exitCode = it.exitValue(),
                dir = workingDir.trimProjectPath(project = project),
            )
        }.asDeferred()
    }
}
