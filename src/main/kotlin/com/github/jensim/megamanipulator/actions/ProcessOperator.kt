package com.github.jensim.megamanipulator.actions

import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import kotlinx.coroutines.Deferred
import java.io.File

interface ProcessOperator {

    fun runCommandAsync(workingDir: File, command: List<String>): Deferred<ApplyOutput>
    fun clearPids()
    fun stopAll()
}
