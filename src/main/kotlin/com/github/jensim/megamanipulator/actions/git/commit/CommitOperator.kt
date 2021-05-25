package com.github.jensim.megamanipulator.actions.git.commit

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.ForkSetting
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.UiProtector
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class CommitOperator(
    private val dialogGenerator: DialogGenerator,
    private val settingsFileOperator: SettingsFileOperator,
    private val localRepoOperator: LocalRepoOperator,
    private val processOperator: ProcessOperator,
    private val prRouter: PrRouter,
    private val uiProtector: UiProtector,
) {

    fun commit(): Map<String, List<Pair<String, ApplyOutput>>> {
        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        val commitMessage = dialogGenerator.askForInput(
            "Create commits for all changes in all checked out repositories",
            "Commit message"
        )
        if (commitMessage != null && commitMessage.isNotEmpty()) {
            val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
            var push = false

            var workTitle = "Commiting"
            if (dialogGenerator.showConfirm("Also push?", "Also push? $commitMessage")) {
                push = true
                workTitle += " & pushing"
            }
            val dirs = localRepoOperator.getLocalRepoFiles()
            uiProtector.mapConcurrentWithProgress(
                title = workTitle,
                data = dirs,
            ) { commitProcess(it, result, commitMessage, push, settings) }
        } else {
            dialogGenerator.showConfirm("Info", "No commit performed!")
        }
        if (result.isEmpty()) {
            result["no result"] = mutableListOf("nothing" to ApplyOutput(".", std = "", err = "", exitCode = 1))
        }
        return result
    }

    suspend fun commitProcess(
        it: File,
        result: ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>,
        commitMessage: String,
        push: Boolean,
        settings: MegaManipulatorSettings
    ) {
        processOperator.runCommandAsync(it, listOf("git", "add", "--all")).await()
        val log = result.computeIfAbsent(it.path) { ArrayList() }
        val commit = processOperator.runCommandAsync(it, listOf("git", "commit", "-m", commitMessage)).await()
            .also { output -> log += "commit" to output }
        if (push && commit.exitCode == 0) {
            push(settings, it, log)
        }
    }

    private suspend fun push(
        settings: MegaManipulatorSettings,
        dir: File,
        log: MutableList<Pair<String, ApplyOutput>>
    ) {
        val codeHostSettings: CodeHostSettings = settings.resolveSettings(dir)!!.second
        if (localRepoOperator.hasFork(dir) || codeHostSettings.forkSetting == ForkSetting.PLAIN_BRANCH) {
            localRepoOperator.push(dir)
                .also { output -> log += "push" to output }
        } else if (codeHostSettings.forkSetting == ForkSetting.LAZY_FORK) {
            val pushResult = localRepoOperator.push(dir)
                .also { output -> log += "push" to output }
            if (pushResult.exitCode != 0) {
                forkAndPush(dir, log)
            }
        } else if (codeHostSettings.forkSetting == ForkSetting.EAGER_FORK) {
            forkAndPush(dir, log)
        }
    }

    private suspend fun forkAndPush(dir: File, log: MutableList<Pair<String, ApplyOutput>>) {
        val cloneUrl = prRouter.createFork(SearchResult.fromPath(dir))
        if (cloneUrl != null) {
            log += "fork" to ApplyOutput.dummy(dir = dir.path, std = "Created or found fork", exitCode = 0)
            val fork = localRepoOperator.addForkRemote(dir, cloneUrl).also { output -> log += "fork" to output }
            if (fork.exitCode == 0) {
                localRepoOperator.push(dir).also { output -> log += "push" to output }
            }
        } else {
            log += "fork" to ApplyOutput.dummy(dir = dir.path, std = "Failed creating or finding fork", exitCode = 1)
        }
    }

    fun push(): Map<String, List<Pair<String, ApplyOutput>>> {
        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        if (dialogGenerator.showConfirm("Push", "Push local commits to remote")) {
            val dirs = localRepoOperator.getLocalRepoFiles()
            val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
            uiProtector.mapConcurrentWithProgress(
                title = "Pushing",
                data = dirs
            ) { dir ->
                push(settings, dir, result.computeIfAbsent(dir.path) { ArrayList() })
            }
        }
        if (result.isEmpty()) {
            result["no result"] = mutableListOf("nothing" to ApplyOutput(".", std = "", err = "", exitCode = 1))
        }
        return result
    }
}
