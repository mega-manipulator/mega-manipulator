package com.github.jensim.megamanipulatior.actions

import java.io.File
import java.util.concurrent.CompletableFuture

object ProcessOperator {

    fun runCommand(workingDir: File, command: String): CompletableFuture<Process>? {
        val parts = command.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        return proc.onExit()
    }
}