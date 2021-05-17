package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.CloneType.SSH
import com.github.jensim.megamanipulator.settings.CodeHostSettings
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.PasswordsOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.NotificationType.WARNING
import java.io.File

private typealias Action = Pair<String, ApplyOutput>

@SuppressWarnings("LongParameterList")
class CloneOperator(
    private val filesOperator: FilesOperator,
    private val projectOperator: ProjectOperator,
    private val prRouter: PrRouter,
    private val localRepoOperator: LocalRepoOperator,
    private val processOperator: ProcessOperator,
    private val notificationsOperator: NotificationsOperator,
    private val uiProtector: UiProtector,
    private val settingsFileOperator: SettingsFileOperator,
    private val passwordsOperator: PasswordsOperator,
) {

    fun clone(repos: Set<SearchResult>) {
        val basePath = projectOperator.project.basePath!!
        filesOperator.refreshConf()
        val settings = settingsFileOperator.readSettings()!!
        val state: List<Pair<SearchResult, List<Action>?>> = uiProtector.mapConcurrentWithProgress(
            title = "Cloning repos",
            extraText1 = "Cloning repos",
            extraText2 = { it.asPathString() },
            data = repos,
        ) { repo ->
            val codeSettings: CodeHostSettings = settings.resolveSettings(repo.searchHostName, repo.codeHostName)!!.second
            prRouter.getRepo(repo)?.let { vcsRepo: RepoWrapper ->
                val cloneUrl = buildCloneUrl(codeSettings, vcsRepo)
                val defaultBranch = prRouter.getRepo(repo)?.getDefaultBranch()!!
                val dir = File(basePath, "clones/${repo.asPathString()}")
                clone(dir, cloneUrl, defaultBranch)
            }
        }
        filesOperator.refreshClones()
        reportState(state)
    }

    private fun buildCloneUrl(codeSettings: CodeHostSettings, vcsRepo: RepoWrapper): String {
        val cloneUrl = vcsRepo.getCloneUrl(codeSettings.cloneType)!!
        return when (codeSettings.cloneType) {
            SSH -> cloneUrl
            HTTPS -> {
                "${cloneUrl.substringBefore("://")}://${codeSettings.username!!}:${passwordsOperator.getPassword(codeSettings.username!!,codeSettings.baseUrl)!!}@${cloneUrl.substringAfter("://")}"
            }
        }
    }

    private fun reportState(state: List<Pair<Any, List<Action>?>>) {
        val badState = state.filter { it.second == null || it.second!!.isNotEmpty() }
        if (badState.isEmpty()) {
            notificationsOperator.show(
                title = "Cloning done",
                body = "All ${state.size} cloned successfully",
                type = INFORMATION,
            )
        } else {
            notificationsOperator.show(
                title = "Cloning done with failures",
                body = "Failed cloning ${badState.size}/${state.size} repos, details in ide logs",
                type = WARNING,
            )
            System.err.println("Failed cloning ${badState.size}/${state.size} repos, these are the causes:\n$badState")
        }
    }

    fun clone(pullRequests: List<PullRequestWrapper>) {
        val settings = uiProtector.uiProtectedOperation("Load settings") { settingsFileOperator.readSettings() }
        if (settings == null) {
            reportState(listOf("Settings" to listOf("Load Settings" to ApplyOutput.dummy(err = "No settings found for project."))))
        } else {
            val state: List<Pair<PullRequestWrapper, List<Action>?>> =
                uiProtector.mapConcurrentWithProgress(
                    title = "Cloning repos",
                    extraText1 = "Cloning repos",
                    extraText2 = { it.asPathString() },
                    data = pullRequests,
                ) { cloneRepos(it, settings) }
            filesOperator.refreshClones()
            reportState(state)
        }
    }

    suspend fun cloneRepos(pullRequest: PullRequestWrapper, settings: MegaManipulatorSettings): List<Action> {
        val basePath = projectOperator.project.basePath!!
        val fullPath = "$basePath/clones/${pullRequest.asPathString()}"
        val dir = File(fullPath)
        val prSettings: CodeHostSettings = settings.resolveSettings(pullRequest.searchHostName(), pullRequest.codeHostName())?.second
            ?: return listOf("Settings" to ApplyOutput.dummy(dir = pullRequest.asPathString(), err = "No settings found for ${pullRequest.searchHostName()}/${pullRequest.codeHostName()}"))
        val badState: List<Action> = clone(dir, pullRequest.cloneUrlFrom(prSettings.cloneType)!!, pullRequest.fromBranch())
        if (badState.isEmpty() && pullRequest.isFork()) {
            localRepoOperator.promoteOriginToForkRemote(dir, pullRequest.cloneUrlTo(prSettings.cloneType)!!)
        }
        return badState
    }

    @SuppressWarnings("ReturnCount")
    private suspend fun clone(dir: File, cloneUrl: String, branch: String): List<Action> {
        val badState: MutableList<Action> = mutableListOf()
        dir.mkdirs()
        if (File(dir, ".git").exists()) {
            badState.add("Repo already cloned" to ApplyOutput.dummy(dir = dir.path, std = "Repo already cloned"))
            return badState
        }
        val p0 = processOperator.runCommandAsync(
            dir.parentFile,
            listOf("git", "clone", cloneUrl, "--depth", "1", "--branch", branch, dir.absolutePath)
        ).await()
        if (p0.exitCode == 0) {
            return badState
        }
        badState.add("Failed shallow clone attempt" to p0)
        val p1 = processOperator.runCommandAsync(dir.parentFile, listOf("git", "clone", cloneUrl, dir.absolutePath)).await()
        if (p1.exitCode != 0) {
            badState.add("Failed full clone attempt" to p1)
            return badState
        }
        val p2 = processOperator.runCommandAsync(dir, listOf("git", "checkout", branch)).await()
        if (p2.exitCode == 0) {
            return badState
        }
        val p3 = processOperator.runCommandAsync(dir, listOf("git", "checkout", "-b", branch)).await()
        if (p3.exitCode != 0) {
            badState.add("Branch switch failed" to p3)
        }
        return badState
    }
}
