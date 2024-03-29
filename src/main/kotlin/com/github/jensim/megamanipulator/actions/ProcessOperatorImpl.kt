package com.github.jensim.megamanipulator.actions

import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.ui.trimProjectPath
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ProcessOperatorImpl(private val project: Project) : ProcessOperator {

    private val log = LoggerFactory.getLogger(javaClass)
    private val pidMap = ConcurrentHashMap<Long, String>()

    override fun runCommandAsync(workingDir: File, command: List<String>): Deferred<ApplyOutput> {
        val builder = ProcessBuilder(command)
            .directory(workingDir)
        builder.redirectErrorStream(true)
        val proc: Process = builder.start()
        addPid(proc.pid(), workingDir, command)
        return proc.onExit().thenApply {
            pidMap.remove(it.pid())
            ApplyOutput(
                command = command.joinToString(separator = " "),
                std = it.inputStream.readAllBytes().decodeToString(),
                exitCode = it.exitValue(),
                dir = workingDir.trimProjectPath(project = project),
            )
        }.asDeferred()
    }

    private fun addPid(pid: Long, workingDir: File, command: List<String>) {
        pidMap[pid] = "dir:'${workingDir.path}', command:$command"
    }

    override fun clearPids(): Unit = pidMap.clear()
    override fun stopAll() {
        pidMap.forEach { (pid, description) ->
            ProcessHandle.of(pid).ifPresent { handle ->
                if (handle.destroyForcibly()) {
                    log.warn("Killed process with pid:$pid $description")
                } else {
                    log.warn("Was unable to killed process with pid:$pid $description")
                }
            }
        }
        clearPids()
    }
}
