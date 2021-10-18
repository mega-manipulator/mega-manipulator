package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.GitUrlHelper
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import java.io.File

private typealias Action = Pair<String, ApplyOutput>

@SuppressWarnings("LongParameterList")
class CloneOperator @NonInjectable constructor(
    private val project: Project,
    filesOperator: FilesOperator?,
    prRouter: PrRouter?,
    localRepoOperator: LocalRepoOperator?,
    processOperator: ProcessOperator?,
    notificationsOperator: NotificationsOperator?,
    uiProtector: UiProtector?,
    settingsFileOperator: SettingsFileOperator?,
    gitUrlHelper: GitUrlHelper?,
) {
    constructor(project: Project) : this(
        project = project,
        filesOperator = null,
        prRouter = null,
        localRepoOperator = null,
        processOperator = null,
        notificationsOperator = null,
        uiProtector = null,
        settingsFileOperator = null,
        gitUrlHelper = null
    )

    private val filesOperator: FilesOperator by lazyService(project, filesOperator)
    private val prRouter: PrRouter by lazyService(project, prRouter)
    private val localRepoOperator: LocalRepoOperator by lazyService(project, localRepoOperator)
    private val processOperator: ProcessOperator by lazyService(project, processOperator)
    private val notificationsOperator: NotificationsOperator by lazyService(project, notificationsOperator)
    private val uiProtector: UiProtector by lazyService(project, uiProtector)
    private val settingsFileOperator: SettingsFileOperator by lazyService(project, settingsFileOperator)
    private val gitUrlHelper: GitUrlHelper by lazyService(project, gitUrlHelper)

    fun clone(repos: Set<SearchResult>, branchName: String, shallow: Boolean) {
        val basePath = project.basePath!!
        filesOperator.refreshConf()
        val settings = settingsFileOperator.readSettings()!!
        val state: List<Pair<SearchResult, List<Action>?>> = uiProtector.mapConcurrentWithProgress(
            title = "Cloning repos",
            extraText1 = "Cloning repos",
            extraText2 = { it.asPathString() },
            data = repos,
        ) { repo ->
            val codeSettings: CodeHostSettings =
                settings.resolveSettings(repo.searchHostName, repo.codeHostName)!!.second
            prRouter.getRepo(repo)?.let { vcsRepo: RepoWrapper ->
                val cloneUrl = gitUrlHelper.buildCloneUrl(codeSettings, vcsRepo)
                val defaultBranch = prRouter.getRepo(repo)?.getDefaultBranch()!!
                val dir = File(basePath, "clones/${repo.asPathString()}")
                clone(dir = dir, cloneUrl = cloneUrl, defaultBranch = defaultBranch, branch = branchName, shallow = shallow)
            }
        }
        filesOperator.refreshClones()
        reportState(state)
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
        val basePath = project.basePath!!
        val fullPath = "$basePath/clones/${pullRequest.asPathString()}"
        val dir = File(fullPath)
        val prSettings: CodeHostSettings =
            settings.resolveSettings(pullRequest.searchHostName(), pullRequest.codeHostName())?.second
                ?: return listOf(
                    "Settings" to ApplyOutput.dummy(
                        dir = pullRequest.asPathString(),
                        err = "No settings found for ${pullRequest.searchHostName()}/${pullRequest.codeHostName()}"
                    )
                )
        val badState: List<Action> =
            clone(
                dir = dir,
                cloneUrl = pullRequest.cloneUrlFrom(prSettings.cloneType)!!,
                defaultBranch = pullRequest.fromBranch()
            )
        if (badState.isEmpty() && pullRequest.isFork()) {
            localRepoOperator.promoteOriginToForkRemote(dir, pullRequest.cloneUrlTo(prSettings.cloneType)!!)
        }
        return badState
    }

    @SuppressWarnings("ReturnCount")
    private suspend fun clone(
        dir: File,
        cloneUrl: String,
        defaultBranch: String,
        branch: String = defaultBranch,
        shallow: Boolean = false,
    ): List<Action> {
        val badState: MutableList<Action> = mutableListOf()
        dir.mkdirs()
        if (File(dir, ".git").exists()) {
            badState.add("Repo already cloned" to ApplyOutput.dummy(dir = dir.path, std = "Repo already cloned"))
            return badState
        }
        var regularClone = !shallow
        if (shallow) {
            val p0 = processOperator.runCommandAsync(
                dir.parentFile,
                listOf("git", "clone", cloneUrl, "--depth", "1", "--branch", defaultBranch, dir.absolutePath)
            ).await()
            if (p0.exitCode != 0) {
                badState.add("Failed shallow clone clone" to p0)
                regularClone = true
            }
        }
        if (regularClone) {
            val p1 = processOperator.runCommandAsync(
                dir.parentFile,
                listOf("git", "clone", "--branch", defaultBranch, cloneUrl, dir.absolutePath)
            ).await()
            if (p1.exitCode != 0) {
                badState.add("Failed clone" to p1)
                return badState
            }
        }
        if (defaultBranch != branch) {
            val p2 = processOperator.runCommandAsync(dir, listOf("git", "checkout", branch)).await()
            if (p2.exitCode == 0) {
                return badState
            }
            val p3 = processOperator.runCommandAsync(dir, listOf("git", "checkout", "-b", branch)).await()
            if (p3.exitCode != 0) {
                badState.add("Branch switch failed" to p3)
            }
        }
        return badState
    }
}
