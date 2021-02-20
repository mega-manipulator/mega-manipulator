package com.github.jensim.megamanipulatior.actions

import com.github.jensim.megamanipulatior.actions.apply.ApplyOutput
import java.io.File
import java.util.concurrent.CompletableFuture

object ProcessOperator {

    fun runCommand(workingDir: File, command: String): CompletableFuture<ApplyOutput>? {
        val parts = command.split("\\s".toRegex())
        val tempOutput = File.createTempFile("mega-manipulator-apply-out", "txt")
        val tempErrOutput = File.createTempFile("mega-manipulator-apply-err", "txt")
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectError(tempOutput)
            .redirectOutput(tempOutput)
            .start()
        return proc.onExit().thenApply {
            ApplyOutput(
                std = tempOutput.readText(),
                err = tempErrOutput.readText(),
                exitCode = it.exitValue(),
                dir = workingDir.absolutePath,
            )
        }
    }
}
