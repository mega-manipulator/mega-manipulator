package com.github.jensim.megamanipulator.actions.git.commit

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.settings.CodeHostSettings
import com.github.jensim.megamanipulator.settings.ForkSetting
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
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

    companion object {

        val instance by lazy {
            CommitOperator(
                dialogGenerator = DialogGenerator.instance,
                settingsFileOperator = SettingsFileOperator.instance,
                localRepoOperator = LocalRepoOperator.instance,
                processOperator = ProcessOperator.instance,
                prRouter = PrRouter.instance,
                uiProtector = UiProtector.instance,
            )
        }
    }

    fun commit(): Map<String, List<Pair<String, ApplyOutput>>> {
        val commitMessageKey = "Commit message"
        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        dialogGenerator.askForInput(
            title = "Create commits",
            message = "Create commits for all changes in all checked out repositories",
            values = listOf(commitMessageKey),
            onOk = {
                val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
                var push = false
                val commitMessage = it[commitMessageKey]
                if (commitMessage.isNullOrEmpty()) {
                    return@askForInput
                }
                var workTitle = "Commiting"
                dialogGenerator.showConfirm("Also push?", "Also push? $commitMessage") {
                    push = true
                    workTitle += " & pushing"
                }
                val dirs = localRepoOperator.getLocalRepoFiles()
                uiProtector.mapConcurrentWithProgress(
                    title = workTitle,
                    data = dirs,
                ) { dir: File ->
                    processOperator.runCommandAsync(dir, listOf("git", "add", "--all")).await()
                    val log = result.computeIfAbsent(dir.path) { ArrayList() }
                    val commit = processOperator.runCommandAsync(dir, listOf("git", "commit", "-m", commitMessage)).await()
                        .also { output -> log += "commit" to output }
                    if (push && commit.exitCode == 0) {
                        push(settings, dir, log)
                    }
                }
            },
            onCancel = {
                dialogGenerator.showConfirm("Info", "No commit performed!") {}
            }
        )
        if (result.isEmpty()) {
            result["no result"] = mutableListOf("nothing" to ApplyOutput(".", std = "", err = "", exitCode = 1))
        }
        return result
    }

    private suspend fun push(settings: MegaManipulatorSettings, dir: File, log: MutableList<Pair<String, ApplyOutput>>) {
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
            val fork = localRepoOperator.addForkRemote(dir, cloneUrl)
                .also { output -> log += "fork" to output }
            if (fork.exitCode == 0) {
                localRepoOperator.push(dir)
                    .also { output -> log += "push" to output }
            }
        } else {
            log += "fork" to ApplyOutput.dummy(dir = dir.path, std = "Failed creating or finding fork", exitCode = 1)
        }
    }

    fun push(): Map<String, List<Pair<String, ApplyOutput>>> {
        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        dialogGenerator.showConfirm("Push", "Push local commits to remote") {
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
