package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.ui.UiProtector
import com.github.jensim.megamanipulator.ui.trimProjectPath
import com.intellij.notification.NotificationType
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

    companion object {

        val instance by lazy {
            CloneOperator(
                filesOperator = FilesOperator.instance,
                projectOperator = ProjectOperator.instance,
                prRouter = PrRouter.instance,
                localRepoOperator = LocalRepoOperator.instance,
                processOperator = ProcessOperator.instance,
                notificationsOperator = NotificationsOperator.instance,
                uiProtector = UiProtector.instance,
            )
        }
    }

    fun clone(branch: String, repos: Set<SearchResult>) {
        val basePath = projectOperator.project?.basePath!!
        val noConf = mutableListOf<SearchResult>()

        filesOperator.refreshConf()
        uiProtector.mapConcurrentWithProgress(
            title = "Cloning repos",
            extraText1 = "Cloning repos",
            extraText2 = { it.asPathString() },
            data = repos,
        ) { repo ->
            val vcsRepo = prRouter.getRepo(repo)
            val cloneUrl = vcsRepo?.getCloneUrl()!!
            val dir = File(basePath, "clones/${repo.asPathString()}")
            clone(dir, cloneUrl, branch)
        }
        filesOperator.refreshClones()
        if (noConf.isEmpty()) {
            println("All done")
        } else {
            println("All done, no conf found for $noConf")
        }
    }

    suspend fun clone(pullRequest: PullRequestWrapper) {
        val basePath = projectOperator.project?.basePath!!
        val fullPath = "$basePath/clones/${pullRequest.searchHostName()}/${pullRequest.codeHostName()}/${pullRequest.project()}/${pullRequest.baseRepo()}"
        val dir = File(fullPath)
        clone(dir, pullRequest.cloneUrlFrom()!!, pullRequest.fromBranch())
        if (pullRequest.isFork()) {
            localRepoOperator.promoteOriginToForkRemote(dir, pullRequest.cloneUrlTo()!!)
        }
    }

    private suspend fun clone(dir: File, cloneUrl: String, branch: String) {
        dir.mkdirs()
        if (File(dir, ".git").exists()) {
            notificationsOperator.show(
                title = "Repo already cloned",
                body = "Repo ${dir.trimProjectPath(projectOperator.project!!)} already cloned.\nWill not do anything.",
                type = WARNING
            )
        } else {
            val p1 = processOperator.runCommandAsync(dir.parentFile, listOf("git", "clone", cloneUrl, dir.absolutePath)).await()
            if (p1.exitCode != 0) {
                notificationsOperator.show(
                    "Clone failed",
                    "Clone in dir ${p1.dir} failed with code ${p1.exitCode} and output ${p1.std}",
                    NotificationType.ERROR
                )
            } else {
                val p2 = processOperator.runCommandAsync(dir, listOf("git", "checkout", branch)).await()
                if (p2.exitCode != 0) {
                    val p3 = processOperator.runCommandAsync(dir, listOf("git", "checkout", "-b", branch)).await()
                    if (p3.exitCode != 0) {
                        notificationsOperator.show(
                            "Branch switch failed",
                            "Branch switch in dir ${p3.dir} failed with code ${p3.exitCode} and output ${p3.std}",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
}
