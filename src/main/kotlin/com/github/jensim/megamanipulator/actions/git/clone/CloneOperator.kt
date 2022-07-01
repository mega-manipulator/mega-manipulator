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
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import kotlinx.serialization.encodeToString
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

    fun clone(repos: Set<SearchResult>, branchName: String, shallow: Boolean, sparseDef: String?) {
        val basePath = project.basePath!!
        filesOperator.refreshConf()
        val settings = settingsFileOperator.readSettings()!!
        val state: List<Pair<SearchResult, List<Action>?>> = uiProtector.mapConcurrentWithProgress(
            title = "Cloning repos",
            extraText1 = "Cloning repos",
            extraText2 = { it.asPathString() },
            data = repos,
        ) { repo: SearchResult ->
            val codeSettings: CodeHostSettings = settings.resolveSettings(repo.searchHostName, repo.codeHostName)?.second
                ?: return@mapConcurrentWithProgress listOf("Settings" to ApplyOutput.dummy(std = "Settings were not resolvable for ${repo.searchHostName}/${repo.codeHostName}, most likely the key of the code host does not match the one returned by your search host!"))
            prRouter.getRepo(repo)?.let { vcsRepo: RepoWrapper ->
                val cloneUrl = gitUrlHelper.buildCloneUrl(codeSettings, vcsRepo)
                val defaultBranch = prRouter.getRepo(repo)?.getDefaultBranch()!!
                val dir = File(basePath, "clones/${repo.asPathString()}")
                clone(
                    dir = dir,
                    cloneUrl = cloneUrl,
                    defaultBranch = defaultBranch,
                    branch = branchName,
                    shallow = shallow,
                    sparseDef = sparseDef
                )
            }
        }
        filesOperator.refreshClones()
        reportState(state)
    }

    private fun reportState(state: List<Pair<Any, List<Action>?>>) {
        val (badState: List<Pair<Any, List<Action>?>>, goodState: List<Pair<Any, List<Action>?>>) = state.partition { it.second.orEmpty().lastOrNull()?.second?.exitCode != 0 }
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

    fun clone(pullRequests: List<PullRequestWrapper>, sparseDef: String?) {
        val settings = uiProtector.uiProtectedOperation("Load settings") { settingsFileOperator.readSettings() }
        if (settings == null) {
            reportState(listOf("Settings" to listOf("Load Settings" to ApplyOutput.dummy(std = "No settings found for project."))))
        } else {
            val state: List<Pair<PullRequestWrapper, List<Action>?>> =
                uiProtector.mapConcurrentWithProgress(
                    title = "Cloning repos",
                    extraText1 = "Cloning repos",
                    extraText2 = { it.asPathString() },
                    data = pullRequests,
                ) {
                    cloneRepos(
                        pullRequest = it,
                        settings = settings,
                        sparseDef = sparseDef
                    )
                }
            filesOperator.refreshClones()
            reportState(state)
        }
    }

    suspend fun cloneRepos(pullRequest: PullRequestWrapper, settings: MegaManipulatorSettings, sparseDef: String?): List<Action> {
        val basePath = project.basePath!!
        val fullPath = "$basePath/clones/${pullRequest.asPathString()}"
        val dir = File(fullPath)
        val prSettings: CodeHostSettings =
            settings.resolveSettings(pullRequest.searchHostName(), pullRequest.codeHostName())?.second
                ?: return listOf(
                    "Settings" to ApplyOutput.dummy(
                        dir = pullRequest.asPathString(),
                        std = "No settings found for ${pullRequest.searchHostName()}/${pullRequest.codeHostName()}, most likely the key of the code host does not match the one returned by your search host!"
                    )
                )
        val badState: List<Action> =
            clone(
                dir = dir,
                cloneUrl = pullRequest.cloneUrlFrom(prSettings.cloneType)!!,
                defaultBranch = pullRequest.fromBranch(),
                shallow = false,
                sparseDef = sparseDef
            )
        if (badState.isOkay() && pullRequest.isFork()) {
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
        shallow: Boolean,
        sparseDef: String?,
    ): List<Action> {
        val actionTrace: MutableList<Action> = mutableListOf()
        dir.mkdirs()
        if (File(dir, ".git").exists()) {
            actionTrace.add("Repo already cloned" to ApplyOutput.dummy(dir = dir.path, std = "Repo already cloned"))
            return actionTrace
        }
        val cloneCommandsResult = cloneCommands(shallow, cloneUrl, defaultBranch, dir)
        actionTrace.addAll(cloneCommandsResult)

        if (actionTrace.isOkay()) {
            actionTrace.addAll(pullCommands(dir, sparseDef, defaultBranch, shallow))
        }

        if (defaultBranch != branch && actionTrace.isOkay()) {
            val p3 = processOperator.runCommandAsync(dir, listOf("git", "checkout", branch)).await()
            actionTrace.add("Switch branch" to p3)
            if (p3.exitCode != 0) {
                val p4 = processOperator.runCommandAsync(dir, listOf("git", "checkout", "-b", branch)).await()
                actionTrace.add("Create branch" to p4)
            }
        }
        return actionTrace
    }

    private fun List<Action>.isOkay(): Boolean = isEmpty() || lastOrNull()?.second?.exitCode == 0

    private suspend fun cloneCommands(shallow: Boolean, cloneUrl: String, defaultBranch: String, dir: File): List<Action> {
        val actionTrace = mutableListOf<Action>()
        val cloneActionName = if (shallow) "Shallow clone" else "Clone"

        val cloneArgs = if (shallow) {
            listOf("git", "clone", cloneUrl, "--depth", "1", "--no-checkout", "--branch", defaultBranch, dir.absolutePath)
        } else {
            listOf("git", "clone", cloneUrl, "--no-checkout", "--branch", defaultBranch, dir.absolutePath)
        }
        val p0 = processOperator.runCommandAsync(dir.parentFile, cloneArgs).await()
        actionTrace.add(cloneActionName to p0)
        return actionTrace
    }

    private suspend fun pullCommands(dir: File, sparseDef: String?, defaultBranch: String, shallow: Boolean): List<Action> {
        val actionTrace = mutableListOf<Action>()
        if (sparseDef != null) {
            val p0 = processOperator.runCommandAsync(dir, listOf("git", "config", "core.sparseCheckout", "true")).await()
            actionTrace.add("Config sparse checkout" to p0)
            if (p0.exitCode == 0) {
                try {
                    val sparseFile = File(dir, ".git/info/sparse-checkout")
                    sparseFile.writeText(sparseDef)
                    actionTrace.add("Setup sparse checkout config" to ApplyOutput(dir = dir.absolutePath, std = "Setup successful", exitCode = 0))
                } catch (e: Exception) {
                    // e.printStackTrace()
                    actionTrace.add("Setup sparse checkout config" to ApplyOutput(dir = dir.absolutePath, std = "Failed writing sparse config file\n${e.stackTraceToString()}", exitCode = 1))
                }
            }
        }
        if (actionTrace.isOkay()) {
            val p1 = if (shallow) {
                processOperator.runCommandAsync(dir, listOf("git", "fetch", "--depth", "1", "origin", defaultBranch)).await()
            } else {
                processOperator.runCommandAsync(dir, listOf("git", "fetch", "origin", defaultBranch)).await()
            }
            actionTrace.add("Fetch" to p1)
            if (p1.exitCode == 0) {
                val p2 = processOperator.runCommandAsync(dir, listOf("git", "checkout", defaultBranch)).await()
                actionTrace.add("Checkout" to p2)
            }
        }
        return actionTrace
    }
}
