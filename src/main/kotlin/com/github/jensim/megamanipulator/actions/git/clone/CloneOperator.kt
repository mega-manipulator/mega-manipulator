package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.Action
import com.github.jensim.megamanipulator.actions.git.GitUrlHelper
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettings
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import java.io.File

class CloneOperator @NonInjectable constructor(
    private val project: Project,
    remoteCloneOperator: RemoteCloneOperator?,
    localCloneOperator: LocalCloneOperator?,
    settingsFileOperator: SettingsFileOperator?,
    filesOperator: FilesOperator?,
    prRouter: PrRouter?,
    notificationsOperator: NotificationsOperator?,
    uiProtector: UiProtector?,
    gitUrlHelper: GitUrlHelper?,
) {

    constructor(project: Project) : this(
        project = project,
        remoteCloneOperator = null,
        localCloneOperator = null,
        settingsFileOperator = null,
        filesOperator = null,
        prRouter = null,
        notificationsOperator = null,
        uiProtector = null,
        gitUrlHelper = null,
    )

    private val remoteCloneOperator: RemoteCloneOperator by lazyService(project, remoteCloneOperator)
    private val settingsFileOperator: SettingsFileOperator by lazyService(project, settingsFileOperator)
    private val filesOperator: FilesOperator by lazyService(project, filesOperator)
    private val prRouter: PrRouter by lazyService(project, prRouter)
    private val notificationsOperator: NotificationsOperator by lazyService(project, notificationsOperator)
    private val uiProtector: UiProtector by lazyService(project, uiProtector)
    private val gitUrlHelper: GitUrlHelper by lazyService(project, gitUrlHelper)
    private val localCloneOperator: LocalCloneOperator by lazyService(project, localCloneOperator)

    fun clone(repos: Set<SearchResult>, branchName: String, shallow: Boolean, sparseDef: String?) {
        filesOperator.refreshConf()
        val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
        val state: List<Pair<SearchResult, List<Action>?>> = uiProtector.mapConcurrentWithProgress(
            title = "Cloning repos",
            extraText1 = "Cloning repos",
            extraText2 = { it.asPathString() },
            data = repos,
        ) { repo: SearchResult ->
            clone(settings, repo, branchName, shallow, sparseDef)
        }
        filesOperator.refreshClones()
        reportState(state)
    }

    private suspend fun clone(
        settings: MegaManipulatorSettings,
        repo: SearchResult,
        branchName: String,
        shallow: Boolean,
        sparseDef: String?,
    ): List<Action> {
        val basePath = project.basePath!!
        val codeSettings: CodeHostSettings = settings.resolveSettings(repo.searchHostName, repo.codeHostName)?.second
            ?: return listOf(Action("Settings", ApplyOutput(std = "Settings were not resolvable for ${repo.searchHostName}/${repo.codeHostName}, most likely the key of the code host does not match the one returned by your search host!", dir = repo.asPathString(), exitCode = 1)))
        val vcsRepo: RepoWrapper = prRouter.getRepo(repo)
            ?: return listOf(Action("Finding repo on code host", ApplyOutput(std = "Didn't match settings on remote code host for ${repo.searchHostName}/${repo.codeHostName}", dir = repo.asPathString(), exitCode = 1)))
        val cloneUrl = gitUrlHelper.buildCloneUrl(codeSettings, vcsRepo)
        val defaultBranch = prRouter.getRepo(repo)?.getDefaultBranch() ?: return listOf(Action("Resolve default branch", ApplyOutput(repo.asPathString(), "Could not resolve default branch name", 1)))
        val dir = File(basePath, "clones/${repo.asPathString()}")
        val history = mutableListOf<Action>()

        val copyIf = localCloneOperator.copyIf(codeSettings, repo, defaultBranch, branchName)
        if (!copyIf.success) {
            history.addAll(
                remoteCloneOperator.clone(
                    dir = dir,
                    cloneUrl = cloneUrl,
                    defaultBranch = defaultBranch,
                    branch = branchName,
                    shallow = shallow,
                    sparseDef = sparseDef
                )
            )
            history.addAll(localCloneOperator.saveCopy(codeSettings, repo, defaultBranch).actions)
        }
        return history
    }

    fun clone(pullRequests: List<PullRequestWrapper>, sparseDef: String?) {
        val settings: MegaManipulatorSettings? = settingsFileOperator.readSettings()
        if (settings == null) {
            reportState(listOf("Settings" to listOf(Action("Load Settings", ApplyOutput.dummy(std = "No settings found for project.")))))
            return
        }
        val state: List<Pair<PullRequestWrapper, List<Action>?>> = uiProtector.mapConcurrentWithProgress(
            title = "Cloning repos",
            extraText1 = "Cloning repos",
            extraText2 = { it.asPathString() },
            data = pullRequests,
        ) {
            clone(it, sparseDef, settings)
        }
        filesOperator.refreshClones()
        reportState(state)
    }

    private suspend fun clone(
        pullRequest: PullRequestWrapper,
        sparseDef: String?,
        settings: MegaManipulatorSettings,
    ): List<Action> {
        val codeHostSettings: CodeHostSettings = settings.resolveSettings(pullRequest.searchHostName(), pullRequest.codeHostName())?.second
            ?: return listOf(
                Action(
                    "Settings",
                    ApplyOutput(
                        dir = pullRequest.asPathString(),
                        std = "No settings found for ${pullRequest.searchHostName()}/${pullRequest.codeHostName()}, most likely the key of the code host does not match the one returned by your search host!",
                        exitCode = 1,
                    )
                )
            )
        val history = mutableListOf<Action>()
        if (pullRequest.isFork()) {
            // TODO: Solve this 😬
            if (codeHostSettings.keepLocalRepos?.path != null) {
                history.add(Action("Restore kept repo", ApplyOutput(dir = pullRequest.asPathString(), std = "Pull request clones from local keep repo is not yet supported, it quickly becomes complex when you factor in fork settings, and that those can be changed by you (the user) at any time..", exitCode = 1)))
            }

            history.addAll(
                remoteCloneOperator.cloneRepos(
                    pullRequest = pullRequest,
                    settings = codeHostSettings,
                    sparseDef = sparseDef
                )
            )
            return history
        }
        val repo = pullRequest.asSearchResult()
        val defaultBranch = prRouter.getRepo(repo)?.getDefaultBranch() ?: return listOf(Action("Resolve default branch", ApplyOutput(repo.asPathString(), "Could not resolve default branch name", 1)))

        val copyIf = localCloneOperator.copyIf(codeHostSettings, repo, defaultBranch, pullRequest.fromBranch())
        history.addAll(copyIf.actions)
        if (!copyIf.success) {
            history.addAll(
                remoteCloneOperator.cloneRepos(
                    pullRequest = pullRequest,
                    settings = codeHostSettings,
                    sparseDef = sparseDef
                )
            )
        }
        return history
    }

    private fun reportState(state: List<Pair<Any, List<Action>?>>) {
        val badState: List<Pair<Any, List<Action>?>> = state.filter { it.second.orEmpty().lastOrNull()?.how?.exitCode != 0 }
        if (badState.isEmpty()) {
            notificationsOperator.show(
                title = "Cloning done",
                body = "All ${state.size} cloned successfully",
                type = INFORMATION,
            )
        } else {
            val stateAsString = badState.joinToString(
                prefix = "<ul>",
                separator = "<br>",
                postfix = "</ul>"
            ) { "<li>${it.first}${it.second?.lastOrNull()?.how?.let { "<br>output:'${it.std.replace("\n","<br>\n")}'" } ?: "..."}</li>" }
            notificationsOperator.show(
                title = "Cloning done with failures",
                body = "Failed cloning ${badState.size}/${state.size} repos. More info in IDE logs...<br>$stateAsString",
                type = WARNING,
            )
            val serializaleBadState: List<Pair<String, List<Action>?>> = badState.map { it.first.toString() to it.second }
            val badStateString = SerializationHolder.objectMapper.writeValueAsString(serializaleBadState)
            System.err.println("Failed cloning ${badState.size}/${state.size} repos, these are the causes:\n$badStateString")
        }
    }
}
