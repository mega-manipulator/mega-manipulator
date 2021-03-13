package com.github.jensim.megamanipulatior.actions.git.commit

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.apply.ApplyOutput
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.actions.vcs.PrRouter
import com.github.jensim.megamanipulatior.settings.CodeHostSettings
import com.github.jensim.megamanipulatior.settings.ForkSetting
import com.github.jensim.megamanipulatior.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import com.github.jensim.megamanipulatior.ui.DialogGenerator
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object CommitOperator {

    fun commit(): Map<String, List<Pair<String, ApplyOutput>>> {
        val commitMessageKey = "Commit message"
        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        DialogGenerator.askForInput(
            title = "Create commits",
            message = "Create commits for all changes in all checked out repositories",
            values = listOf(commitMessageKey),
            onOk = {
                val settings: MegaManipulatorSettings = SettingsFileOperator.readSettings()!!
                var push = false
                val commitMessage = it[commitMessageKey]
                if (commitMessage.isNullOrEmpty()) {
                    return@askForInput
                }
                var workTitle = "Commiting"
                DialogGenerator.showConfirm("Also push?", "Also push? $commitMessage") {
                    push = true
                    workTitle += " & pushing"
                }
                val dirs = LocalRepoOperator.getLocalRepoFiles()
                dirs.mapConcurrentWithProgress(title = workTitle) { dir: File ->
                    ProcessOperator.runCommandAsync(dir, arrayOf("git", "add", "--all")).await()
                    val log = result.computeIfAbsent(dir.path) { ArrayList() }
                    val commit = ProcessOperator.runCommandAsync(dir, arrayOf("git", "commit", "-m", commitMessage)).await()
                        .also { output -> log += "commit" to output }
                    if (push && commit.exitCode == 0) {
                        push(settings, dir, log)
                    }
                }
            },
            onCancel = {
                DialogGenerator.showConfirm("Info", "No commit performed!") {}
            }
        )
        if (result.isEmpty()) {
            result["no result"] = mutableListOf("nothing" to ApplyOutput(".", std = "", err = "", exitCode = 1))
        }
        return result
    }

    private suspend fun push(settings: MegaManipulatorSettings, dir: File, log: MutableList<Pair<String, ApplyOutput>>) {
        val codeHostSettings: CodeHostSettings = settings.resolveSettings(dir)!!.second
        if (LocalRepoOperator.hasFork(dir) || codeHostSettings.forkSetting == ForkSetting.PLAIN_BRANCH) {
            LocalRepoOperator.push(dir)
                .also { output -> log += "push" to output }
        } else if (codeHostSettings.forkSetting == ForkSetting.LAZY_FORK) {
            val pushResult = LocalRepoOperator.push(dir)
                .also { output -> log += "push" to output }
            if (pushResult.exitCode != 0) {
                forkAndPush(dir, log)
            }
        } else if (codeHostSettings.forkSetting == ForkSetting.EAGER_FORK) {
            forkAndPush(dir, log)
        }
    }

    private suspend fun forkAndPush(dir: File, log: MutableList<Pair<String, ApplyOutput>>) {
        val cloneUrl = PrRouter.createFork(SearchResult.fromPath(dir))
        if (cloneUrl != null) {
            log += "fork" to ApplyOutput.dummy(dir = dir.path, std = "Created or found fork", exitCode = 0)
            val fork = LocalRepoOperator.addForkRemote(dir, cloneUrl)
                .also { output -> log += "fork" to output }
            if (fork.exitCode == 0) {
                LocalRepoOperator.push(dir)
                    .also { output -> log += "push" to output }
            }
        } else {
            log += "fork" to ApplyOutput.dummy(dir = dir.path, std = "Failed creating or finding fork", exitCode = 1)
        }
    }

    fun push(): Map<String, List<Pair<String, ApplyOutput>>> {
        val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
        DialogGenerator.showConfirm("Push", "Push local commits to remote") {
            val dirs = LocalRepoOperator.getLocalRepoFiles()
            val settings: MegaManipulatorSettings = SettingsFileOperator.readSettings()!!
            dirs.mapConcurrentWithProgress("Pushing") { dir ->
                push(settings, dir, result.computeIfAbsent(dir.path) { ArrayList() })
            }
        }
        if (result.isEmpty()) {
            result["no result"] = mutableListOf("nothing" to ApplyOutput(".", std = "", err = "", exitCode = 1))
        }
        return result
    }
}
