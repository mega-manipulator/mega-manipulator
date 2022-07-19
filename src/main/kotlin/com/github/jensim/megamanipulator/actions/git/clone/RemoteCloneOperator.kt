package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettings
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import java.io.File

internal typealias Action = Pair<String, ApplyOutput>

class RemoteCloneOperator @NonInjectable constructor(
    private val project: Project,
    localRepoOperator: LocalRepoOperator?,
    processOperator: ProcessOperator?,
) {
    constructor(project: Project) : this(
        project = project,
        localRepoOperator = null,
        processOperator = null,
    )

    private val localRepoOperator: LocalRepoOperator by lazyService(project, localRepoOperator)
    private val processOperator: ProcessOperator by lazyService(project, processOperator)

    suspend fun cloneRepos(pullRequest: PullRequestWrapper, settings: MegaManipulatorSettings, sparseDef: String?): List<Action> {
        val basePath = project.basePath!!
        val fullPath = "$basePath/clones/${pullRequest.asPathString()}"
        val dir = File(fullPath)
        val prSettings: CodeHostSettings =
            settings.resolveSettings(pullRequest.searchHostName(), pullRequest.codeHostName())?.second
                ?: return listOf(
                    "Settings" to ApplyOutput.dummy(
                        dir = pullRequest.asPathString(),
                        std = "No settings found for ${pullRequest.searchHostName()}/${pullRequest.codeHostName()}, most likely the key of the code host does not match the one returned by your search host!"
                    )
                )
        val badState: List<Action> =
            clone(
                dir = dir,
                cloneUrl = pullRequest.cloneUrlFrom(prSettings.cloneType)!!,
                defaultBranch = pullRequest.fromBranch(),
                shallow = false,
                sparseDef = sparseDef
            )
        if (badState.isOkay() && pullRequest.isFork()) {
            localRepoOperator.promoteOriginToForkRemote(dir, pullRequest.cloneUrlTo(prSettings.cloneType)!!)
        }
        return badState
    }

    @SuppressWarnings("ReturnCount")
    suspend fun clone(
        dir: File,
        cloneUrl: String,
        defaultBranch: String,
        branch: String = defaultBranch,
        shallow: Boolean,
        sparseDef: String?,
    ): List<Action> {
        val actionTrace: MutableList<Action> = mutableListOf()
        dir.mkdirs()
        if (File(dir, ".git").exists()) {
            actionTrace.add("Repo already cloned" to ApplyOutput.dummy(dir = dir.path, std = "Repo already cloned"))
            return actionTrace
        }
        val cloneCommandsResult = cloneCommands(shallow, cloneUrl, defaultBranch, dir)
        actionTrace.addAll(cloneCommandsResult)

        if (actionTrace.isOkay()) {
            actionTrace.addAll(pullCommands(dir, sparseDef, defaultBranch, shallow))
        }

        if (defaultBranch != branch && actionTrace.isOkay()) {
            val p3 = processOperator.runCommandAsync(dir, listOf("git", "checkout", branch)).await()
            actionTrace.add("Switch branch" to p3)
            if (p3.exitCode != 0) {
                val p4 = processOperator.runCommandAsync(dir, listOf("git", "checkout", "-b", branch)).await()
                actionTrace.add("Create branch" to p4)
            }
        }
        return actionTrace
    }

    private fun List<Action>.isOkay(): Boolean = isEmpty() || lastOrNull()?.second?.exitCode == 0

    private suspend fun cloneCommands(shallow: Boolean, cloneUrl: String, defaultBranch: String, dir: File): List<Action> {
        val actionTrace = mutableListOf<Action>()
        val cloneActionName = if (shallow) "Shallow clone" else "Clone"

        val cloneArgs = if (shallow) {
            listOf("git", "clone", cloneUrl, "--depth", "1", "--no-checkout", "--branch", defaultBranch, dir.absolutePath)
        } else {
            listOf("git", "clone", cloneUrl, "--no-checkout", "--branch", defaultBranch, dir.absolutePath)
        }
        val p0 = processOperator.runCommandAsync(dir.parentFile, cloneArgs).await()
        actionTrace.add(cloneActionName to p0)
        return actionTrace
    }

    private suspend fun pullCommands(dir: File, sparseDef: String?, defaultBranch: String, shallow: Boolean): List<Action> {
        val actionTrace = mutableListOf<Action>()
        if (sparseDef != null) {
            val p0 = processOperator.runCommandAsync(dir, listOf("git", "config", "core.sparseCheckout", "true")).await()
            actionTrace.add("Config sparse checkout" to p0)
            if (p0.exitCode == 0) {
                try {
                    val sparseFile = File(dir, ".git/info/sparse-checkout")
                    sparseFile.writeText(sparseDef)
                    actionTrace.add("Setup sparse checkout config" to ApplyOutput(dir = dir.absolutePath, std = "Setup successful", exitCode = 0))
                } catch (e: Exception) {
                    // e.printStackTrace()
                    actionTrace.add("Setup sparse checkout config" to ApplyOutput(dir = dir.absolutePath, std = "Failed writing sparse config file\n${e.stackTraceToString()}", exitCode = 1))
                }
            }
        }
        if (actionTrace.isOkay()) {
            val p1 = if (shallow) {
                processOperator.runCommandAsync(dir, listOf("git", "fetch", "--depth", "1", "origin", defaultBranch)).await()
            } else {
                processOperator.runCommandAsync(dir, listOf("git", "fetch", "origin", defaultBranch)).await()
            }
            actionTrace.add("Fetch" to p1)
            if (p1.exitCode == 0) {
                val p2 = processOperator.runCommandAsync(dir, listOf("git", "checkout", defaultBranch)).await()
                actionTrace.add("Checkout" to p2)
            }
        }
        return actionTrace
    }
}
