package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.MyBundle
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.Action
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.ui.trimProjectPath
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import org.slf4j.LoggerFactory
import java.io.File

class SparseConfigSetupOperator @NonInjectable constructor(
    private val project: Project,
    processOperator: ProcessOperator?,
) {

    constructor(project: Project) : this(project, null)

    private val logger = LoggerFactory.getLogger(javaClass)
    private val processOperator: ProcessOperator by lazyService(project, processOperator)

    suspend fun setupSparseDef(sparseDef: String?, dir: File): List<Action> {
        val actionTrace = mutableListOf<Action>()
        val sparseFile = File(dir, ".git/info/sparse-checkout")
        if (sparseDef != null) {
            val p0 = processOperator.runCommandAsync(dir, listOf("git", "config", "core.sparseCheckout", "true")).await()
            actionTrace.add(Action(MyBundle.message("sparseCheckoutConfig"), p0))
            if (p0.exitCode == 0) {
                try {
                    sparseFile.writeText(sparseDef)
                    actionTrace.add(
                        Action(
                            MyBundle.message("sparseCheckoutWrite"),
                            ApplyOutput(
                                dir = dir.absolutePath,
                                std = "Setup successful",
                                exitCode = 0,
                            )
                        )
                    )
                } catch (e: Exception) {
                    val msg = "Failed writing sparse config file"
                    logger.error(msg, e)
                    actionTrace.add(
                        Action(
                            MyBundle.message("sparseCheckoutWrite"),
                            ApplyOutput(
                                dir = dir.absolutePath,
                                std = "$msg. Full stacktrace in IDE logs. ${e.javaClass.simpleName}: ${e.message}",
                                exitCode = 1,
                            )
                        )
                    )
                }
            }
        } else {
            val p0 = processOperator.runCommandAsync(dir, listOf("git", "config", "core.sparseCheckout", "false")).await()
            actionTrace.add(Action(MyBundle.message("sparseCheckoutDisable"), p0))
            if (sparseFile.exists()) {
                val deleted = sparseFile.delete()
                val what = MyBundle.message("sparseCheckoutFileDeleted")
                actionTrace.add(Action(what, ApplyOutput(dir.trimProjectPath(project), what, if (deleted) 0 else 1, command = "${File::class.java.canonicalName}.delete()")))
            }
        }
        return actionTrace.toList()
    }
}
