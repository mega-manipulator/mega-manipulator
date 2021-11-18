package com.github.jensim.megamanipulator.actions.git.commit

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.GitUrlHelper
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class CommitOperator @NonInjectable constructor(
    project: Project,
    settingsFileOperator: SettingsFileOperator?,
    localRepoOperator: LocalRepoOperator?,
    processOperator: ProcessOperator?,
    prRouter: PrRouter?,
    gitUrlHelper: GitUrlHelper?,
) {

    constructor(project: Project) : this(project, null, null, null, null, null)

    private val settingsFileOperator: SettingsFileOperator by lazyService(project = project, settingsFileOperator)
    private val localRepoOperator: LocalRepoOperator by lazyService(project = project, localRepoOperator)
    private val processOperator: ProcessOperator by lazyService(project = project, processOperator)
    private val prRouter: PrRouter by lazyService(project = project, prRouter)
    private val gitUrlHelper: GitUrlHelper by lazyService(project = project, gitUrlHelper)

    suspend fun commitProcess(
        it: File,
        result: ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>,
        commitMessage: String,
        push: Boolean,
        force: Boolean,
        settings: MegaManipulatorSettings,
    ) {
        processOperator.runCommandAsync(it, listOf("git", "add", "--all")).await()
        val log = result.computeIfAbsent(it.path) { ArrayList() }
        val commit = processOperator.runCommandAsync(it, listOf("git", "commit", "-m", commitMessage)).await()
            .also { output -> log += "commit" to output }
        if (push && commit.exitCode == 0) {
            log += push(settings, it, force)
        }
    }

    suspend fun push(
        settings: MegaManipulatorSettings,
        dir: File,
        force: Boolean,

    ): List<Pair<String, ApplyOutput>> {
        val log = mutableListOf<Pair<String, ApplyOutput>>()
        val codeHostSettings: CodeHostSettings = settings.resolveSettings(dir)!!.second
        if (localRepoOperator.hasFork(dir) || codeHostSettings.forkSetting == ForkSetting.PLAIN_BRANCH) {
            localRepoOperator.push(dir, force)
                .also { output -> log += "push" to output }
        } else if (codeHostSettings.forkSetting == ForkSetting.LAZY_FORK) {
            val pushResult = localRepoOperator.push(dir, force)
                .also { output -> log += "push" to output }
            if (pushResult.exitCode != 0) {
                forkAndPush(dir, force)
            }
        } else if (codeHostSettings.forkSetting == ForkSetting.EAGER_FORK) {
            forkAndPush(dir, force)
        }
        return log
    }

    private suspend fun forkAndPush(dir: File, force: Boolean): List<Pair<String, ApplyOutput>> {
        val log = mutableListOf<Pair<String, ApplyOutput>>()
        val repo = SearchResult.fromPath(dir)
        val cloneUrl = prRouter.createFork(repo)
        if (cloneUrl != null) {
            log += "fork" to ApplyOutput.dummy(dir = dir.path, std = "Created or found fork", exitCode = 0)
            val settings = settingsFileOperator.readSettings()?.resolveSettings(dir)?.second
            if (settings == null) {
                log += "settings" to ApplyOutput.dummy(std = "No settings for ${repo.asPathString()}")
            } else {
                val actualGitUrl = gitUrlHelper.buildCloneUrl(settings, cloneUrl)
                val fork = localRepoOperator.addForkRemote(dir, actualGitUrl).also { output -> log += "fork" to output }
                if (fork.exitCode == 0) {
                    localRepoOperator.push(dir, force).also { output -> log += "push" to output }
                }
            }
        } else {
            log += "fork" to ApplyOutput.dummy(dir = dir.path, std = "Failed creating or finding fork", exitCode = 1)
        }
        return log
    }
}
