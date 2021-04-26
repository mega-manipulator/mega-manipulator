package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.NotificationType.WARNING
import java.io.File

@SuppressWarnings("LongParameterList")
class CloneOperator(
    private val filesOperator: FilesOperator,
    private val projectOperator: ProjectOperator,
    private val prRouter: PrRouter,
    private val localRepoOperator: LocalRepoOperator,
    private val processOperator: ProcessOperator,
    private val notificationsOperator: NotificationsOperator,
    private val uiProtector: UiProtector,
) {

    fun clone(repos: Set<SearchResult>) {
        val basePath = projectOperator.project.basePath!!

        filesOperator.refreshConf()
        val state: List<Pair<SearchResult, List<Pair<String, ApplyOutput>>?>> = uiProtector.mapConcurrentWithProgress(
            title = "Cloning repos",
            extraText1 = "Cloning repos",
            extraText2 = { it.asPathString() },
            data = repos,
        ) { repo ->
            prRouter.getRepo(repo)?.let { vcsRepo ->
                val cloneUrl = vcsRepo.getCloneUrl()!!
                val defaultBranch = prRouter.getRepo(repo)?.getDefaultBranch()!!
                val dir = File(basePath, "clones/${repo.asPathString()}")
                clone(dir, cloneUrl, defaultBranch)
            }
        }
        filesOperator.refreshClones()
        reportState(state)
    }

    private fun reportState(state: List<Pair<Any, List<Pair<String, ApplyOutput>>?>>) {
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
        val state: List<Pair<PullRequestWrapper, List<Pair<String, ApplyOutput>>?>> =
            uiProtector.mapConcurrentWithProgress(
                title = "Cloning repos",
                extraText1 = "Cloning repos",
                extraText2 = { it.asPathString() },
                data = pullRequests,
            ) { cloneRepos(it) }
        filesOperator.refreshClones()
        reportState(state)
    }

    suspend fun cloneRepos(pullRequest: PullRequestWrapper): List<Pair<String, ApplyOutput>> {
        val basePath = projectOperator.project.basePath!!
        val fullPath =
            "$basePath/clones/${pullRequest.searchHostName()}/${pullRequest.codeHostName()}/${pullRequest.project()}/${pullRequest.baseRepo()}"
        val dir = File(fullPath)
        val badState: List<Pair<String, ApplyOutput>> =
            clone(dir, pullRequest.cloneUrlFrom()!!, pullRequest.fromBranch())
        if (badState.isEmpty() && pullRequest.isFork()) {
            localRepoOperator.promoteOriginToForkRemote(dir, pullRequest.cloneUrlTo()!!)
        }
        return badState
    }

    private suspend fun clone(dir: File, cloneUrl: String, branch: String): List<Pair<String, ApplyOutput>> {
        val badState: MutableList<Pair<String, ApplyOutput>> = mutableListOf()
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
        val p1 =
            processOperator.runCommandAsync(dir.parentFile, listOf("git", "clone", cloneUrl, dir.absolutePath)).await()
        if (p1.exitCode != 0) {
            badState.add("Failed fill clone attempt" to p1)
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
