package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
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

    private suspend fun clone(settings: MegaManipulatorSettings, repo: SearchResult, branchName: String, shallow: Boolean, sparseDef: String?): List<Action> {
        val basePath = project.basePath!!
        val codeSettings: CodeHostSettings = settings.resolveSettings(repo.searchHostName, repo.codeHostName)?.second
            ?: return listOf("Settings" to ApplyOutput.dummy(std = "Settings were not resolvable for ${repo.searchHostName}/${repo.codeHostName}, most likely the key of the code host does not match the one returned by your search host!"))
        val vcsRepo: RepoWrapper = prRouter.getRepo(repo)
            ?: return listOf("Finding repo on code host" to ApplyOutput.dummy(std = "Didn't match settings on remote code host for ${repo.searchHostName}/${repo.codeHostName}"))
        val cloneUrl = gitUrlHelper.buildCloneUrl(codeSettings, vcsRepo)
        val defaultBranch = prRouter.getRepo(repo)?.getDefaultBranch()!!
        val dir = File(basePath, "clones/${repo.asPathString()}")
        if (codeSettings.keepLocalRepos?.path == null) {
            return remoteCloneOperator.clone(
                dir = dir,
                cloneUrl = cloneUrl,
                defaultBranch = defaultBranch,
                branch = branchName,
                shallow = shallow,
                sparseDef = sparseDef
            )
        } else {
            println("keepLocalRepos path: ${codeSettings.keepLocalRepos?.path}")
            TODO("Not yet implemented")
        }
    }

    fun clone(pullRequests: List<PullRequestWrapper>, sparseDef: String?) {
        val settings = settingsFileOperator.readSettings()
        if (settings == null) {
            reportState(listOf("Settings" to listOf("Load Settings" to ApplyOutput.dummy(std = "No settings found for project."))))
            return
        }
        val state: List<Pair<PullRequestWrapper, List<Action>?>> = uiProtector.mapConcurrentWithProgress(
            title = "Cloning repos",
            extraText1 = "Cloning repos",
            extraText2 = { it.asPathString() },
            data = pullRequests,
        ) {
            remoteCloneOperator.cloneRepos(
                pullRequest = it,
                settings = settings,
                sparseDef = sparseDef
            )
        }
        filesOperator.refreshClones()
        reportState(state)
    }

    private fun reportState(state: List<Pair<Any, List<Action>?>>) {
        val badState: List<Pair<Any, List<Action>?>> = state.filter { it.second.orEmpty().lastOrNull()?.second?.exitCode != 0 }
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
            ) { "<li>${it.first}${it.second?.lastOrNull()?.second?.let { "<br>output:'${it.std.replace("\n","<br>\n")}'" } ?: "..."}</li>" }
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
