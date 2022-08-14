package com.github.jensim.megamanipulator.actions.git.commit

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.apply.StepResult
import com.github.jensim.megamanipulator.actions.git.GitUrlHelper
import com.github.jensim.megamanipulator.actions.git.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettings
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
        repoDir: File,
        result: ConcurrentHashMap<String, MutableList<StepResult>>,
        commitMessage: String,
        push: Boolean,
        force: Boolean,
        settings: MegaManipulatorSettings,
    ) {
        processOperator.runCommandAsync(repoDir, listOf("git", "add", "--all")).await()
        val log = result.computeIfAbsent(repoDir.path) { ArrayList() }
        val commit = processOperator.runCommandAsync(repoDir, listOf("git", "commit", "-m", commitMessage)).await()
            .also { output -> log += StepResult("commit", output) }
        if (push && commit.exitCode == 0) {
            log += push(settings, repoDir, force)
        }
    }

    suspend fun push(
        settings: MegaManipulatorSettings,
        dir: File,
        force: Boolean,

    ): List<StepResult> {
        val log = mutableListOf<StepResult>()
        val codeHostSettings: CodeHostSettings = settings.resolveSettings(dir)!!.second
        if (localRepoOperator.hasFork(dir) || codeHostSettings.forkSetting == ForkSetting.PLAIN_BRANCH) {
            localRepoOperator.push(dir, force)
                .also { output -> log += StepResult("push", output) }
        } else if (codeHostSettings.forkSetting == ForkSetting.LAZY_FORK) {
            val pushResult = localRepoOperator.push(dir, force)
                .also { output -> log += StepResult("push", output) }
            if (pushResult.exitCode != 0) {
                log += forkAndPush(dir, force)
            }
        } else if (codeHostSettings.forkSetting == ForkSetting.EAGER_FORK) {
            log += forkAndPush(dir, force)
        }
        return log
    }

    private suspend fun forkAndPush(dir: File, force: Boolean): List<StepResult> {
        val log = mutableListOf<StepResult>()
        val repo = SearchResult.fromPath(dir)
        val cloneUrl = prRouter.createFork(repo)
        if (cloneUrl != null) {
            log += StepResult("fork", ApplyOutput.dummy(dir = dir.path, std = "Created or found fork", exitCode = 0))
            val settings = settingsFileOperator.readSettings()?.resolveSettings(dir)?.second
            if (settings == null) {
                log += StepResult("settings", ApplyOutput.dummy(std = "No settings for ${repo.asPathString()}"))
            } else {
                val actualGitUrl = gitUrlHelper.buildCloneUrl(settings, cloneUrl)
                val fork = localRepoOperator.addForkRemote(dir, actualGitUrl).also { output -> log += StepResult("fork", output) }
                if (fork.exitCode == 0) {
                    localRepoOperator.push(dir, force).also { output -> log += StepResult("push", output) }
                }
            }
        } else {
            log += StepResult("fork", ApplyOutput.dummy(dir = dir.path, std = "Failed creating or finding fork", exitCode = 1))
        }
        return log
    }
}
